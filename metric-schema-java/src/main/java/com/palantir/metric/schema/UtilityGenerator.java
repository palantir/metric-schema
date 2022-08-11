/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.metric.schema;

import com.codahale.metrics.Gauge;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CheckReturnValue;
import com.palantir.logsafe.Preconditions;
import com.palantir.logsafe.Safe;
import com.palantir.metric.schema.model.BuilderStage;
import com.palantir.metric.schema.model.ImplementationVisibility;
import com.palantir.metric.schema.model.StagedBuilderSpec;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

final class UtilityGenerator {

    static JavaFile generateUtilityClass(
            String namespace,
            MetricNamespace metrics,
            Optional<String> libraryName,
            String packageName,
            ImplementationVisibility visibility) {
        String name = metrics.getShortName().orElse(namespace);
        ClassName className = ClassName.get(packageName, className(name));
        TypeSpec.Builder builder = TypeSpec.classBuilder(className.simpleName())
                .addModifiers(visibility.apply(Modifier.FINAL))
                .addJavadoc(Javadoc.render(metrics.getDocs()))
                .addField(TaggedMetricRegistry.class, ReservedNames.REGISTRY_NAME, Modifier.PRIVATE, Modifier.FINAL)
                .addField(FieldSpec.builder(
                                String.class,
                                ReservedNames.JAVA_VERSION_FIELD,
                                Modifier.PRIVATE,
                                Modifier.STATIC,
                                Modifier.FINAL)
                        .initializer("$T.getProperty($S, $S)", System.class, "java.version", "unknown")
                        .build());
        if (!metrics.getTags().isEmpty()) {
            metrics.getTags().forEach(tagDef -> {
                if (tagDefinitionRequiresParam(tagDef)) {
                    builder.addField(
                            String.class, Custodian.sanitizeName(tagDef.getName()), Modifier.PRIVATE, Modifier.FINAL);
                }

                if (tagDef.getValues().size() <= 1) {
                    return;
                }

                generateTagEnum(builder, name, visibility, tagDef);
            });
        }

        if (metrics.getTags().isEmpty()) {
            builder.addMethod(generateSimpleFactory(visibility, className));
        } else {
            generateFactoryBuilder(name, className, metrics, builder, visibility);
        }

        if (libraryName.isPresent()) {
            builder.addField(FieldSpec.builder(
                            String.class,
                            ReservedNames.LIBRARY_NAME_FIELD,
                            Modifier.PRIVATE,
                            Modifier.STATIC,
                            Modifier.FINAL)
                    .initializer("$S", libraryName.get())
                    .build());
            builder.addField(FieldSpec.builder(
                            String.class,
                            ReservedNames.LIBRARY_VERSION_FIELD,
                            Modifier.PRIVATE,
                            Modifier.STATIC,
                            Modifier.FINAL)
                    .initializer(
                            "$T.requireNonNullElse($T.class.getPackage().getImplementationVersion(), \"unknown\")",
                            Objects.class,
                            className)
                    .build());
        }

        builder.addMethod(generateConstructor(name, metrics));

        metrics.getMetrics().forEach((metricName, definition) -> {
            generateConstants(builder, metricName, definition, visibility);
            if (numArgs(definition) <= 1) {
                builder.addMethods(generateSimpleMetricFactory(
                        namespace, metricName, libraryName, metrics, definition, visibility));
            } else {
                generateMetricFactoryBuilder(
                        namespace, metricName, libraryName, definition, metrics, builder, visibility);
            }
        });

        builder.addMethod(generateToString(metrics, className));

        return JavaFile.builder(className.packageName(), builder.build())
                .skipJavaLangImports(true)
                .indent("    ")
                .build();
    }

    private static MethodSpec generateSimpleFactory(ImplementationVisibility visibility, ClassName className) {
        return MethodSpec.methodBuilder(ReservedNames.FACTORY_METHOD)
                .addModifiers(visibility.apply(Modifier.STATIC))
                .addParameter(TaggedMetricRegistry.class, ReservedNames.REGISTRY_NAME)
                .addStatement(
                        "return new $T($T.checkNotNull(registry, $S))",
                        className,
                        Preconditions.class,
                        TaggedMetricRegistry.class.getSimpleName())
                .returns(className)
                .build();
    }

    /** Produce a private staged builder, which implements public interfaces. */
    private static void generateFactoryBuilder(
            String name,
            ClassName className,
            MetricNamespace metricNamespace,
            TypeSpec.Builder outerBuilder,
            ImplementationVisibility visibility1) {
        BuilderStage metricRegistryStage = BuilderStage.builder()
                .name(ReservedNames.REGISTRY_NAME)
                .sanitizedName(ReservedNames.REGISTRY_NAME)
                .className(ClassName.get(TaggedMetricRegistry.class))
                .build();
        List<BuilderStage> tagStages = metricNamespace.getTags().stream()
                .filter(UtilityGenerator::tagDefinitionRequiresParam)
                .map(tagDef -> BuilderStage.builder()
                        .name(tagDef.getName())
                        .sanitizedName(Custodian.sanitizeName(tagDef.getName()))
                        .className(getTagClassName(name, tagDef))
                        .build())
                .collect(Collectors.toList());
        CodeBlock.Builder buildBlock = CodeBlock.builder();
        buildBlock.add("new $T($L", className, metricRegistryStage.sanitizedName());
        tagStages.forEach(stage -> buildBlock.add(", $L", stage.sanitizedName()));
        buildBlock.add(");");
        StagedBuilderSpec stagedBuilderSpec = StagedBuilderSpec.builder()
                .name(name)
                .className(className)
                .constructor(buildBlock.build())
                .visibility(visibility1)
                .isStatic(true)
                .addAllStages(ImmutableList.<BuilderStage>builder()
                        .add(metricRegistryStage)
                        .addAll(tagStages)
                        .build())
                .build();
        generateStagedBuilder(stagedBuilderSpec, outerBuilder);
    }

    private static void generateStagedBuilder(StagedBuilderSpec stagedBuilderSpec, TypeSpec.Builder outerBuilder) {
        MethodSpec.Builder abstractBuildMethodBuilder = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(stagedBuilderSpec.className());
        abstractBuildMethodBuilder.addAnnotation(CheckReturnValue.class);
        MethodSpec abstractBuildMethod = abstractBuildMethodBuilder.build();
        List<MethodSpec> abstractBuildMethods = ImmutableList.of(abstractBuildMethod);
        outerBuilder.addType(TypeSpec.interfaceBuilder(buildStage(stagedBuilderSpec.name()))
                .addModifiers(stagedBuilderSpec.visibility().apply())
                .addMethods(abstractBuildMethods)
                .build());
        for (int i = 0; i < stagedBuilderSpec.stages().size(); i++) {
            boolean lastTag = i == stagedBuilderSpec.stages().size() - 1;
            BuilderStage tag = stagedBuilderSpec.stages().get(i);
            String tagName = tag.name();
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(tag.sanitizedName());
            tag.docs().ifPresent(docs -> methodBuilder.addJavadoc(Javadoc.render(docs)));
            outerBuilder.addType(TypeSpec.interfaceBuilder(stageName(stagedBuilderSpec.name(), tagName))
                    .addModifiers(stagedBuilderSpec.visibility().apply())
                    .addMethod(methodBuilder
                            .addParameter(ParameterSpec.builder(tag.className(), tag.sanitizedName())
                                    .addAnnotation(Safe.class)
                                    .build())
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addAnnotation(CheckReturnValue.class)
                            .returns(ClassName.bestGuess(
                                    lastTag
                                            ? buildStage(stagedBuilderSpec.name())
                                            : stageName(
                                                    stagedBuilderSpec.name(),
                                                    stagedBuilderSpec
                                                            .stages()
                                                            .get(i + 1)
                                                            .name())))
                            .build())
                    .build());
        }
        MethodSpec.Builder buildMethodBuilder = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(stagedBuilderSpec.className())
                .addCode("return ")
                .addCode(stagedBuilderSpec.constructor());
        MethodSpec buildMethod = buildMethodBuilder.build();
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.addAll(List.of(Modifier.PRIVATE, Modifier.FINAL));
        if (stagedBuilderSpec.isStatic()) {
            modifiers.add(Modifier.STATIC);
        }
        outerBuilder.addType(TypeSpec.classBuilder(Custodian.anyToUpperCamel(stagedBuilderSpec.name()) + "Builder")
                .addModifiers(modifiers.toArray(new Modifier[0]))
                .addSuperinterfaces(stagedBuilderSpec.stages().stream()
                        .map(stage -> ClassName.bestGuess(stageName(stagedBuilderSpec.name(), stage.name())))
                        .collect(ImmutableList.toImmutableList()))
                .addSuperinterface(ClassName.bestGuess(buildStage(stagedBuilderSpec.name())))
                .addFields(stagedBuilderSpec.stages().stream()
                        .map(tag -> FieldSpec.builder(tag.className(), tag.sanitizedName(), Modifier.PRIVATE)
                                .build())
                        .collect(ImmutableList.toImmutableList()))
                .addMethod(buildMethod)
                .addMethods(stagedBuilderSpec.stages().stream()
                        .map(tag -> MethodSpec.methodBuilder(tag.sanitizedName())
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(Override.class)
                                .returns(ClassName.bestGuess(
                                        Custodian.anyToUpperCamel(stagedBuilderSpec.name()) + "Builder"))
                                .addParameter(ParameterSpec.builder(tag.className(), tag.sanitizedName())
                                        .addAnnotation(Safe.class)
                                        .build())
                                .addStatement(
                                        "$1T.checkState(this.$2L == null, $3S)",
                                        Preconditions.class,
                                        tag.sanitizedName(),
                                        tag.name() + " is already set")
                                .addStatement(
                                        "this.$1L = $2T.checkNotNull($1L, $3S)",
                                        tag.sanitizedName(),
                                        Preconditions.class,
                                        tag.name() + " is required")
                                .addStatement("return this")
                                .build())
                        .collect(ImmutableList.toImmutableList()))
                .build());
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(ReservedNames.BUILDER_METHOD)
                .addModifiers(stagedBuilderSpec.visibility().apply(Modifier.STATIC))
                .returns(ClassName.bestGuess(stageName(
                        stagedBuilderSpec.name(),
                        stagedBuilderSpec.stages().get(0).name())))
                .addAnnotation(CheckReturnValue.class)
                .addStatement(
                        "return new $T()",
                        ClassName.bestGuess(Custodian.anyToUpperCamel(stagedBuilderSpec.name()) + "Builder"));
        stagedBuilderSpec.docs().ifPresent(docs -> methodBuilder.addJavadoc(Javadoc.render(docs)));
        outerBuilder.addMethod(methodBuilder.build());
    }

    private static MethodSpec generateConstructor(String actualName, MetricNamespace metrics) {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(TaggedMetricRegistry.class, ReservedNames.REGISTRY_NAME)
                .addStatement("this.$1L = $1L", ReservedNames.REGISTRY_NAME);

        if (!metrics.getTags().isEmpty()) {
            metrics.getTags().forEach(tagDef -> {
                if (tagDefinitionRequiresParam(tagDef)) {
                    builder.addParameter(getTagClassName(actualName, tagDef), Custodian.sanitizeName(tagDef.getName()));
                    if (tagDef.getValues().isEmpty()) {
                        builder.addStatement("this.$1L = $1L", Custodian.sanitizeName(tagDef.getName()));
                    } else {
                        builder.addStatement("this.$1L = $1L.getValue()", Custodian.sanitizeName(tagDef.getName()));
                    }
                }
            });
        }

        return builder.build();
    }

    private static void generateConstants(
            TypeSpec.Builder builder,
            String metricName,
            MetricDefinition definition,
            ImplementationVisibility visibility) {
        definition.getTagDefinitions().forEach(tagDef -> {
            if (tagDef.getValues().size() <= 1) {
                return;
            }

            generateTagEnum(builder, metricName, visibility, tagDef);
        });
    }

    private static void generateTagEnum(
            TypeSpec.Builder builder, String metricName, ImplementationVisibility visibility, TagDefinition tagDef) {
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(getTagClassName(metricName, tagDef));
        tagDef.getValues()
                .forEach(value -> enumBuilder.addEnumConstant(
                        Custodian.anyToUpperUnderscore(value.getValue()),
                        TypeSpec.anonymousClassBuilder("$S", value.getValue()).build()));

        builder.addType(enumBuilder
                .addModifiers(visibility.apply())
                .addField(FieldSpec.builder(String.class, "value", Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(String.class, "value")
                        .addStatement("this.value = value")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getValue")
                        .addModifiers(Modifier.PRIVATE)
                        .returns(String.class)
                        .addStatement("return value")
                        .build())
                .build());
    }

    private static String className(String namespace) {
        return Custodian.anyToUpperCamel(namespace) + "Metrics";
    }

    private static CodeBlock metricName(
            String namespace,
            String metricName,
            Optional<String> libraryName,
            MetricDefinition definition,
            MetricNamespace metricNamespace) {
        String safeName = namespace + '.' + metricName;
        CodeBlock.Builder builder = CodeBlock.builder().add("$T.builder().safeName($S)", MetricName.class, safeName);
        metricNamespace.getTags().forEach(tagDef -> {
            if (tagDef.getValues().size() != 1) {
                builder.add(".putSafeTags($S, $L)", tagDef.getName(), Custodian.sanitizeName(tagDef.getName()));
            } else {
                builder.add(
                        ".putSafeTags($S, $S)",
                        tagDef.getName(),
                        Iterables.getOnlyElement(tagDef.getValues()).getValue());
            }
        });

        definition.getTagDefinitions().forEach(tagDef -> putSafeTags(tagDef, builder));
        ImmutableSortedSet<String> insensitiveTags = insensitiveTags(definition);
        if (libraryName.isPresent()) {
            if (!insensitiveTags.contains(ReservedNames.LIBRARY_NAME_TAG)) {
                builder.add(".putSafeTags($S, $L)", ReservedNames.LIBRARY_NAME_TAG, ReservedNames.LIBRARY_NAME_FIELD);
            }
            if (!insensitiveTags.contains(ReservedNames.LIBRARY_VERSION_TAG)) {
                builder.add(
                        ".putSafeTags($S, $L)", ReservedNames.LIBRARY_VERSION_TAG, ReservedNames.LIBRARY_VERSION_FIELD);
            }
        }
        if (!insensitiveTags.contains(ReservedNames.JAVA_VERSION_TAG)) {
            builder.add(".putSafeTags($S, $L)", ReservedNames.JAVA_VERSION_TAG, ReservedNames.JAVA_VERSION_FIELD);
        }
        return builder.add(".build()").build();
    }

    private static void putSafeTags(TagDefinition tagDef, CodeBlock.Builder builder) {
        if (tagDef.getValues().isEmpty()) {
            builder.add(".putSafeTags($S, $L)", tagDef.getName(), Custodian.sanitizeName(tagDef.getName()));
        } else if (tagDef.getValues().size() == 1) {
            builder.add(
                    ".putSafeTags($S, $S)",
                    tagDef.getName(),
                    Iterables.getOnlyElement(tagDef.getValues()).getValue());
        } else {
            builder.add(".putSafeTags($S, $L.getValue())", tagDef.getName(), Custodian.sanitizeName(tagDef.getName()));
        }
    }

    private static ImmutableSortedSet<String> insensitiveTags(MetricDefinition definition) {
        return definition.getTagDefinitions().stream()
                .map(TagDefinition::getName)
                .collect(ImmutableSortedSet.toImmutableSortedSet(String.CASE_INSENSITIVE_ORDER));
    }

    private static MethodSpec generateToString(MetricNamespace metricNamespace, ClassName className) {
        CodeBlock.Builder returnBuilder = CodeBlock.builder();
        returnBuilder.add(
                "return $S + $L",
                String.format("%s{%s=", className.simpleName(), ReservedNames.REGISTRY_NAME),
                ReservedNames.REGISTRY_NAME);
        metricNamespace.getTags().forEach(tagDef -> {
            if (tagDef.getValues().size() != 1) {
                returnBuilder.add(
                        "+ $S + $L",
                        String.format(", %s=", tagDef.getName()),
                        Custodian.sanitizeName(tagDef.getName()));
            } else {
                returnBuilder.add(
                        "+ $S",
                        String.format(
                                ", %s=%s",
                                tagDef.getName(),
                                Iterables.getOnlyElement(tagDef.getValues()).getValue()));
            }
        });
        returnBuilder.add("+ '}'");
        return MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addStatement(returnBuilder.build())
                .returns(String.class)
                .build();
    }

    private static List<MethodSpec> generateSimpleMetricFactory(
            String namespace,
            String metricName,
            Optional<String> libraryName,
            MetricNamespace metricNamespace,
            MetricDefinition definition,
            ImplementationVisibility visibility) {
        boolean isGauge = MetricType.GAUGE.equals(definition.getType());

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Custodian.sanitizeName(metricName))
                .addModifiers(visibility.apply())
                .returns(MetricTypes.type(definition.getType()))
                .addParameters(definition.getTagDefinitions().stream()
                        .filter(UtilityGenerator::tagDefinitionRequiresParam)
                        .map(tag -> ParameterSpec.builder(
                                        getTagClassName(metricName, tag), Custodian.sanitizeName(tag.getName()))
                                .addAnnotation(Safe.class)
                                .build())
                        .collect(ImmutableList.toImmutableList()))
                .addJavadoc(Javadoc.render(definition.getDocs()));

        CodeBlock metricNameBlock = metricName(namespace, metricName, libraryName, definition, metricNamespace);
        MethodSpec metricNameMethod = MethodSpec.methodBuilder(Custodian.sanitizeName(metricName + "MetricName"))
                .addModifiers(visibility.apply())
                .addModifiers(Modifier.STATIC)
                .returns(MetricName.class)
                .addCode("return $L;", metricNameBlock)
                .build();

        String metricRegistryMethod = MetricTypes.registryAccessor(definition.getType());
        if (isGauge) {
            methodBuilder.addParameter(
                    ParameterizedTypeName.get(ClassName.get(Gauge.class), WildcardTypeName.subtypeOf(Object.class)),
                    ReservedNames.GAUGE_NAME);
            // TODO(ckozak): Update to use a method which can log a warning and replace existing gauges.
            // See MetricRegistries.registerWithReplacement.
            methodBuilder.addStatement(
                    "$L.$L($L(), $L)",
                    ReservedNames.REGISTRY_NAME,
                    metricRegistryMethod,
                    metricNameMethod.name,
                    ReservedNames.GAUGE_NAME);
        } else {
            methodBuilder.addAnnotation(CheckReturnValue.class);
            methodBuilder.addStatement(
                    "return $L.$L($L)", ReservedNames.REGISTRY_NAME, metricRegistryMethod, metricNameBlock);
        }
        MethodSpec method = methodBuilder.build();

        return isGauge ? ImmutableList.of(method, metricNameMethod) : ImmutableList.of(method);
    }

    /** Produce a private staged builder, which implements public interfaces. */
    private static void generateMetricFactoryBuilder(
            String namespaceName,
            String metricName,
            Optional<String> libraryName,
            MetricDefinition definition,
            MetricNamespace metricNamespace,
            TypeSpec.Builder outerBuilder,
            ImplementationVisibility visibility) {
        boolean isGauge = MetricType.GAUGE.equals(definition.getType());
        MethodSpec.Builder abstractBuildMethodBuilder = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(MetricTypes.type(definition.getType()));
        if (isGauge) {
            abstractBuildMethodBuilder.addParameter(
                    ParameterizedTypeName.get(ClassName.get(Gauge.class), WildcardTypeName.subtypeOf(Object.class)),
                    ReservedNames.GAUGE_NAME);
        } else {
            abstractBuildMethodBuilder.addAnnotation(CheckReturnValue.class);
        }
        MethodSpec abstractBuildMethod = abstractBuildMethodBuilder.build();
        MethodSpec abstractBuildMetricName = MethodSpec.methodBuilder("buildMetricName")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(MetricName.class)
                .build();
        List<MethodSpec> abstractBuildMethods = isGauge
                ? ImmutableList.of(abstractBuildMethod, abstractBuildMetricName)
                : ImmutableList.of(abstractBuildMethod);

        outerBuilder.addType(TypeSpec.interfaceBuilder(buildStage(metricName))
                .addModifiers(visibility.apply())
                .addMethods(abstractBuildMethods)
                .build());
        ImmutableList<TagDefinition> tagList = definition.getTagDefinitions().stream()
                .filter(UtilityGenerator::tagDefinitionRequiresParam)
                .collect(ImmutableList.toImmutableList());
        for (int i = 0; i < tagList.size(); i++) {
            boolean lastTag = i == tagList.size() - 1;
            TagDefinition tag = tagList.get(i);
            String tagName = tag.getName();
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Custodian.sanitizeName(tagName));
            tag.getDocs().ifPresent(docs -> methodBuilder.addJavadoc(Javadoc.render(docs)));
            outerBuilder.addType(TypeSpec.interfaceBuilder(stageName(metricName, tagName))
                    .addModifiers(visibility.apply())
                    .addMethod(methodBuilder
                            .addParameter(ParameterSpec.builder(
                                            getTagClassName(metricName, tag), Custodian.sanitizeName(tagName))
                                    .addAnnotation(Safe.class)
                                    .build())
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addAnnotation(CheckReturnValue.class)
                            .returns(ClassName.bestGuess(
                                    lastTag
                                            ? buildStage(metricName)
                                            : stageName(
                                                    metricName,
                                                    tagList.get(i + 1).getName())))
                            .build())
                    .build());
        }
        CodeBlock metricNameBlock = metricName(namespaceName, metricName, libraryName, definition, metricNamespace);
        String metricRegistryMethod = MetricTypes.registryAccessor(definition.getType());

        MethodSpec buildMetricName = MethodSpec.methodBuilder("buildMetricName")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(MetricName.class)
                .addStatement("return $L", metricNameBlock)
                .build();

        MethodSpec.Builder buildMethodBuilder = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(MetricTypes.type(definition.getType()));
        if (isGauge) {
            buildMethodBuilder
                    .addParameter(
                            ParameterizedTypeName.get(
                                    ClassName.get(Gauge.class), WildcardTypeName.subtypeOf(Object.class)),
                            ReservedNames.GAUGE_NAME)
                    // TODO(ckozak): Update to use a method which can log a warning and replace existing gauges.
                    // See MetricRegistries.registerWithReplacement.
                    .addStatement(
                            "$L.$L($L(), $L)",
                            ReservedNames.REGISTRY_NAME,
                            metricRegistryMethod,
                            buildMetricName.name,
                            ReservedNames.GAUGE_NAME);
        } else {
            buildMethodBuilder.addStatement(
                    "return $L.$L($L)", ReservedNames.REGISTRY_NAME, metricRegistryMethod, metricNameBlock);
        }
        MethodSpec buildMethod = buildMethodBuilder.build();
        List<MethodSpec> buildMethods =
                isGauge ? ImmutableList.of(buildMethod, buildMetricName) : ImmutableList.of(buildMethod);

        outerBuilder.addType(TypeSpec.classBuilder(Custodian.anyToUpperCamel(metricName) + "Builder")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .addSuperinterfaces(tagList.stream()
                        .map(tag -> ClassName.bestGuess(stageName(metricName, tag.getName())))
                        .collect(ImmutableList.toImmutableList()))
                .addSuperinterface(ClassName.bestGuess(buildStage(metricName)))
                .addFields(tagList.stream()
                        .map(tag -> FieldSpec.builder(
                                        getTagClassName(metricName, tag),
                                        Custodian.sanitizeName(tag.getName()),
                                        Modifier.PRIVATE)
                                .build())
                        .collect(ImmutableList.toImmutableList()))
                .addMethods(buildMethods)
                .addMethods(tagList.stream()
                        .map(tag -> MethodSpec.methodBuilder(Custodian.sanitizeName(tag.getName()))
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(Override.class)
                                .returns(ClassName.bestGuess(Custodian.anyToUpperCamel(metricName) + "Builder"))
                                .addParameter(ParameterSpec.builder(
                                                getTagClassName(metricName, tag), Custodian.sanitizeName(tag.getName()))
                                        .addAnnotation(Safe.class)
                                        .build())
                                .addStatement(
                                        "$1T.checkState(this.$2L == null, $3S)",
                                        Preconditions.class,
                                        Custodian.sanitizeName(tag.getName()),
                                        tag.getName() + " is already set")
                                .addStatement(
                                        "this.$1L = $2T.checkNotNull($1L, $3S)",
                                        Custodian.sanitizeName(tag.getName()),
                                        Preconditions.class,
                                        tag.getName() + " is required")
                                .addStatement("return this")
                                .build())
                        .collect(ImmutableList.toImmutableList()))
                .build());

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Custodian.sanitizeName(metricName))
                .addModifiers(visibility.apply())
                .returns(
                        ClassName.bestGuess(stageName(metricName, tagList.get(0).getName())))
                .addAnnotation(CheckReturnValue.class)
                .addStatement("return new $T()", ClassName.bestGuess(Custodian.anyToUpperCamel(metricName) + "Builder"))
                .addJavadoc(Javadoc.render(definition.getDocs()));
        outerBuilder.addMethod(methodBuilder.build());
    }

    private static int numArgs(MetricDefinition definition) {
        return (int) definition.getTagDefinitions().stream()
                        .filter(UtilityGenerator::tagDefinitionRequiresParam)
                        .count()
                // Gauges require a gauge argument.
                + (MetricType.GAUGE.equals(definition.getType()) ? 1 : 0);
    }

    private static boolean tagDefinitionRequiresParam(TagDefinition tagDefinition) {
        return tagDefinition.getValues().size() != 1;
    }

    private static ClassName getTagClassName(String metricName, TagDefinition tag) {
        if (tag.getValues().isEmpty()) {
            return ClassName.get(String.class);
        }
        return ClassName.bestGuess(
                Custodian.anyToUpperCamel(metricName) + "_" + Custodian.anyToUpperCamel(tag.getName()));
    }

    private static String stageName(String metricName, String tag) {
        return Custodian.anyToUpperCamel(metricName) + "Builder" + Custodian.anyToUpperCamel(tag) + "Stage";
    }

    private static String buildStage(String metricName) {
        return Custodian.anyToUpperCamel(metricName) + "BuildStage";
    }

    private UtilityGenerator() {}
}

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
import com.palantir.logsafe.Preconditions;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.Modifier;

final class UtilityGenerator {

    static JavaFile generateUtilityClass(
            String namespace,
            MetricNamespace metrics,
            Optional<String> libraryName,
            String packageName,
            ImplementationVisibility visibility) {
        ClassName className =
                ClassName.get(packageName, className(metrics.getShortName().orElse(namespace)));
        TypeSpec.Builder builder = TypeSpec.classBuilder(className.simpleName())
                .addModifiers(visibility.apply(Modifier.FINAL))
                .addJavadoc(Javadoc.render(metrics.getDocs()))
                .addMethod(MethodSpec.methodBuilder(ReservedNames.FACTORY_METHOD)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(TaggedMetricRegistry.class, ReservedNames.REGISTRY_NAME)
                        .addStatement(
                                "return new $T($T.checkNotNull(registry, \"TaggedMetricRegistry\"))",
                                className,
                                Preconditions.class)
                        .returns(className)
                        .build())
                .addField(TaggedMetricRegistry.class, ReservedNames.REGISTRY_NAME, Modifier.PRIVATE, Modifier.FINAL);
        if (libraryName.isPresent()) {
            builder.addField(FieldSpec.builder(
                            String.class, ReservedNames.LIBRARY_NAME, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", libraryName.get())
                    .build());
            builder.addField(FieldSpec.builder(
                            String.class,
                            ReservedNames.LIBRARY_VERSION,
                            Modifier.PRIVATE,
                            Modifier.STATIC,
                            Modifier.FINAL)
                    .initializer(
                            "$T.ofNullable($T.class.getPackage().getImplementationVersion()).orElse(\"unknown\")",
                            Optional.class,
                            className)
                    .build());
        }

        builder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(TaggedMetricRegistry.class, ReservedNames.REGISTRY_NAME)
                .addStatement("this.$1L = $1L", ReservedNames.REGISTRY_NAME)
                .build());

        // metrics
        for (Map.Entry<String, MetricDefinition> entry : metrics.getMetrics().entrySet()) {
            String metricName = entry.getKey();
            MetricDefinition definition = entry.getValue();
            generateMetricFactory(namespace, metricName, libraryName, definition, builder, visibility);
        }

        builder.addMethod(generateToString(className));

        return JavaFile.builder(className.packageName(), builder.build())
                .skipJavaLangImports(true)
                .indent("    ")
                .build();
    }

    private static String className(String namespace) {
        return Custodian.anyToUpperCamel(namespace) + "Metrics";
    }

    private static CodeBlock metricName(
            String namespace, String metricName, Optional<String> libraryName, Set<String> tagNames) {
        String safeName = namespace + '.' + metricName;
        CodeBlock.Builder builder = CodeBlock.builder().add("$T.builder().safeName($S)", MetricName.class, safeName);
        tagNames.forEach(tagName -> builder.add(".putSafeTags($S, $L)", tagName, Custodian.sanitizeName(tagName)));
        if (libraryName.isPresent()) {
            builder.add(".putSafeTags(\"libraryName\", $L)", ReservedNames.LIBRARY_NAME);
            builder.add(".putSafeTags(\"libraryVersion\", $L)", ReservedNames.LIBRARY_VERSION);
        }
        return builder.add(".build()").build();
    }

    private static MethodSpec generateToString(ClassName className) {
        return MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addStatement(
                        "return $S + $L + '}'",
                        String.format("%s{%s=", className.simpleName(), ReservedNames.REGISTRY_NAME),
                        ReservedNames.REGISTRY_NAME)
                .returns(String.class)
                .build();
    }

    private static void generateMetricFactory(
            String namespace,
            String metricName,
            Optional<String> libraryName,
            MetricDefinition definition,
            TypeSpec.Builder builder,
            ImplementationVisibility visibility) {
        if (numArgs(definition) <= 1) {
            builder.addMethod(generateSimpleMetricFactory(namespace, metricName, libraryName, definition, visibility));
        } else {
            generateMetricFactoryBuilder(namespace, metricName, libraryName, definition, builder, visibility);
        }
    }

    private static MethodSpec generateSimpleMetricFactory(
            String namespace,
            String metricName,
            Optional<String> libraryName,
            MetricDefinition definition,
            ImplementationVisibility visibility) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Custodian.sanitizeName(metricName))
                .addModifiers(visibility.apply())
                .returns(MetricTypes.type(definition.getType()))
                .addParameters(definition.getTags().stream()
                        .map(tag -> ParameterSpec.builder(String.class, Custodian.sanitizeName(tag))
                                .build())
                        .collect(ImmutableList.toImmutableList()))
                .addJavadoc(Javadoc.render(definition.getDocs()));
        CodeBlock metricNameBlock = metricName(namespace, metricName, libraryName, definition.getTags());
        String metricRegistryMethod = MetricTypes.registryAccessor(definition.getType());
        if (MetricType.GAUGE.equals(definition.getType())) {
            methodBuilder.addParameter(
                    ParameterizedTypeName.get(ClassName.get(Gauge.class), WildcardTypeName.subtypeOf(Object.class)),
                    ReservedNames.GAUGE_NAME);
            // TODO(ckozak): Update to use a method which can log a warning and replace existing gauges.
            // See MetricRegistries.registerWithReplacement.
            methodBuilder.addStatement(
                    "$L.$L($L, $L)",
                    ReservedNames.REGISTRY_NAME,
                    metricRegistryMethod,
                    metricNameBlock,
                    ReservedNames.GAUGE_NAME);
        } else {
            methodBuilder.addStatement(
                    "return $L.$L($L)", ReservedNames.REGISTRY_NAME, metricRegistryMethod, metricNameBlock);
        }
        return methodBuilder.build();
    }

    private static void generateMetricFactoryBuilder(
            String namespaceName,
            String metricName,
            Optional<String> libraryName,
            MetricDefinition definition,
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
        }
        outerBuilder.addType(TypeSpec.interfaceBuilder(buildStage(metricName))
                .addModifiers(visibility.apply())
                .addMethod(abstractBuildMethodBuilder.build())
                .build());
        ImmutableList<String> tagList = ImmutableList.copyOf(definition.getTags());
        for (int i = 0; i < tagList.size(); i++) {
            boolean lastTag = i == tagList.size() - 1;
            String tag = tagList.get(i);
            outerBuilder.addType(TypeSpec.interfaceBuilder(stageName(metricName, tag))
                    .addModifiers(visibility.apply())
                    .addMethod(MethodSpec.methodBuilder(Custodian.sanitizeName(tag))
                            .addParameter(String.class, Custodian.sanitizeName(tag))
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(ClassName.bestGuess(
                                    lastTag ? buildStage(metricName) : stageName(metricName, tagList.get(i + 1))))
                            .build())
                    .build());
        }
        CodeBlock metricNameBlock = metricName(namespaceName, metricName, libraryName, definition.getTags());
        String metricRegistryMethod = MetricTypes.registryAccessor(definition.getType());

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
                            "$L.$L($L, $L)",
                            ReservedNames.REGISTRY_NAME,
                            metricRegistryMethod,
                            metricNameBlock,
                            ReservedNames.GAUGE_NAME);
        } else {
            buildMethodBuilder.addStatement(
                    "return $L.$L($L)", ReservedNames.REGISTRY_NAME, metricRegistryMethod, metricNameBlock);
        }

        outerBuilder.addType(TypeSpec.classBuilder(Custodian.anyToUpperCamel(metricName) + "Builder")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .addSuperinterfaces(tagList.stream()
                        .map(tag -> ClassName.bestGuess(stageName(metricName, tag)))
                        .collect(ImmutableList.toImmutableList()))
                .addSuperinterface(ClassName.bestGuess(buildStage(metricName)))
                .addFields(tagList.stream()
                        .map(tag -> FieldSpec.builder(String.class, Custodian.sanitizeName(tag), Modifier.PRIVATE)
                                .build())
                        .collect(ImmutableList.toImmutableList()))
                .addMethod(buildMethodBuilder.build())
                .addMethods(tagList.stream()
                        .map(tag -> MethodSpec.methodBuilder(Custodian.sanitizeName(tag))
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(Override.class)
                                .returns(ClassName.bestGuess(Custodian.anyToUpperCamel(metricName) + "Builder"))
                                .addParameter(String.class, Custodian.sanitizeName(tag))
                                .addStatement(
                                        "$1T.checkState(this.$2L == null, $3S)",
                                        Preconditions.class,
                                        Custodian.sanitizeName(tag),
                                        tag + " is already set")
                                .addStatement(
                                        "this.$1L = $2T.checkNotNull($1L, $3S)",
                                        Custodian.sanitizeName(tag),
                                        Preconditions.class,
                                        tag + " is required")
                                .addStatement("return this")
                                .build())
                        .collect(ImmutableList.toImmutableList()))
                .build());

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Custodian.sanitizeName(metricName))
                .addModifiers(visibility.apply())
                .returns(ClassName.bestGuess(stageName(metricName, tagList.get(0))))
                .addStatement("return new $T()", ClassName.bestGuess(Custodian.anyToUpperCamel(metricName) + "Builder"))
                .addJavadoc(Javadoc.render(definition.getDocs()));
        outerBuilder.addMethod(methodBuilder.build());
    }

    private static int numArgs(MetricDefinition definition) {
        return definition.getTags().size()
                // Gauges require a gauge argument.
                + (MetricType.GAUGE.equals(definition.getType()) ? 1 : 0);
    }

    private static String stageName(String metricName, String tag) {
        return Custodian.anyToUpperCamel(metricName) + "Builder" + Custodian.anyToUpperCamel(tag) + "Stage";
    }

    private static String buildStage(String metricName) {
        return Custodian.anyToUpperCamel(metricName) + "BuildStage";
    }

    private UtilityGenerator() {}
}

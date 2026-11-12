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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.palantir.goethe.Goethe;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeUncheckedIoException;
import com.palantir.metric.schema.model.ImplementationVisibility;
import com.squareup.javapoet.JavaFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.io.output.StringBuilderWriter;

public final class JavaGenerator {
    // TODO(forozco): consider splitting this out into an API package
    /**
     * Specifies under which package Java classes should be generated.
     */
    private static final String JAVA_PACKAGE = "javaPackage";

    /**
     * Specifies visibility of generated Utility class. Defaults to public
     */
    private static final String JAVA_VISIBILITY = "javaVisibility";

    @CanIgnoreReturnValue
    public static List<Path> generate(JavaGeneratorArgs args) {
        List<JavaFile> javaFiles = SchemaParser.get().parseFile(args.input()).stream()
                .flatMap(schema -> schema.getNamespaces().entrySet().stream()
                        .map(entry -> UtilityGenerator.generateUtilityClass(
                                entry.getKey(),
                                entry.getValue(),
                                args.libraryName(),
                                getPackage(args, schema),
                                getVisibility(schema))))
                .collect(ImmutableList.toImmutableList());
        Properties metadata = new Properties();
        args.libraryVersion()
                .ifPresent(version -> metadata.setProperty(UtilityGenerator.LIBRARY_VERSION_PROPERTY, version));
        // Generated code only reads the resource when it has a libraryName
        args.libraryName()
                .ifPresent(libraryName -> javaFiles.stream()
                        .map(javaFile -> javaFile.packageName)
                        .distinct()
                        .forEach(packageName -> writeMetadataResource(
                                args.resourcesOutput(),
                                packageName,
                                UtilityGenerator.metadataResourceName(libraryName),
                                metadata)));
        return javaFiles.stream()
                .map(javaFile -> Goethe.formatAndEmit(javaFile, args.javaOutput()))
                .collect(ImmutableList.toImmutableList());
    }

    private static void writeMetadataResource(
            Path resourcesOutput, String packageName, String resourceName, Properties metadata) {
        Path packageDir = resourcesOutput;
        for (String component : Splitter.on('.').split(packageName)) {
            packageDir = packageDir.resolve(component);
        }
        Path resource = packageDir.resolve(resourceName);
        try {
            // StringBuilderWriter rather than StringWriter, which synchronizes on a StringBuffer
            StringBuilderWriter writer = new StringBuilderWriter();
            metadata.store(writer, /* comments= */ null);
            // Properties.store always writes a timestamp comment, which would change the resource on every build
            String contents = writer.toString()
                    .lines()
                    .filter(line -> !line.startsWith("#"))
                    .map(line -> line + "\n")
                    .collect(Collectors.joining());
            Files.createDirectories(packageDir);
            Files.writeString(resource, contents, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SafeUncheckedIoException(
                    "Failed to write metadata resource", e, SafeArg.of("resource", resource));
        }
    }

    private static String getPackage(JavaGeneratorArgs args, MetricSchema schema) {
        return Optional.ofNullable(schema.getOptions().get(JAVA_PACKAGE)).orElseGet(args::defaultPackageName);
    }

    private static ImplementationVisibility getVisibility(MetricSchema schema) {
        return Optional.ofNullable(schema.getOptions().get(JAVA_VISIBILITY))
                .map(ImplementationVisibility::fromString)
                .orElse(ImplementationVisibility.PUBLIC);
    }

    private JavaGenerator() {}
}

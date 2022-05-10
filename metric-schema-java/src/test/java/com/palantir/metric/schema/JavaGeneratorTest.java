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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.palantir.conjure.java.serialization.ObjectMappers;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import com.palantir.metric.schema.lang.MetricSchemaCompiler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JavaGeneratorTest {
    private final ObjectMapper mapper = ObjectMappers.newClientObjectMapper();

    private static final String REFERENCE_FILES_FOLDER = "src/integrationInput/java";

    private static final String INJECT_REFERENCE_FILES_FOLDER = "src/integrationInputWithInject/java";

    @TempDir
    public Path outputDir;

    @TempDir
    public Path inputDir;

    @Test
    void generates_code_without_inject() {
        JavaGenerator.generate(JavaGeneratorArgs.builder()
                        .output(outputDir)
                        .input(compileAndEmit(listFiles(Paths.get("src/test/resources"), _path -> true)))
                        .defaultPackageName("com.palantir.test")
                        .libraryName("witchcraft")
                        .build())
                .stream()
                .map(outputDir::relativize)
                .map(Path::toString)
                .forEach(relativePath ->
                        assertThatFilesAreTheSame(outputDir.resolve(relativePath), REFERENCE_FILES_FOLDER));
    }

    @Test
    void generates_code_with_inject() {
        JavaGenerator.generate(JavaGeneratorArgs.builder()
                        .output(outputDir)
                        .input(compileAndEmit(listFiles(Paths.get("src/test/resources"), path -> path.getFileName()
                                .startsWith("server.yml"))))
                        .defaultPackageName("com.palantir.test")
                        .libraryName("witchcraft")
                        .generateInjectAnnotation(true)
                        .build())
                .stream()
                .map(outputDir::relativize)
                .map(Path::toString)
                .forEach(relativePath ->
                        assertThatFilesAreTheSame(outputDir.resolve(relativePath), INJECT_REFERENCE_FILES_FOLDER));
    }

    private void assertThatFilesAreTheSame(Path outputFile, String referenceFilesFolder) {
        Path relativized = outputDir.relativize(outputFile);
        Path expectedFile = Paths.get(referenceFilesFolder, relativized.toString());
        if (Boolean.parseBoolean(System.getProperty("recreate", "false"))) {
            try {
                Files.createDirectories(expectedFile.getParent());
                Files.deleteIfExists(expectedFile);
                Files.copy(outputFile, expectedFile);
            } catch (IOException e) {
                throw new SafeRuntimeException("Failed to recreate test data", e);
            }
        }
        assertThat(outputFile).hasSameTextualContentAs(expectedFile);
    }

    private List<Path> listFiles(Path path, Predicate<Path> filter) {
        Preconditions.checkArgument(Files.isDirectory(path), "Expected a directory", SafeArg.of("path", path));
        try (Stream<Path> stream = Files.list(path)) {
            return stream.filter(filter).collect(ImmutableList.toImmutableList());
        } catch (IOException e) {
            throw new SafeRuntimeException("Failed to list directory", e, SafeArg.of("path", path));
        }
    }

    private Path compileAndEmit(List<Path> inputFiles) {
        Path outputFile = inputDir.resolve("metrics.json");
        try {
            mapper.writeValue(
                    outputFile.toFile(),
                    inputFiles.stream().map(MetricSchemaCompiler::compile).collect(ImmutableSet.toImmutableSet()));
            return outputFile;
        } catch (IOException e) {
            throw new SafeRuntimeException("Failed to compile", e, SafeArg.of("inputFiles", inputFiles));
        }
    }
}

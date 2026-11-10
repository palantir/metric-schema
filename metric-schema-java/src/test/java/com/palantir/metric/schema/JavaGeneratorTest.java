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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.palantir.conjure.java.serialization.ObjectMappers;
import com.palantir.logsafe.Preconditions;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeNullPointerException;
import com.palantir.logsafe.exceptions.SafeUncheckedIoException;
import com.palantir.metric.schema.lang.MetricSchemaCompiler;
import com.palantir.test.MonitorsMetrics;
import com.palantir.test.missingversionkey.MissingVersionKeyMetrics;
import com.palantir.test.providedversion.ProvidedVersionMetrics;
import com.palantir.tritium.metrics.registry.DefaultTaggedMetricRegistry;
import com.palantir.tritium.metrics.registry.MetricName;
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

    @TempDir
    public Path outputDir;

    @TempDir
    public Path inputDir;

    @TempDir
    public Path resourcesDir;

    @Test
    void generates_code() {
        JavaGenerator.generate(JavaGeneratorArgs.builder()
                        .javaOutput(outputDir)
                        .resourcesOutput(resourcesDir)
                        .input(compileAndEmit(listFiles(Paths.get("src/test/resources"))))
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
    void specific_libraryVersion() {
        JavaGenerator.generate(JavaGeneratorArgs.builder()
                        .javaOutput(outputDir)
                        .resourcesOutput(resourcesDir)
                        .input(compileAndEmit(ImmutableList.of(
                                Paths.get("src/test/resources/provided-version/provided-version.yml"),
                                Paths.get("src/test/resources/missing-version-key/missing-version-key.yml"))))
                        .defaultPackageName("com.palantir.test")
                        .libraryName("specific_libraryVersion")
                        .libraryVersion("1.0.0")
                        .build())
                .stream()
                .map(outputDir::relativize)
                .map(Path::toString)
                .forEach(relativePath ->
                        assertThatFilesAreTheSame(outputDir.resolve(relativePath), REFERENCE_FILES_FOLDER));

        assertThat(resourcesDir.resolve("com/palantir/test/providedversion/metric-schema.properties"))
                .hasContent("libraryVersion=1.0.0\n");
    }

    @Test
    void libraryVersion_writesOneResourcePerPackage() throws IOException {
        List<Path> generated = JavaGenerator.generate(JavaGeneratorArgs.builder()
                .javaOutput(outputDir)
                .resourcesOutput(resourcesDir)
                .input(compileAndEmit(listFiles(Paths.get("src/test/resources"))))
                .defaultPackageName("com.palantir.test")
                .libraryName("witchcraft")
                .libraryVersion("1.0.0")
                .build());

        assertThat(generated).hasSizeGreaterThan(1);
        try (Stream<Path> files = Files.walk(resourcesDir)) {
            assertThat(files.filter(Files::isRegularFile).map(resourcesDir::relativize))
                    .containsExactly(Paths.get("com/palantir/test/metric-schema.properties"));
        }
    }

    @Test
    void libraryVersion_doesNotChangeGeneratedSources() throws IOException {
        Path schema =
                compileAndEmit(ImmutableList.of(Paths.get("src/test/resources/provided-version/provided-version.yml")));
        Path unversionedOutput = Files.createDirectory(outputDir.resolve("unversioned"));
        Path versionedOutput = Files.createDirectory(outputDir.resolve("versioned"));

        List<Path> unversioned = JavaGenerator.generate(JavaGeneratorArgs.builder()
                .javaOutput(unversionedOutput)
                .resourcesOutput(resourcesDir)
                .input(schema)
                .defaultPackageName("com.palantir.test")
                .libraryName("specific_libraryVersion")
                .build());
        JavaGenerator.generate(JavaGeneratorArgs.builder()
                .javaOutput(versionedOutput)
                .resourcesOutput(resourcesDir)
                .input(schema)
                .defaultPackageName("com.palantir.test")
                .libraryName("specific_libraryVersion")
                .libraryVersion("1.0.0")
                .build());

        assertThat(unversioned)
                .isNotEmpty()
                .allSatisfy(file -> assertThat(file)
                        .hasSameTextualContentAs(versionedOutput.resolve(unversionedOutput.relativize(file))));
    }

    @Test
    public void testJavaVersionTag() {
        DefaultTaggedMetricRegistry registry = new DefaultTaggedMetricRegistry();
        MonitorsMetrics.of(registry).more("value").mark();
        MetricName key = Iterables.getOnlyElement(registry.getMetrics().keySet());
        assertThat(key.safeTags().get("javaVersion")).matches("\\d+\\.\\d+(\\.\\d+)+");
    }

    @Test
    void libraryVersionTag_readFromResourceBesideClass() {
        DefaultTaggedMetricRegistry registry = new DefaultTaggedMetricRegistry();
        ProvidedVersionMetrics.of(registry).utilization(() -> 1);
        MetricName key = Iterables.getOnlyElement(registry.getMetrics().keySet());
        assertThat(key.safeTags()).containsEntry("libraryVersion", "1.0.0");
    }

    @Test
    void libraryVersionTag_resourceWithoutVersionFailsClassInitialization() {
        DefaultTaggedMetricRegistry registry = new DefaultTaggedMetricRegistry();
        assertThatThrownBy(() -> MissingVersionKeyMetrics.of(registry))
                .isInstanceOf(ExceptionInInitializerError.class)
                .cause()
                .isInstanceOf(SafeNullPointerException.class)
                .hasMessageContaining("Library version resource is missing libraryVersion");
    }

    @Test
    void libraryVersionTag_fallsBackWithoutResource() {
        DefaultTaggedMetricRegistry registry = new DefaultTaggedMetricRegistry();
        MonitorsMetrics.of(registry).more("value").mark();
        MetricName key = Iterables.getOnlyElement(registry.getMetrics().keySet());
        // Classes run from a directory, so there is no manifest Implementation-Version either
        assertThat(key.safeTags()).containsEntry("libraryVersion", "unknown");
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
                throw new SafeUncheckedIoException("Failed to recreate test data", e);
            }
        }
        assertThat(outputFile).hasSameTextualContentAs(expectedFile);
    }

    private List<Path> listFiles(Path path) {
        Preconditions.checkArgument(Files.isDirectory(path), "Expected a directory", SafeArg.of("path", path));
        try (Stream<Path> stream = Files.list(path)) {
            return stream.filter(Files::isRegularFile).collect(ImmutableList.toImmutableList());
        } catch (IOException e) {
            throw new SafeUncheckedIoException("Failed to list directory", e, SafeArg.of("path", path));
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
            throw new SafeUncheckedIoException("Failed to compile", e, SafeArg.of("inputFiles", inputFiles));
        }
    }
}

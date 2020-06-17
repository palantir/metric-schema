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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JavaGeneratorTest {
    private static final String REFERENCE_FILES_FOLDER = "src/integrationInput/java";

    @TempDir
    public Path tempDir;

    @Test
    void generates_code() {
        JavaGenerator.generate(JavaGeneratorArgs.builder()
                        .output(tempDir)
                        .inputs(listFiles(Paths.get("src/test/resources")))
                        .defaultPackageName("com.palantir.test")
                        .libraryName("witchcraft")
                        .build())
                .stream()
                .map(tempDir::relativize)
                .map(Path::toString)
                .forEach(relativePath ->
                        assertThatFilesAreTheSame(tempDir.resolve(relativePath), REFERENCE_FILES_FOLDER));
    }

    private void assertThatFilesAreTheSame(Path outputFile, String referenceFilesFolder) {
        Path relativized = tempDir.relativize(outputFile);
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
        assertThat(outputFile).hasSameContentAs(expectedFile);
    }

    private List<Path> listFiles(Path path) {
        Preconditions.checkArgument(Files.isDirectory(path), "Expected a directory", SafeArg.of("path", path));
        try (Stream<Path> stream = Files.list(path)) {
            return stream.collect(ImmutableList.toImmutableList());
        } catch (IOException e) {
            throw new SafeRuntimeException("Failed to list directory", e, SafeArg.of("path", path));
        }
    }
}

/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.metric.schema.datadog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DataDogGeneratorTest {
    private static final String REFERENCE_FILES_FOLDER = "src/test/resources";

    @TempDir public Path tempDir;

    @Test
    void generates_code() throws IOException {
        Path output = tempDir.resolve("dashboard.json");
        DataDogGenerator.generate(Paths.get("src/test/resources/server.yml"), output);

        assertThatFilesAreTheSame(output, REFERENCE_FILES_FOLDER);
    }

    private void assertThatFilesAreTheSame(Path outputFile, String referenceFilesFolder) throws IOException {
        Path relativized = tempDir.relativize(outputFile);
        Path expectedFile = Paths.get(referenceFilesFolder, relativized.toString());
        if (Boolean.parseBoolean(System.getProperty("recreate", "false"))) {
            Files.createDirectories(expectedFile.getParent());
            Files.deleteIfExists(expectedFile);
            Files.copy(outputFile, expectedFile);
        }
        assertThat(outputFile).hasSameContentAs(expectedFile);
    }
}

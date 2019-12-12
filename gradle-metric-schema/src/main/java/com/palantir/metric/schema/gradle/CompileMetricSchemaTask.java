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

package com.palantir.metric.schema.gradle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableSet;
import com.palantir.conjure.java.serialization.ObjectMappers;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import com.palantir.metric.schema.MetricSchema;
import java.io.File;
import java.io.IOException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

@CacheableTask
public class CompileMetricSchemaTask extends SourceTask {
    private static final ObjectReader reader = ObjectMappers.withDefaultModules(new ObjectMapper(new YAMLFactory()))
            .readerFor(MetricSchema.class);
    private static final ObjectWriter writer = ObjectMappers.newServerObjectMapper().writer();

    private final RegularFileProperty outputFile = getProject().getObjects().fileProperty();

    @OutputFile
    public final RegularFileProperty getOutputFile() {
        return outputFile;
    }

    @TaskAction
    public final void action() throws IOException {
        File output = getOutputFile().getAsFile().get();
        getProject().mkdir(output.getParent());

        writer.writeValue(
                output,
                getSource().getFiles().stream()
                        .map(CompileMetricSchemaTask::readFile)
                        .collect(ImmutableSet.toImmutableSet()));
    }

    private static MetricSchema readFile(File file) {
        try {
            return reader.readValue(file);
        } catch (IOException e) {
            throw new SafeRuntimeException("Failed to deserialize file", e, SafeArg.of("file", file));
        }
    }
}

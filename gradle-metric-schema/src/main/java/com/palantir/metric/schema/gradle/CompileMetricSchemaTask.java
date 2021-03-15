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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.palantir.conjure.java.serialization.ObjectMappers;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import com.palantir.metric.schema.MetricSchema;
import com.palantir.metric.schema.lang.LangConverter;
import java.io.File;
import java.io.IOException;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

@CacheableTask
public abstract class CompileMetricSchemaTask extends DefaultTask {
    private static final ObjectReader reader = ObjectMappers.withDefaultModules(new ObjectMapper(new YAMLFactory()))
            .readerFor(com.palantir.metric.schema.lang.MetricSchema.class);

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSource();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public final void action() throws IOException {
        File output = getOutputFile().getAsFile().get();
        getProject().mkdir(output.getParent());

        com.palantir.metric.schema.gradle.ObjectMappers.mapper.writeValue(
                output,
                getSource().getFiles().stream()
                        .map(CompileMetricSchemaTask::readFile)
                        .peek(CompileMetricSchemaTask::validate)
                        .collect(ImmutableSet.toImmutableSet()));
    }

    private static void validate(MetricSchema schema) {
        schema.getNamespaces().forEach((name, namespace) -> {
            namespace.getMetrics().forEach((metricName, metricDefinition) -> {
                Sets.SetView<String> valuesWithoutTags =
                        Sets.difference(metricDefinition.getValues().keySet(), metricDefinition.getTags());
                if (!valuesWithoutTags.isEmpty()) {
                    throw new GradleException(String.format(
                            "metric '%s' in namespace '%s' has values %s without corresponding tags",
                            metricName, name, valuesWithoutTags));
                }
            });
        });
    }

    private static MetricSchema readFile(File file) {
        try {
            return LangConverter.toApi(reader.readValue(file));
        } catch (IOException e) {
            throw new SafeRuntimeException("Failed to deserialize file", e, SafeArg.of("file", file));
        }
    }
}

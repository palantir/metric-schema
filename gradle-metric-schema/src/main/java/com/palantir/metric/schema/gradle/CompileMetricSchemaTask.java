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

import com.google.common.collect.ImmutableSet;
import com.palantir.metric.schema.lang.MetricSchemaCompiler;
import java.io.File;
import java.io.IOException;
import org.gradle.api.DefaultTask;
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
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSource();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public final void action() throws IOException {
        File output = getOutputFile().getAsFile().get();
        getProject().mkdir(output.getParent());

        ObjectMappers.mapper.writeValue(
                output,
                getSource().getFiles().stream()
                        .map(File::toPath)
                        .map(MetricSchemaCompiler::compile)
                        .collect(ImmutableSet.toImmutableSet()));
    }
}

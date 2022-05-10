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

import com.palantir.metric.schema.JavaGenerator;
import com.palantir.metric.schema.JavaGeneratorArgs;
import java.io.File;
import java.util.Optional;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GFileUtils;

@CacheableTask
public abstract class GenerateMetricSchemaTask extends DefaultTask {
    private final Property<String> libraryName =
            getProject().getObjects().property(String.class).value(defaultLibraryName());
    private final Property<Boolean> generateDaggerAnnotations =
            getProject().getObjects().property(Boolean.class).value(false);

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputFile();

    @Input
    public final Property<String> getLibraryName() {
        return libraryName;
    }

    @Input
    public final Property<Boolean> getGenerateDaggerAnnotations() {
        return generateDaggerAnnotations;
    }

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public final void generate() {
        File output = getOutputDir().getAsFile().get();
        GFileUtils.deleteDirectory(output);
        getProject().mkdir(output);

        JavaGenerator.generate(JavaGeneratorArgs.builder()
                .input(getInputFile().getAsFile().get().toPath())
                .output(output.toPath())
                .libraryName(Optional.ofNullable(libraryName.getOrNull()))
                .generateDaggerAnnotations(getGenerateDaggerAnnotations().get())
                // TODO(forozco): probably want something better
                .defaultPackageName(getProject().getGroup().toString())
                .build());
    }

    private String defaultLibraryName() {
        String rootProjectName = getProject().getRootProject().getName();
        return rootProjectName.replaceAll("-root$", "");
    }
}

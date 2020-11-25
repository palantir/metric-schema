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
import com.palantir.metric.schema.JavaGenerator;
import com.palantir.metric.schema.JavaGeneratorArgs;
import java.io.File;
import java.util.Optional;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GFileUtils;

@CacheableTask
public class GenerateMetricSchemaTask extends SourceTask {
    private final DirectoryProperty outputDirectory = getProject().getObjects().directoryProperty();
    private final Property<String> libraryName = getProject().getObjects().property(String.class);

    public GenerateMetricSchemaTask() {
        libraryName.set(defaultLibraryName());
    }

    // @Override
    // @InputFiles
    // @SkipWhenEmpty
    // @PathSensitive(PathSensitivity.RELATIVE)
    // public final FileTree getSource() {
    //     return super.getSource();
    // }

    @Input
    public final Property<String> getLibraryName() {
        return libraryName;
    }

    @OutputDirectory
    public final DirectoryProperty getOutputDir() {
        return outputDirectory;
    }

    @TaskAction
    public final void generate() {
        File output = getOutputDir().getAsFile().get();
        GFileUtils.deleteDirectory(output);
        getProject().mkdir(output);

        JavaGenerator.generate(JavaGeneratorArgs.builder()
                .inputs(getSource().getFiles().stream().map(File::toPath).collect(ImmutableSet.toImmutableSet()))
                .output(output.toPath())
                .libraryName(Optional.ofNullable(libraryName.getOrNull()))
                // TODO(forozco): probably want something better
                .defaultPackageName(getProject().getGroup().toString())
                .build());
    }

    private String defaultLibraryName() {
        String rootProjectName = getProject().getRootProject().getName();
        return rootProjectName.replaceAll("-root$", "");
    }
}

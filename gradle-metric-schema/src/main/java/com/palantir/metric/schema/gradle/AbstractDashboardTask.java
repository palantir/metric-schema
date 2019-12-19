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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.palantir.metric.schema.Dashboard;
import com.palantir.metric.schema.MetricSchema;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GFileUtils;

public abstract class AbstractDashboardTask extends DefaultTask {

    private static final ObjectMapper json = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final RegularFileProperty manifestFile = getProject().getObjects().fileProperty();
    private final DirectoryProperty outputDirectory = getProject().getObjects().directoryProperty();

    @InputFile
    public final RegularFileProperty getManifestFile() {
        return manifestFile;
    }

    @OutputDirectory
    public final DirectoryProperty getOutputDirectory() {
        return outputDirectory;
    }

    @TaskAction
    public final void generate() throws IOException {
        File manifest = getManifestFile().getAsFile().get();
        Map<String, List<MetricSchema>> schemas = CreateMetricsManifestTask.mapper
                .readValue(manifest, new TypeReference<Map<String, List<MetricSchema>>>() {});
        if (schemas.isEmpty()) {
            return;
        }

        schemas.values().stream()
                .flatMap(Collection::stream)
                .flatMap(metricSchema -> metricSchema.getDashboards().entrySet().stream())
                .forEach(entry -> {
                    try {
                        GFileUtils.mkdirs(getOutputDirectory().getAsFile().get());
                        json.writeValue(
                                getOutputDirectory().file(entry.getKey() + "." + dashboardExtension())
                                        .get()
                                        .getAsFile(),
                                renderDashboard(entry.getValue()));
                    } catch (IOException e) {
                        throw new GradleException("Failed to render dashboard to file", e);
                    }
                });
    }

    protected abstract String dashboardExtension();

    protected abstract Object renderDashboard(Dashboard dashboard);

}

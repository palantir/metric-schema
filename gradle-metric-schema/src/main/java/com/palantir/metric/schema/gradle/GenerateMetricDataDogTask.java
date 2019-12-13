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
import com.palantir.metric.schema.MetricSchema;
import com.palantir.metric.schema.datadog.DashboardConfig;
import com.palantir.metric.schema.datadog.DataDogRenderer;
import com.palantir.metric.schema.datadog.api.TemplateVariable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GFileUtils;

@CacheableTask
public class GenerateMetricDataDogTask extends DefaultTask {

    private final RegularFileProperty manifestFile = getProject().getObjects().fileProperty();
    private final RegularFileProperty outputFile = getProject().getObjects().fileProperty();

    @Input
    public final Property<String> getDashboardTitle() {
        return getProject().getExtensions().getByType(MetricSchemaDataDogExtension.class).getTitle();
    }

    @Input
    public final Property<String> getDashboardDescription() {
        return getProject().getExtensions().getByType(MetricSchemaDataDogExtension.class).getDescription();
    }

    @Input
    public final MapProperty<String, String> getSelectedTags() {
        return getProject().getExtensions().getByType(MetricSchemaDataDogExtension.class).getSelectedTags();
    }

    @Input
    public final ListProperty<String> getTemplateVariables() {
        return getProject().getExtensions().getByType(MetricSchemaDataDogExtension.class).getTemplateVariables();
    }

    @InputFile
    public final RegularFileProperty getManifestFile() {
        return manifestFile;
    }

    @OutputFile
    public final RegularFileProperty outputFile() {
        return outputFile;
    }

    @TaskAction
    public final void generate() throws IOException {
        File manifest = getManifestFile().getAsFile().get();
        Map<String, List<MetricSchema>> schemas = CreateMetricsManifestTask.mapper.readValue(
                manifest, new TypeReference<Map<String, List<MetricSchema>>>() {});
        if (schemas.isEmpty()) {
            return;
        }

        DashboardConfig dashboardConfig = DashboardConfig.builder()
                .title(getDashboardTitle().get())
                .description(getDashboardDescription().get())
                .putAllSelectedTags(getSelectedTags().get())
                .addAllTemplateVariables(getTemplateVariables().get().stream()
                        .map(TemplateVariable::of)
                        .collect(Collectors.toList()))
                .build();

        GFileUtils.writeFile(
                DataDogRenderer.render(
                        dashboardConfig,
                        schemas.values().stream()
                                .flatMap(List::stream)
                                .flatMap(schema -> schema.getGraphs().stream())
                                .collect(Collectors.toList())),
                outputFile.get().getAsFile());
    }

}

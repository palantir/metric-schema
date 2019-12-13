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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

public final class MetricSchemaDataDogPlugin implements Plugin<Project> {

    public static final String EXT_DATADOG = "datadog";
    public static final String TASK_GENERATE_METRICS_DATADOG = "generateMetricsDataDog";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(LifecycleBasePlugin.class);
        project.getPluginManager().apply(MetricSchemaPlugin.class);

        MetricSchemaDataDogExtension dataDogExtension = project.getExtensions()
                .create(EXT_DATADOG, MetricSchemaDataDogExtension.class, project);

        TaskProvider<CreateMetricsManifestTask> createMetricsManifest =
                project.getTasks().named(MetricSchemaPlugin.CREATE_METRICS_MANIFEST, CreateMetricsManifestTask.class);

        project.getTasks()
                .register(TASK_GENERATE_METRICS_DATADOG, GenerateMetricDataDogTask.class, task -> {
                    task.getDashboardTitle().set(dataDogExtension.getTitle());
                    task.getDashboardDescription().set(dataDogExtension.getDescription());
                    task.getSelectedTags().set(dataDogExtension.getSelectedTags());

                    task.getManifestFile().set(createMetricsManifest.flatMap(CreateMetricsManifestTask::getOutputFile));
                    task.outputFile().set(project.getBuildDir().toPath().resolve("datadog.json").toFile());
                    task.dependsOn(createMetricsManifest);
                });
    }
}

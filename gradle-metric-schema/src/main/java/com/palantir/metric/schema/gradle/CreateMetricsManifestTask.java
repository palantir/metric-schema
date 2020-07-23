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
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.palantir.metric.schema.MetricSchema;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

@CacheableTask
public class CreateMetricsManifestTask extends DefaultTask {
    private static final Logger log = Logging.getLogger(CreateMetricsManifestTask.class);

    private final RegularFileProperty metricsFile = getProject().getObjects().fileProperty();
    private final Property<Configuration> configuration =
            getProject().getObjects().property(Configuration.class);
    private final RegularFileProperty outputFile = getProject().getObjects().fileProperty();
    private final SetProperty<String> projectDependencies =
            getProject().getObjects().setProperty(String.class);

    @org.gradle.api.tasks.Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public final RegularFileProperty getMetricsFile() {
        return metricsFile;
    }

    @Classpath
    public final Property<Configuration> getConfiguration() {
        return configuration;
    }

    @Input
    public final SetProperty<String> getProjectDependencies() {
        return projectDependencies;
    }

    @OutputFile
    public final RegularFileProperty getOutputFile() {
        return outputFile;
    }

    public CreateMetricsManifestTask() {
        // Depend on all CompileMetricSchema in the repo just in case since the tasks are relatively cheap
        getProject().getRootProject().getAllprojects().forEach(sibling -> {
            dependsOn(sibling.getTasks().withType(CompileMetricSchemaTask.class));
        });
    }

    @TaskAction
    public final void createManifest() throws IOException {
        File output = getOutputFile().getAsFile().get();
        getProject().mkdir(output.getParent());

        ObjectMappers.mapper.writeValue(
                output,
                ImmutableMap.builder()
                        .putAll(getLocalMetrics())
                        .putAll(getExternalDiscoveredMetrics())
                        .putAll(getLocalDiscoveredMetrics())
                        .build());
    }

    private Map<String, List<MetricSchema>> getLocalMetrics() {
        if (getMetricsFile().getAsFile().isPresent()) {
            return ImmutableMap.of(
                    getProjectCoordinates(getProject()),
                    ObjectMappers.loadMetricSchema(getMetricsFile().getAsFile().get()));
        }
        return Collections.emptyMap();
    }

    private Map<String, List<MetricSchema>> getExternalDiscoveredMetrics() {
        ImmutableMap.Builder<String, List<MetricSchema>> discoveredMetrics = ImmutableMap.builder();

        configuration.get().getResolvedConfiguration().getResolvedArtifacts().forEach(artifact -> {
            ComponentIdentifier id = artifact.getId().getComponentIdentifier();
            getExternalMetrics(id, artifact).ifPresent(metrics -> discoveredMetrics.put(id.toString(), metrics));
        });

        return discoveredMetrics.build();
    }

    private Map<String, List<MetricSchema>> getLocalDiscoveredMetrics() {
        ImmutableMap.Builder<String, List<MetricSchema>> discoveredMetrics = ImmutableMap.builder();
        getProjectDependencies().get().stream().map(getProject()::project).forEach(project -> {
            getProjectDependencyMetrics(project)
                    .ifPresent(metrics -> discoveredMetrics.put(getProjectCoordinates(project), metrics));
        });
        return discoveredMetrics.build();
    }

    private static Optional<List<MetricSchema>> getProjectDependencyMetrics(Project dependencyProject) {
        if (!dependencyProject.getPlugins().hasPlugin(MetricSchemaPlugin.class)) {
            return Optional.empty();
        }
        CompileMetricSchemaTask compileMetricSchemaTask = (CompileMetricSchemaTask)
                dependencyProject.getTasks().getByName(MetricSchemaPlugin.COMPILE_METRIC_SCHEMA);

        File file = compileMetricSchemaTask.getOutputFile().get().getAsFile();
        if (!file.isFile()) {
            log.debug("File {} does not exist", file);
            return Optional.empty();
        }
        return Optional.of(ObjectMappers.loadMetricSchema(file));
    }

    private static java.util.Optional<List<MetricSchema>> getExternalMetrics(
            ComponentIdentifier id, ResolvedArtifact artifact) {
        if (!artifact.getFile().exists()) {
            log.debug("Artifact did not exist: {}", artifact.getFile());
            return java.util.Optional.empty();
        } else if (!Files.getFileExtension(artifact.getFile().getName()).equals("jar")) {
            log.debug("Artifact is not jar: {}", artifact.getFile());
            return java.util.Optional.empty();
        }

        try {
            ZipFile zipFile = new ZipFile(artifact.getFile());
            ZipEntry manifestEntry = zipFile.getEntry(MetricSchemaPlugin.METRIC_SCHEMA_RESOURCE);
            if (manifestEntry == null) {
                log.debug("Manifest file does not exist in JAR: {}", id);
                return java.util.Optional.empty();
            }

            try (InputStream is = zipFile.getInputStream(manifestEntry)) {
                return java.util.Optional.of(
                        ObjectMappers.mapper.readValue(is, new TypeReference<List<MetricSchema>>() {}));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load external monitors");
        }
    }

    private static String getProjectCoordinates(Project project) {
        // We explicitly exclude the version for project dependencies so that the output of the task does not depend on
        // project version and is more likely to be cached
        return String.format("%s:%s:$projectVersion", project.getGroup(), project.getName());
    }

    static Configuration removeProjectDependencies(Project project, Configuration parentConf) {
        Configuration existingConfig = project.getConfigurations().findByName("metricManifestDependencies");
        if (existingConfig != null) {
            return existingConfig;
        }

        Configuration conf = project.getConfigurations().create("metricManifestDependencies");
        parentConf.getIncoming().getResolutionResult().getAllDependencies().forEach(dependency -> {
            ComponentSelector selector = dependency.getRequested();
            if (selector instanceof ModuleComponentSelector) {
                ModuleComponentSelector mod = (ModuleComponentSelector) selector;
                conf.getDependencies()
                        .add(project.getDependencies()
                                .create(mod.getGroup() + ":" + mod.getModule() + ":" + mod.getVersion()));
            }
        });

        return conf;
    }
}

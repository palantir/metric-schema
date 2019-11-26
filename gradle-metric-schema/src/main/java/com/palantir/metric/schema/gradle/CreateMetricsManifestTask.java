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
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.palantir.conjure.java.serialization.ObjectMappers;
import com.palantir.metric.schema.MetricSchema;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public class CreateMetricsManifestTask extends DefaultTask {
    private static final Logger log = Logging.getLogger(CreateMetricsManifestTask.class);
    static final ObjectMapper mapper = ObjectMappers.newClientObjectMapper();

    private final RegularFileProperty metricsFile = getProject().getObjects().fileProperty();
    private final Property<Configuration> configuration = getProject().getObjects().property(Configuration.class);
    private final RegularFileProperty outputFile = getProject().getObjects().fileProperty();

    @InputFile
    @Optional
    public final RegularFileProperty getMetricsFile() {
        return metricsFile;
    }

    @Classpath
    public final Property<Configuration> getConfiguration() {
        return configuration;
    }

    @OutputFile
    public final RegularFileProperty getOutputFile() {
        return outputFile;
    }

    public CreateMetricsManifestTask() {
        dependsOn(otherProjectMetricSchemaTasks());
    }

    /**
     * A lazy collection of tasks that ensure the {@link CompileMetricSchemaTask} task of any project dependencies from
     * {@link #configuration} are executed.
     */
    private Provider<FileCollection> otherProjectMetricSchemaTasks() {
        return getConfiguration().map(productDeps -> {
            // Using a ConfigurableFileCollection simply because it implements Buildable and provides a convenient API
            // to wire up task dependencies to it in a lazy way.
            ConfigurableFileCollection emptyFileCollection = getProject().files();
            productDeps.getIncoming().getArtifacts().getArtifacts().stream()
                    .flatMap(artifact -> {
                        ComponentIdentifier id = artifact.getId().getComponentIdentifier();

                        // Depend on the ConfigureProductDependenciesTask, if it exists, which will wire up the jar
                        // manifest
                        // with recommended product dependencies.
                        if (id instanceof ProjectComponentIdentifier) {
                            Project dependencyProject = getProject()
                                    .getRootProject()
                                    .project(((ProjectComponentIdentifier) id).getProjectPath());
                            return Stream.of(dependencyProject.getTasks().withType(CompileMetricSchemaTask.class));
                        }
                        return Stream.empty();
                    })
                    .forEach(emptyFileCollection::builtBy);
            return emptyFileCollection;
        });
    }

    @TaskAction
    public final void createManifest() throws IOException {
        File output = getOutputFile().getAsFile().get();
        getProject().mkdir(output.getParent());

        mapper.writeValue(
                output, ImmutableMap.builder().putAll(getLocalMetrics()).putAll(discoverMetricSchema()).build());
    }

    private Map<String, List<MetricSchema>> getLocalMetrics() throws IOException {
        if (getMetricsFile().getAsFile().isPresent()) {
            return ImmutableMap.of(getProjectCoordinates(getProject()), mapper.readValue(
                    getMetricsFile().getAsFile().get(), new TypeReference<List<MetricSchema>>() {}));
        }
        return Collections.emptyMap();
    }

    private Map<String, List<MetricSchema>> discoverMetricSchema() {
        ImmutableMap.Builder<String, List<MetricSchema>> discoveredMetrics = ImmutableMap.builder();

        configuration.get().getIncoming().getArtifacts().getArtifacts().forEach(artifact -> {
            String artifactName = artifact.getId().getDisplayName();
            ComponentIdentifier id = artifact.getId().getComponentIdentifier();
            String sourceCoordinates;
            InputStream metricSchemaStream;

            try {
                if (id instanceof ProjectComponentIdentifier) {
                    Project dependencyProject =
                            getProject().getRootProject().project(((ProjectComponentIdentifier) id).getProjectPath());
                    if (!dependencyProject.getPlugins().hasPlugin(MetricSchemaPlugin.class)) {
                        return;
                    }
                    CompileMetricSchemaTask compileMetricSchemaTask = (CompileMetricSchemaTask)
                            dependencyProject.getTasks().getByName(MetricSchemaPlugin.COMPILE_METRIC_SCHEMA);

                    File file = compileMetricSchemaTask.getOutputFile().get().getAsFile();
                    if (!file.isFile()) {
                        log.debug("File {} does not exist", file);
                        return;
                    }
                    sourceCoordinates = getProjectCoordinates(dependencyProject);
                    metricSchemaStream = Files.asByteSource(file).openStream();
                } else {
                    if (!artifact.getFile().exists()) {
                        log.debug("Artifact did not exist: {}", artifact.getFile());
                        return;
                    } else if (!Files.getFileExtension(artifact.getFile().getName())
                            .equals("jar")) {
                        log.debug("Artifact is not jar: {}", artifact.getFile());
                        return;
                    }

                    ZipFile zipFile = new ZipFile(artifact.getFile());
                    ZipEntry manifestEntry = zipFile.getEntry(MetricSchemaPlugin.METRIC_SCHEMA_RESOURCE);
                    if (manifestEntry == null) {
                        log.debug("Manifest file does not exist in jar: {}", id);
                        return;
                    }

                    sourceCoordinates = id.toString();
                    metricSchemaStream = zipFile.getInputStream(manifestEntry);
                }
            } catch (IOException e) {
                log.warn(
                        "IOException encountered when processing artifact '{}', file '{}', {}",
                        artifactName,
                        artifact.getFile(),
                        e);
                return;
            }

            try (InputStream is = metricSchemaStream) {
                discoveredMetrics.put(
                        sourceCoordinates, mapper.readValue(is, new TypeReference<List<MetricSchema>>() {}));
            } catch (IOException | IllegalArgumentException e) {
                log.debug("Failed to load metric schema for artifact '{}', file '{}', '{}'", artifactName, artifact, e);
            }
        });

        return discoveredMetrics.build();
    }

    private static String getProjectCoordinates(Project project) {
        // We explicitly exclude the version for project dependencies so that the output of the task does not depend on
        // project version and is more likely to be cached
        return String.format("%s:%s:$projectVersion", project.getGroup(), project.getName());
    }
}

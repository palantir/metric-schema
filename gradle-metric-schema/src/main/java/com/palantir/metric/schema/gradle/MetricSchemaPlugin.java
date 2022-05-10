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
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;

public final class MetricSchemaPlugin implements Plugin<Project> {
    private static final String TASK_GROUP = "MetricSchema";
    public static final String METRIC_SCHEMA_RESOURCE = "metric-schema/metrics.json";

    public static final String COMPILE_METRIC_SCHEMA = "compileMetricSchema";
    public static final String CHECK_METRICS_MARKDOWN = "checkMetricsMarkdown";
    public static final String GENERATE_METRICS_MARKDOWN = "generateMetricsMarkdown";
    public static final String CREATE_METRICS_MANIFEST = "createMetricsManifest";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaLibraryPlugin.class);
        SourceDirectorySet sourceSet = createSourceSet(project);
        Provider<Directory> metricSchemaDir =
                project.getLayout().getBuildDirectory().dir("metricSchema");

        TaskProvider<CompileMetricSchemaTask> compileSchemaTask =
                createCompileSchemaTask(project, metricSchemaDir, sourceSet);

        Provider<Directory> generatedJavaOutputDir = metricSchemaDir.map(file -> file.dir("generated_src"));
        TaskProvider<GenerateMetricSchemaTask> generateMetricsTask = project.getTasks()
                .register("generateMetrics", GenerateMetricSchemaTask.class, task -> {
                    task.setGroup(TASK_GROUP);
                    task.setDescription("Generates bindings for producing well defined metrics");
                    task.getInputFile().set(compileSchemaTask.flatMap(CompileMetricSchemaTask::getOutputFile));
                    task.getOutputDir().set(generatedJavaOutputDir);
                    task.getGenerateInjectAnnotation().set(project.provider(() -> hasInjectionFramework(project)));
                });
        project.getTasks().named("compileJava", compileJava -> compileJava.dependsOn(generateMetricsTask));

        configureJavaSource(project, generatedJavaOutputDir);
        configureIdea(project, generateMetricsTask, generatedJavaOutputDir);
        configureEclipse(project, generateMetricsTask);
        configureProjectDependencies(project);

        createManifestTask(project, metricSchemaDir, compileSchemaTask);
        project.getPluginManager()
                .withPlugin("com.palantir.sls-java-service-distribution", _plugin -> project.getPluginManager()
                        .apply(MetricSchemaMarkdownPlugin.class));
    }

    private static boolean hasInjectionFramework(Project project) {
        Configuration compileClasspath = project.getConfigurations().getByName("compileClasspath");
        return compileClasspath.getResolvedConfiguration().getResolvedArtifacts().stream()
                .map(ResolvedArtifact::getId)
                .map(ComponentArtifactIdentifier::getComponentIdentifier)
                .filter(ModuleComponentIdentifier.class::isInstance)
                .map(ModuleComponentIdentifier.class::cast)
                .anyMatch(id ->
                        id.getGroup().equals("jakarta.inject") && id.getModule().equals("jakarta.inject-api"));
    }

    private static void createManifestTask(
            Project project,
            Provider<Directory> metricSchemaDir,
            TaskProvider<CompileMetricSchemaTask> compileSchemaTask) {
        Provider<RegularFile> manifestFile = metricSchemaDir.map(dir -> dir.file("manifest.json"));
        Configuration runtimeClasspath = project.getConfigurations().getByName("runtimeClasspath");
        project.getTasks().register(CREATE_METRICS_MANIFEST, CreateMetricsManifestTask.class, task -> {
            task.getMetricsFile().set(compileSchemaTask.flatMap(CompileMetricSchemaTask::getOutputFile));
            task.getOutputFile().set(manifestFile);
            task.getConfiguration().set(runtimeClasspath);
            task.dependsOn(compileSchemaTask);
        });
    }

    private static TaskProvider<CompileMetricSchemaTask> createCompileSchemaTask(
            Project project, Provider<Directory> metricSchemaDir, SourceDirectorySet sourceSet) {
        Provider<RegularFile> schemaFile = metricSchemaDir.map(dir -> dir.file(METRIC_SCHEMA_RESOURCE));
        JavaPluginConvention javaPlugin = project.getConvention().getPlugin(JavaPluginConvention.class);
        TaskProvider<CompileMetricSchemaTask> compileMetricSchema = project.getTasks()
                .register(COMPILE_METRIC_SCHEMA, CompileMetricSchemaTask.class, task -> {
                    task.getSource().from(sourceSet);
                    task.getOutputFile().set(schemaFile);
                });
        project.getTasks()
                .named("processResources", processResources -> processResources.dependsOn(compileMetricSchema));

        javaPlugin.getSourceSets().getByName("main").resources(resources -> {
            SourceDirectorySet sourceDir = project.getObjects()
                    .sourceDirectorySet("metricSchema", "metric schema")
                    .srcDir(metricSchemaDir);
            sourceDir.include("metric-schema/**");
            resources.source(sourceDir);
        });

        return compileMetricSchema;
    }

    private static SourceDirectorySet createSourceSet(Project project) {
        SourceDirectorySet sourceSet =
                project.getObjects().sourceDirectorySet("metricSchema", "Metric Schema source set");
        sourceSet.srcDir("src/main/metrics");
        sourceSet.include("**/*.yml");
        return sourceSet;
    }

    private static void configureJavaSource(Project project, Provider<Directory> outputDir) {
        JavaPluginConvention javaPlugin = project.getConvention().findPlugin(JavaPluginConvention.class);
        javaPlugin.getSourceSets().getByName("main").getJava().srcDir(outputDir);
    }

    private static void configureEclipse(Project project, TaskProvider<? extends Task> generateMetrics) {
        project.getPluginManager().withPlugin("eclipse", _plugin -> {
            Task task = project.getTasks().findByName("eclipseClasspath");
            if (task != null) {
                task.dependsOn(generateMetrics);
            }
        });
    }

    private static void configureIdea(
            Project project, TaskProvider<? extends Task> generateMetrics, Provider<Directory> outputDir) {
        project.getPluginManager().withPlugin("idea", _plugin -> {
            project.getTasks().getByName("ideaModule", task -> task.dependsOn(generateMetrics));
            project.getExtensions().configure(IdeaModel.class, idea -> {
                IdeaModule module = idea.getModule();
                module.getSourceDirs().add(outputDir.get().getAsFile());
                module.getGeneratedSourceDirs().add(outputDir.get().getAsFile());
            });
        });
    }

    private static void configureProjectDependencies(Project project) {
        project.getDependencies().add("api", "com.palantir.tritium:tritium-registry");
        project.getDependencies().add("api", "com.palantir.safe-logging:preconditions");
        project.getDependencies().add("api", "com.google.errorprone:error_prone_annotations");
    }
}

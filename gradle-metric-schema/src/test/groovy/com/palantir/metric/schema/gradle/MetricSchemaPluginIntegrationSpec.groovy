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

package com.palantir.metric.schema.gradle

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.base.Throwables
import com.palantir.metric.schema.MetricSchema
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import nebula.test.IntegrationSpec
import nebula.test.dependencies.DependencyGraph
import nebula.test.dependencies.GradleDependencyGenerator

class MetricSchemaPluginIntegrationSpec extends IntegrationSpec {
    public static final String METRICS = """
        namespaces:
          server:
            docs: General web server metrics.
            metrics:
              response.size:
                type: histogram
                tags:
                  - service-name
                  - endpoint
                docs: A histogram of the number of bytes written into the response.
              worker.utilization:
                type: gauge
                docs: A gauge of the ratio of active workers to the number of workers.
        """.stripIndent()

    public static final String METRICS_INVALID = """
        namespaces:
          server:
            docs: General web server metrics.
            metrics:
              response.size:
                type: histogram
                tags:
                  - service_name
                  - endpoint
                docs: A histogram of the number of bytes written into the response.
        """.stripIndent()

    void setup() {
        buildFile << """
        allprojects {
            ${applyPlugin(MetricSchemaPlugin.class)}
            group 'com.palantir.test'

            repositories {
                jcenter()
                maven {
                    url 'https://dl.bintray.com/palantir/releases/'
                }
            }

            configurations.all {
                resolutionStrategy {
                    force 'com.palantir.tritium:tritium-registry:${Versions.TRITIUM}'
                    force 'com.palantir.safe-logging:preconditions:${Versions.SAFE_LOGGING}'
                }
            }
        }
        """.stripIndent()
    }

    def 'generates java'() {
        when:
        file('src/main/metrics/metrics.yml') << METRICS

        then:
        def result = runTasksSuccessfully('classes')
        result.wasExecuted(':generateMetrics')
        fileExists("build/metricSchema/generated_src/com/palantir/test/ServerMetrics.java")
    }

    def 'invalid schema results in task failure'() {
        when:
        file('src/main/metrics/metrics.yml') << METRICS_INVALID

        then:
        def result = runTasksWithFailure('classes')
        result.wasExecuted(':generateMetrics')
        Throwables.getRootCause(result.getFailure()).getMessage().contains("tags must match pattern")
    }

    def 'missing definition results in task failure'() {
        when:
        file('src/main/metrics/metrics.yml') << """
            namespaces:
              aa.namespace:
                docs: test metrics
                metrics:
                  my.custom.metric:
            """

        then:
        def result = runTasksWithFailure('classes')
        result.wasExecuted(':generateMetrics')
        Throwables.getRootCause(result.getFailure()).getMessage().contains("MetricDefinition is required")
    }

    def 'embeds metrics into jar'() {
        when:
        buildFile << """
        task unJar(type: Sync, dependsOn: jar) {
            from { zipTree(tasks.jar.archiveFile) }
            into file("unjar")
        }
        """.stripIndent()
        file('src/main/metrics/metrics.yml') << METRICS

        then:
        def result = runTasksSuccessfully(':unJar')
        result.wasExecuted(':compileMetricSchema')
        fileExists("unjar/metric-schema/metrics.json")
        !CreateMetricsManifestTask.mapper.readValue(
                file('unjar/metric-schema/metrics.json'),
                new TypeReference<List<MetricSchema>>() {}).isEmpty()
    }

    def "createManifest discovers metric schema"() {
        when:
        def dependencyGraph = new DependencyGraph('a:a:1.0')
        GradleDependencyGenerator generator = new GradleDependencyGenerator(
                dependencyGraph, new File(projectDir, "build/testrepogen").toString())
        def mavenRepo = generator.generateTestMavenRepo()

        Files.copy(
                MetricSchemaPluginIntegrationSpec.getResourceAsStream("/a-1.0.jar"),
                new File(mavenRepo, "a/a/1.0/a-1.0.jar").toPath(),
                StandardCopyOption.REPLACE_EXISTING)

        buildFile << """
        group 'com.palantir.test'

        repositories {
            maven {url "file:///${mavenRepo.getAbsolutePath()}"}
        }
        dependencies {
            compile 'a:a:1.0'
        }
        """.stripIndent()

        then:
        def result = runTasksSuccessfully(':createMetricsManifest')

        then:
        fileExists('build/metricSchema/manifest.json')

        def manifest = CreateMetricsManifestTask.mapper.readValue(file("build/metricSchema/manifest.json"), Map.class)
        !manifest.isEmpty()
        manifest['a:a:1.0'] != null
    }

    def "createManifest discovers in repo metric schema"() {
        setup:
        addSubproject("foo-lib", "")
        file('foo-lib/src/main/metrics/metric.yml') << METRICS

        addSubproject("foo-server", """
            dependencies {
                compile project(':foo-lib')
            }
        """.stripIndent())

        when:
        def result = runTasksSuccessfully(':foo-server:createMetricsManifest')

        then:
        result.wasExecuted(":foo-lib:compileMetricSchema")
        result.wasExecuted(":foo-server:compileMetricSchema")
        !result.wasExecuted(':bar-lib:jar')

        fileExists('foo-server/build/metricSchema/manifest.json')
        def manifest = CreateMetricsManifestTask.mapper.readValue(file("foo-server/build/metricSchema/manifest.json"), Map.class)
        !manifest.isEmpty()
        manifest['com.palantir.test:foo-lib:$projectVersion'] != null
    }

}

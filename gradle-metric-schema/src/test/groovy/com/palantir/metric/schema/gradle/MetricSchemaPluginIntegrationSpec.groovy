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
import com.palantir.metric.schema.MetricSchema
import nebula.test.IntegrationSpec

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

    def "createManifest discovers in repo product dependencies"() {
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
        def manifest = CreateMetricsManifestTask.mapper.readValue(file("foo-server/build/metricSchema/manifest.json"), List.class)
        !manifest.isEmpty()
    }

}

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

import nebula.test.IntegrationSpec

class GenerateMetricMarkdownIntegrationSpec extends IntegrationSpec {
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
        ${applyPlugin(MetricSchemaMarkdownPlugin.class)}

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
        """.stripIndent()
    }

    def 'generate markdown'() {
        when:
        file('src/main/metrics/metrics.yml') << METRICS

        then:
        def result = runTasksSuccessfully('--write-locks')
        result.wasExecuted(':generateMetricsMarkdown')
        fileExists("metrics.md")
    }

    def 'fails if markdown does not exist'() {
        when:
        file('src/main/metrics/metrics.yml') << METRICS

        then:
        def result = runTasksWithFailure(':check')
        result.wasExecuted(':checkMetricsMarkdown')
        result.standardError.contains("metrics.md does not exist")
    }

    def 'fails if markdown is out of date'() {
        when:
        file('src/main/metrics/metrics.yml') << METRICS

        then:
        def result1 = runTasksSuccessfully('--write-locks')
        result1.wasExecuted(':generateMetricsMarkdown')
        fileExists("metrics.md")

        file('metrics.md') << "foo"
        def result2 = runTasksWithFailure(':check')
        result2.standardError.contains("metrics.md is out of date, please run `./gradlew generateMetricsMarkdown` "
                + "or `./gradlew --write-locks` to update it.")
    }
}

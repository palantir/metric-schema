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

package com.palantir.metric.schema.datadog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.palantir.metric.schema.Aggregation;
import com.palantir.metric.schema.AggregationFunction;
import com.palantir.metric.schema.Documentation;
import com.palantir.metric.schema.GraphDefinition;
import com.palantir.metric.schema.GraphWidget;
import com.palantir.metric.schema.MetricDefinition;
import com.palantir.metric.schema.MetricNamespace;
import com.palantir.metric.schema.MetricSchema;
import com.palantir.metric.schema.MetricType;
import com.palantir.metric.schema.Percentile;
import com.palantir.metric.schema.Timeseries;
import com.palantir.metric.schema.TimeseriesGraph;
import com.palantir.metric.schema.datadog.api.TemplateVariable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class DataDogRendererTest {

    private static final MetricSchema schema = MetricSchema.builder()
            .namespaces("First Namespace", MetricNamespace.builder()
                    .docs(Documentation.of("Docs for namespace"))
                    .metrics("server.response", MetricDefinition.builder()
                            .type(MetricType.TIMER)
                            .docs(Documentation.of("Docs for timer metric"))
                            .tags("service-name")
                            .tags("endpoint")
                            .build())
                    .graphs(GraphDefinition.builder()
                            .title("Server Response P99s")
                            .widget(GraphWidget.timeseries(TimeseriesGraph.builder()
                                    .series(Timeseries.builder()
                                            .percentile(Percentile.P95)
                                            .aggregation(Aggregation.of(AggregationFunction.MAX, ImmutableSet.of()))
                                            .build())
                                    .build()))
                            .metric("server.response")
                            .build())
                    .build())
            .namespaces("Second Namespace", MetricNamespace.builder()
                    .docs(Documentation.of("Docs for second namespace"))
                    .build())
            .build();

    @Test
    void render() throws IOException {
        DashboardConfig config = DashboardConfig.builder()
                .title("Dashboard Title")
                .description("Dashboard Description")
                .putSelectedTags("product", "artifacts")
                .addTemplateVariables(TemplateVariable.of("deployment"), TemplateVariable.of("environment"))
                .build();

        String rendered = DataDogRenderer.render(config, ImmutableList.of(schema));

        // FIXME(gatesn): remove
        Files.write(Paths.get("src/test/resources/render.json"), rendered.getBytes(StandardCharsets.UTF_8));

        assertThat(rendered).isEqualTo(contentOf(new File("src/test/resources/render.json")));
    }

}

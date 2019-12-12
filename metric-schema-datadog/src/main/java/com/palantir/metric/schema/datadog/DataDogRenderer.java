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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.palantir.metric.schema.GraphDefinition;
import com.palantir.metric.schema.GraphWidget;
import com.palantir.metric.schema.MetricDefinition;
import com.palantir.metric.schema.MetricNamespace;
import com.palantir.metric.schema.MetricSchema;
import com.palantir.metric.schema.Timeseries;
import com.palantir.metric.schema.TimeseriesGraph;
import com.palantir.metric.schema.datadog.api.Dashboard;
import com.palantir.metric.schema.datadog.api.DisplayType;
import com.palantir.metric.schema.datadog.api.LayoutType;
import com.palantir.metric.schema.datadog.api.Request;
import com.palantir.metric.schema.datadog.api.Widget;
import com.palantir.metric.schema.datadog.api.widgets.BaseWidget;
import com.palantir.metric.schema.datadog.api.widgets.GroupWidget;
import com.palantir.metric.schema.datadog.api.widgets.TimeseriesWidget;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public final class DataDogRenderer {

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .enable(SerializationFeature.INDENT_OUTPUT);

    private DataDogRenderer() {}

    public static String render(DashboardConfig config, Set<MetricSchema> schemas) throws IOException {
        return JSON.writeValueAsString(renderDashboard(config, schemas));
    }

    @VisibleForTesting
    static Dashboard renderDashboard(DashboardConfig config, Set<MetricSchema> schemas) {
        return Dashboard.builder()
                .title(config.title())
                .description(config.description())
                .layoutType(LayoutType.ORDERED)
                .readOnly(true)
                .templateVariables(config.templateVariables())
                .widgets(schemas.stream()
                        .flatMap(schema -> schema.getNamespaces().entrySet().stream()
                                .filter(entry -> !entry.getValue().getGraphs().isEmpty())
                                .map(entry -> renderGroup(config, entry.getKey(), entry.getValue())))
                        .map(widget -> Widget.builder().definition(widget).build())
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static GroupWidget renderGroup(DashboardConfig config, String name, MetricNamespace namespace) {
        return GroupWidget.builder()
                .title(name)
                .layoutType(LayoutType.ORDERED)
                .widgets(namespace.getGraphs().stream()
                        .map(graph -> {
                            return renderMetric(config, graph, namespace.getMetrics().get(graph.getMetric()));
                        })
                        .map(widget -> Widget.builder().definition(widget).build())
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static BaseWidget renderMetric(
            DashboardConfig config, GraphDefinition graph, MetricDefinition _metric) {
        return graph.getWidget().accept(new GraphWidget.Visitor<BaseWidget>() {
            @Override
            public BaseWidget visitTimeseries(TimeseriesGraph timeseriesGraph) {
                return TimeseriesWidget.builder()
                        .title(graph.getTitle())
                        .addAllRequests(timeseriesGraph.getSeries().stream()
                                .map(timeseries -> timeseriesRequest(config, graph.getMetric(), timeseries))
                                .collect(Collectors.toList()))
                        .build();
            }

            @Override
            public BaseWidget visitUnknown(String _unknownType) {
                throw new UnsupportedOperationException();
            }
        });
    }

    static Request timeseriesRequest(DashboardConfig config, String metricName, Timeseries timeseries) {
        return Request.builder()
                .query(Query.timer(metricName)
                        .percentile(timeseries.getPercentile())
                        .from(ImmutableSet.<Query.Selector>builder()
                                .addAll(config.selectedTags().entrySet().stream()
                                        .map(tag -> Query.TagSelector.of(tag.getKey(), tag.getValue()))
                                        .collect(Collectors.toSet()))
                                .addAll(config.templateVariables().stream()
                                        .map(Query.TemplateSelector::of)
                                        .collect(Collectors.toSet()))
                                .build())
                        .aggregate(timeseries.getAggregation())
                        .build())
                .displayType(DisplayType.LINE)
                .build();
    }

}

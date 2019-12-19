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

package com.palantir.metric.schema.grafana;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.palantir.metric.schema.GraphDefinition;
import com.palantir.metric.schema.GraphGroup;
import com.palantir.metric.schema.GraphWidget;
import com.palantir.metric.schema.Timeseries;
import com.palantir.metric.schema.TimeseriesGraph;
import com.palantir.metric.schema.api.QueryBuilder;
import com.palantir.metric.schema.grafana.api.Dashboard;
import com.palantir.metric.schema.grafana.api.panels.GraphPanel;
import com.palantir.metric.schema.grafana.api.panels.Panel;
import com.palantir.metric.schema.grafana.api.panels.RowPanel;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("StrictUnusedVariable")
public final class GrafanaRenderer {

    private static final QueryBuilder queryBuilder = new PrometheusQueryBuilder();
    private static final ObjectMapper JSON =
            new ObjectMapper().registerModule(new Jdk8Module()).enable(SerializationFeature.INDENT_OUTPUT);

    private GrafanaRenderer() {}

    public static String render(DashboardConfig config, List<GraphGroup> graphGroups) throws IOException {
        return JSON.writeValueAsString(renderDashboard(config, graphGroups));
    }

    @VisibleForTesting
    static Dashboard renderDashboard(DashboardConfig config, List<GraphGroup> graphGroups) {
        return Dashboard.builder()
                .title(config.title())
                .panels(graphGroups.stream()
                        .flatMap(group -> renderGroup(config, group).stream())
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static List<Panel> renderGroup(DashboardConfig config, GraphGroup graphGroup) {
        return ImmutableList.<Panel>builder()
                .add(RowPanel.builder()
                        .title(graphGroup.getTitle())
                        .build())
                .addAll(graphGroup.getDefinitions().stream()
                        .map(graph -> renderGraph(config, graph))
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static Panel renderGraph(DashboardConfig config, GraphDefinition graph) {
        return graph.getWidget().accept(new GraphWidget.Visitor<Panel>() {
            @Override
            public Panel visitTimeseries(TimeseriesGraph value) {
                return GraphPanel.builder()
                        .title(graph.getTitle())
                        .addAllTargets(value.getSeries().stream()
                                .map(timeseries -> timeseriesRequest(config, timeseries))
                                .collect(Collectors.toList()))
                        .build();
            }

            @Override
            public Panel visitUnknown(String _unknownType) {
                throw new UnsupportedOperationException();
            }
        });
    }

    static GraphPanel.Target timeseriesRequest(DashboardConfig config, Timeseries timeseries) {
        return GraphPanel.Target.of(QueryBuilder.create(
                queryBuilder,
                timeseries,
                config.selectedTags(),
                ImmutableList.of()));
    }
}

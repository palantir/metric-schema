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
import com.palantir.metric.schema.Cell;
import com.palantir.metric.schema.CellContent;
import com.palantir.metric.schema.Dashboard;
import com.palantir.metric.schema.Row;
import com.palantir.metric.schema.Timeseries;
import com.palantir.metric.schema.TimeseriesCell;
import com.palantir.metric.schema.api.QueryBuilder;
import com.palantir.metric.schema.grafana.api.GrafanaDashboard;
import com.palantir.metric.schema.grafana.api.panels.GraphPanel;
import com.palantir.metric.schema.grafana.api.panels.Panel;
import com.palantir.metric.schema.grafana.api.panels.RowPanel;
import java.io.IOException;
import java.util.stream.Collectors;

@SuppressWarnings("StrictUnusedVariable")
public final class GrafanaRenderer {

    private static final QueryBuilder queryBuilder = new PrometheusQueryBuilder();
    private static final ObjectMapper JSON =
            new ObjectMapper().registerModule(new Jdk8Module()).enable(SerializationFeature.INDENT_OUTPUT);

    private GrafanaRenderer() {}

    public static String render(DashboardConfig config, Dashboard dashboard) throws IOException {
        return JSON.writeValueAsString(renderDashboard(config, dashboard));
    }

    @VisibleForTesting
    static GrafanaDashboard renderDashboard(DashboardConfig config, Dashboard dashboard) {
        return GrafanaDashboard.builder()
                .title(dashboard.getTitle())
                .panels(dashboard.getRows().stream()
                        .map(row -> renderRow(config, row))
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static Panel renderRow(DashboardConfig config, Row row) {
        return RowPanel.builder()
                .title(row.getTitle())
                .panels(row.getCells().stream()
                        .map(cell -> renderCell(config, cell))
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static Panel renderCell(DashboardConfig config, Cell cell) {
        return cell.getContent().accept(new CellContent.Visitor<Panel>() {
            @Override
            public Panel visitTimeseries(TimeseriesCell timeseriesCell) {
                return GraphPanel.builder()
                        .title(cell.getTitle())
                        .addAllTargets(timeseriesCell.getSeries().stream()
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

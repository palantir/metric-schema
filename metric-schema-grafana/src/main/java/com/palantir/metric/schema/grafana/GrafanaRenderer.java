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

import com.google.common.annotations.VisibleForTesting;
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
import java.util.stream.Collectors;

@SuppressWarnings("StrictUnusedVariable")
public final class GrafanaRenderer {

    private static final QueryBuilder queryBuilder = new PrometheusQueryBuilder();

    private GrafanaRenderer() {}

    public static GrafanaDashboard render(Dashboard dashboard) {
        return GrafanaDashboard.builder()
                .title(dashboard.getTitle())
                .panels(dashboard.getRows().stream()
                        .map(row -> renderRow(dashboard, row))
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static Panel renderRow(Dashboard dashboard, Row row) {
        return RowPanel.builder()
                .title(row.getTitle())
                .panels(row.getCells().stream()
                        .map(cell -> renderCell(dashboard, cell))
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static Panel renderCell(Dashboard dashboard, Cell cell) {
        return cell.getContent().accept(new CellContent.Visitor<Panel>() {
            @Override
            public Panel visitTimeseries(TimeseriesCell timeseriesCell) {
                return GraphPanel.builder()
                        .title(cell.getTitle())
                        .addAllTargets(timeseriesCell.getSeries().stream()
                                .map(timeseries -> timeseriesRequest(dashboard, timeseries))
                                .collect(Collectors.toList()))
                        .build();
            }

            @Override
            public Panel visitUnknown(String _unknownType) {
                throw new UnsupportedOperationException();
            }
        });
    }

    static GraphPanel.Target timeseriesRequest(Dashboard dashboard, Timeseries timeseries) {
        return GraphPanel.Target.of(QueryBuilder.create(
                queryBuilder,
                timeseries,
                dashboard.getSelectedTags(),
                dashboard.getTemplatedTags()));
    }
}

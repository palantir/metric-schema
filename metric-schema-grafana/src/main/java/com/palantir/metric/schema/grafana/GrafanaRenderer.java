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
import com.palantir.metric.schema.grafana.api.TemplateVariable;
import com.palantir.metric.schema.grafana.api.Templating;
import com.palantir.metric.schema.grafana.api.panels.GraphPanel;
import com.palantir.metric.schema.grafana.api.panels.GridPosition;
import com.palantir.metric.schema.grafana.api.panels.Panel;
import com.palantir.metric.schema.grafana.api.panels.RowPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("StrictUnusedVariable")
public final class GrafanaRenderer {

    private static final QueryBuilder queryBuilder = new PrometheusQueryBuilder();

    private GrafanaRenderer() {}

    public static GrafanaDashboard render(Dashboard dashboard) {
        return GrafanaDashboard.builder()
                .title(dashboard.getTitle())
                .templating(Templating.builder()
                        .list(dashboard.getTemplatedTags().stream()
                                .map(tag -> TemplateVariable.builder().name(tag).build())
                                .collect(Collectors.toList()))
                        .build())
                .panels(dashboard.getRows().stream()
                        .flatMap(row -> renderRow(dashboard, row).stream())
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static List<Panel> renderRow(Dashboard dashboard, Row row) {
        List<Panel> panels = new ArrayList<>();
        panels.add(RowPanel.builder()
                .title(row.getTitle())
                .build());

        int xPosition = 0;
        for (Cell cell : row.getCells()) {
            Panel panel = renderCell(dashboard, cell, xPosition);
            panels.add(panel);

            // Grafana will push panels onto a new row if they exceed max width, but will push them as far right as
            // they can go if their xPos is high. So need to reset to zero if we exceed max width
            xPosition += panel.gridPos().width();
            if (xPosition >= GridPosition.MAX_WIDTH) {
                xPosition = 0;
            }
        }

        return panels;
    }

    @VisibleForTesting
    static Panel renderCell(Dashboard dashboard, Cell cell, int xPosition) {
        GridPosition gridPosition = GridPosition.builder().xPos(xPosition).build();
        return cell.getContent().accept(new CellContent.Visitor<Panel>() {
            @Override
            public Panel visitTimeseries(TimeseriesCell timeseriesCell) {
                return GraphPanel.builder()
                        .title(cell.getTitle())
                        .gridPos(gridPosition)
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

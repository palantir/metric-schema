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
import com.palantir.metric.schema.Cell;
import com.palantir.metric.schema.CellContent;
import com.palantir.metric.schema.Dashboard;
import com.palantir.metric.schema.Row;
import com.palantir.metric.schema.Timeseries;
import com.palantir.metric.schema.TimeseriesCell;
import com.palantir.metric.schema.api.QueryBuilder;
import com.palantir.metric.schema.datadog.api.DataDogDashboard;
import com.palantir.metric.schema.datadog.api.DisplayType;
import com.palantir.metric.schema.datadog.api.LayoutType;
import com.palantir.metric.schema.datadog.api.Request;
import com.palantir.metric.schema.datadog.api.TemplateVariable;
import com.palantir.metric.schema.datadog.api.Widget;
import com.palantir.metric.schema.datadog.api.widgets.BaseWidget;
import com.palantir.metric.schema.datadog.api.widgets.GroupWidget;
import com.palantir.metric.schema.datadog.api.widgets.TimeseriesWidget;
import java.io.IOException;
import java.util.stream.Collectors;

public final class DataDogRenderer {

    private static final QueryBuilder queryBuilder = new DataDogQueryBuilder();
    private static final ObjectMapper JSON =
            new ObjectMapper().registerModule(new Jdk8Module()).enable(SerializationFeature.INDENT_OUTPUT);

    private DataDogRenderer() {}

    public static String render(DashboardConfig config, Dashboard dashboard) throws IOException {
        return JSON.writeValueAsString(renderDashboard(config, dashboard));
    }

    @VisibleForTesting
    static DataDogDashboard renderDashboard(DashboardConfig config, Dashboard dashboard) {
        return DataDogDashboard.builder()
                .title(dashboard.getTitle())
                .layoutType(LayoutType.ORDERED)
                .readOnly(true)
                .templateVariables(config.templateVariables())
                .widgets(dashboard.getRows().stream()
                        .map(row -> renderRow(config, row))
                        .map(widget -> Widget.builder().definition(widget).build())
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static GroupWidget renderRow(DashboardConfig config, Row row) {
        return GroupWidget.builder()
                .title(row.getTitle())
                .layoutType(LayoutType.ORDERED)
                .widgets(row.getCells().stream()
                        .map(cell -> renderCell(config, cell))
                        .map(widget -> Widget.builder().definition(widget).build())
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static BaseWidget renderCell(DashboardConfig config, Cell cell) {
        return cell.getContent().accept(new CellContent.Visitor<BaseWidget>() {
            @Override
            public BaseWidget visitTimeseries(TimeseriesCell timeseriesCell) {
                return TimeseriesWidget.builder()
                        .title(cell.getTitle())
                        .addAllRequests(timeseriesCell.getSeries().stream()
                                .map(timeseries -> timeseriesRequest(config, timeseries))
                                .collect(Collectors.toList()))
                        .build();
            }

            @Override
            public BaseWidget visitUnknown(String _unknownType) {
                throw new UnsupportedOperationException();
            }
        });
    }

    static Request timeseriesRequest(DashboardConfig config, Timeseries timeseries) {
        return Request.builder()
                .query(QueryBuilder.create(
                        queryBuilder,
                        timeseries,
                        config.selectedTags(),
                        config.templateVariables().stream().map(TemplateVariable::name).collect(Collectors.toList())))
                .displayType(DisplayType.LINE)
                .build();
    }
}

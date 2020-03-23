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

import com.google.common.annotations.VisibleForTesting;
import com.palantir.metric.schema.Cell;
import com.palantir.metric.schema.CellContent;
import com.palantir.metric.schema.Dashboard;
import com.palantir.metric.schema.GroupedTimeseries;
import com.palantir.metric.schema.Row;
import com.palantir.metric.schema.Timeseries;
import com.palantir.metric.schema.TimeseriesCell;
import com.palantir.metric.schema.ValueCell;
import com.palantir.metric.schema.api.QueryBuilder;
import com.palantir.metric.schema.datadog.api.Aggregator;
import com.palantir.metric.schema.datadog.api.DataDogDashboard;
import com.palantir.metric.schema.datadog.api.DisplayType;
import com.palantir.metric.schema.datadog.api.LayoutType;
import com.palantir.metric.schema.datadog.api.Request;
import com.palantir.metric.schema.datadog.api.TemplateVariable;
import com.palantir.metric.schema.datadog.api.Widget;
import com.palantir.metric.schema.datadog.api.widgets.BaseWidget;
import com.palantir.metric.schema.datadog.api.widgets.GroupWidget;
import com.palantir.metric.schema.datadog.api.widgets.QueryValueWidget;
import com.palantir.metric.schema.datadog.api.widgets.TimeseriesWidget;
import java.util.stream.Collectors;

public final class DataDogRenderer {

    private static final QueryBuilder queryBuilder = new DataDogQueryBuilder();

    private DataDogRenderer() {}

    public static DataDogDashboard render(Dashboard dashboard) {
        return DataDogDashboard.builder()
                .title(dashboard.getTitle())
                .layoutType(LayoutType.ORDERED)
                .readOnly(true)
                .templateVariables(dashboard.getTemplatedTags().stream()
                        .map(TemplateVariable::of)
                        .collect(Collectors.toList()))
                .widgets(dashboard.getRows().stream()
                        .map(row -> renderRow(dashboard, row))
                        .map(widget -> Widget.builder().definition(widget).build())
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static GroupWidget renderRow(Dashboard dashboard, Row row) {
        return GroupWidget.builder()
                .title(row.getTitle())
                .layoutType(LayoutType.ORDERED)
                .widgets(row.getCells().stream()
                        .map(cell -> renderCell(dashboard, cell))
                        .map(widget -> Widget.builder().definition(widget).build())
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static BaseWidget renderCell(Dashboard dashboard, Cell cell) {
        return cell.getContent().accept(new CellContent.Visitor<BaseWidget>() {
            @Override
            public BaseWidget visitTimeseries(TimeseriesCell timeseriesCell) {
                return TimeseriesWidget.builder()
                        .title(cell.getTitle())
                        .addAllRequests(timeseriesCell.getSeries().stream()
                                .map(timeseries -> groupedTimeseriesRequest(dashboard, timeseries))
                                .collect(Collectors.toList()))
                        .build();
            }

            @Override
            public BaseWidget visitValue(ValueCell value) {
                return QueryValueWidget.builder()
                        .title(cell.getTitle())
                        .addRequests(Request.builder()
                                .from(timeseriesRequest(dashboard, value.getSeries()))
                                .aggregator(Aggregator.fromString(value.getSelection().toString().toLowerCase()))
                                .build())
                        .build();
            }

            @Override
            public BaseWidget visitUnknown(String _unknownType) {
                throw new UnsupportedOperationException();
            }
        });
    }

    static Request groupedTimeseriesRequest(Dashboard dashboard, GroupedTimeseries groupedTimeseries) {
        return Request.builder()
                .query(QueryBuilder.create(
                        queryBuilder,
                        groupedTimeseries,
                        dashboard.getSelectedTags(),
                        dashboard.getTemplatedTags()))
                .displayType(DisplayType.LINE)
                .build();
    }

    static Request timeseriesRequest(Dashboard dashboard, Timeseries timeseries) {
        return groupedTimeseriesRequest(dashboard, GroupedTimeseries.builder()
                .metric(timeseries.getMetric())
                .aggregation(timeseries.getAggregation())
                .build());
    }
}

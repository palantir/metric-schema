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

import com.google.common.collect.ImmutableSet;
import com.palantir.metric.schema.Aggregation;
import com.palantir.metric.schema.AggregationFunction;
import com.palantir.metric.schema.Cell;
import com.palantir.metric.schema.CellContent;
import com.palantir.metric.schema.Dashboard;
import com.palantir.metric.schema.Row;
import com.palantir.metric.schema.Timeseries;
import com.palantir.metric.schema.TimeseriesCell;
import com.palantir.metric.schema.datadog.api.DataDogDashboard;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class DataDogRendererTest {

    private static final Dashboard dashboard = Dashboard.builder()
            .title("First Group")
            .selectedTags("mytag", "myvalue")
            .templatedTags("templateTag")
            .rows(Row.builder()
                    .title("My Row")
                    .cells(Cell.builder()
                            .title("Client Response P95s")
                            .content(CellContent.timeseries(TimeseriesCell.builder()
                                    .series(Timeseries.builder()
                                            .metric("client.response.p95")
                                            .aggregation(Aggregation.of(
                                                    AggregationFunction.MAX,
                                                    ImmutableSet.of("host")))
                                            .build())
                                    .build()))
                            .build())
                    .build())
            .build();

    @Test
    void render() throws IOException {
        DataDogDashboard rendered = DataDogRenderer.render(dashboard);
        assertThat(rendered).isEqualTo(contentOf(new File("src/test/resources/render.json")));
    }
}

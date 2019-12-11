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
import com.palantir.metric.schema.MetricDefinition;
import com.palantir.metric.schema.MetricNamespace;
import com.palantir.metric.schema.MetricSchema;
import com.palantir.metric.schema.MetricType;
import com.palantir.metric.schema.datadog.api.Dashboard;
import com.palantir.metric.schema.datadog.api.LayoutType;
import com.palantir.metric.schema.datadog.api.Widget;
import com.palantir.metric.schema.datadog.api.widgets.BaseWidget;
import com.palantir.metric.schema.datadog.api.widgets.GroupWidget;
import com.palantir.metric.schema.datadog.api.widgets.TimeseriesWidget;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public final class DataDogRenderer {

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .enable(SerializationFeature.INDENT_OUTPUT);

    private DataDogRenderer() {}

    public static String render(String title, List<MetricSchema> schemas) throws IOException {
        return JSON.writeValueAsString(renderDashboard(title, schemas));
    }

    @VisibleForTesting
    static Dashboard renderDashboard(String title, List<MetricSchema> schemas) {
        return Dashboard.builder()
                .title(title)
                .layoutType(LayoutType.ORDERED)
                .readOnly(true)
                .widgets(schemas.stream()
                        .flatMap(schema -> schema.getNamespaces().entrySet().stream()
                                .map(entry -> renderGroup(entry.getKey(), entry.getValue())))
                        .map(widget -> Widget.builder().definition(widget).build())
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static GroupWidget renderGroup(String name, MetricNamespace namespace) {
        return GroupWidget.builder()
                .title(name)
                .layoutType(LayoutType.ORDERED)
                .widgets(namespace.getMetrics().entrySet().stream()
                        .map(entry -> renderMetric(entry.getKey(), entry.getValue()))
                        .map(widget -> Widget.builder().definition(widget).build())
                        .collect(Collectors.toList()))
                .build();
    }

    @VisibleForTesting
    static BaseWidget renderMetric(String name, MetricDefinition metric) {
        return metric.getType().accept(new MetricType.Visitor<BaseWidget>() {
            @Override
            public BaseWidget visitCounter() {
                throw new UnsupportedOperationException();
            }

            @Override
            public BaseWidget visitGauge() {
                throw new UnsupportedOperationException();
            }

            @Override
            public BaseWidget visitMeter() {
                throw new UnsupportedOperationException();
            }

            @Override
            public BaseWidget visitTimer() {
                return TimeseriesWidget.builder()
                        .title(name)
                        .addRequests(RequestBuilder
                                .timer(name)
                                .p95()
                                .build())
                        .build();
            }

            @Override
            public BaseWidget visitHistogram() {
                throw new UnsupportedOperationException();
            }

            @Override
            public BaseWidget visitUnknown(String unknownValue) {
                throw new UnsupportedOperationException();
            }
        });
    }

}

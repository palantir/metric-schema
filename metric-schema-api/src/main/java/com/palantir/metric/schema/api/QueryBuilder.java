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

package com.palantir.metric.schema.api;

import com.google.common.collect.ImmutableSet;
import com.palantir.metric.schema.Aggregation;
import com.palantir.metric.schema.Timeseries;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface QueryBuilder {

    Metric of(String metric);

    TemplateSelector templateSelector(String templateName);

    TagSelector tagSelector(String tagName, String tagValue);

    static String create(
            QueryBuilder queryBuilder,
            Timeseries timeseries,
            Map<String, String> selectedTags,
            List<String> templateVariables) {
        return queryBuilder.of(timeseries.getMetric())
                .selectFrom(ImmutableSet.<Selector>builder()
                        .addAll(selectedTags.entrySet().stream()
                                .map(tag -> queryBuilder.tagSelector(tag.getKey(), tag.getValue()))
                                .collect(Collectors.toSet()))
                        .addAll(templateVariables.stream()
                                .map(queryBuilder::templateSelector)
                                .collect(Collectors.toSet()))
                        .build())
                .aggregate(timeseries.getAggregation())
                .build();
    }

    interface Metric {
        SelectedMetric selectFromEverywhere();
        SelectedMetric selectFrom(Set<Selector> selectors);
    }

    interface SelectedMetric {
        AggregatedMetric aggregate(Aggregation aggregation);
    }

    interface AggregatedMetric {
        String build();
    }

    interface Selector {
        String selector();
    }

    interface TemplateSelector extends Selector {}

    interface TagSelector extends Selector {}

}

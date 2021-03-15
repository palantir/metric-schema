/*
 * (c) Copyright 2021 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.metric.schema.lang;

import com.google.common.collect.Maps;
import com.palantir.metric.schema.Documentation;
import com.palantir.metric.schema.MetricDefinition;
import com.palantir.metric.schema.MetricNamespace;
import com.palantir.metric.schema.MetricSchema;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class LangConverter {
    public static MetricSchema toApi(com.palantir.metric.schema.lang.MetricSchema schema) {
        return MetricSchema.builder()
                .namespaces(Maps.transformValues(schema.namespaces(), LangConverter::convert))
                .options(schema.options())
                .build();
    }

    private static MetricNamespace convert(com.palantir.metric.schema.lang.MetricNamespace namespace) {
        return MetricNamespace.builder()
                .shortName(namespace.shortName())
                .docs(Documentation.of(namespace.docs()))
                .metrics(Maps.transformValues(namespace.metrics(), LangConverter::convert))
                .build();
    }

    private static MetricDefinition convert(com.palantir.metric.schema.lang.MetricDefinition definition) {
        Map<String, Set<String>> normalizedTags =
                definition.tags().stream().collect(Collectors.toMap(TagDefinition::tagName, TagDefinition::values));
        return MetricDefinition.builder()
                .type(definition.type())
                .tags(normalizedTags.keySet())
                .values(Maps.filterEntries(
                        normalizedTags, entry -> !entry.getValue().isEmpty()))
                .docs(Documentation.of(definition.docs()))
                .build();
    }

    private LangConverter() {}
}

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

package com.palantir.metric.schema.markdown;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.palantir.metric.schema.MetricDefinition;
import com.palantir.metric.schema.MetricSchema;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/** {@link MarkdownRenderer} consumes consolidated metric schemas from a distribution to produce metrics in markdown. */
public final class MarkdownRenderer {

    /** Returns rendered markdown based on the provided schemas. */
    public static String render(Map<String, List<MetricSchema>> schemas) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("# Metrics\n");
        // TODO(forozco): use metric-schema provenance in markdown generation
        namespaces(schemas.values().stream().flatMap(List::stream)).forEach(namespace -> render(namespace, buffer));
        return CharMatcher.whitespace().trimFrom(buffer.toString());
    }

    private static void render(Namespace namespace, StringBuilder output) {
        Map<String, MetricDefinition> metrics = namespace.definition().getMetrics();
        if (metrics.isEmpty()) {
            // Don't render namespaces without metrics.
            return;
        }
        output.append("\n## ")
                .append(namespace.name())
                .append('\n')
                .append(namespace.definition().getDocs().get())
                .append('\n');
        metrics.forEach((metricName, metric) -> renderLine(namespace.name(), metricName, metric, output));
    }

    private static void renderLine(String namespace, String metricName, MetricDefinition metric, StringBuilder output) {
        output.append("- `").append(namespace).append('.').append(metricName).append('`');
        if (!metric.getTags().isEmpty()) {
            output.append(" tagged ")
                    .append(String.join(", ", Collections2.transform(metric.getTags(), value -> '`' + value + '`')));
        }
        output.append(" (")
                .append(metric.getType().toString().toLowerCase(Locale.ENGLISH))
                .append("): ")
                .append(metric.getDocs().get())
                .append('\n');
    }

    private static ImmutableList<Namespace> namespaces(Stream<MetricSchema> schemas) {
        return schemas.flatMap(schema -> schema.getNamespaces().entrySet().stream()
                        .map(entry -> Namespace.builder().name(entry.getKey()).definition(entry.getValue()).build()))
                .sorted(Comparator.comparing(Namespace::name))
                .collect(ImmutableList.toImmutableList());
    }

    private MarkdownRenderer() {}
}

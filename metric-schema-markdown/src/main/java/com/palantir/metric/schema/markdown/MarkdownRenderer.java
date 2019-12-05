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
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.palantir.metric.schema.MetricDefinition;
import com.palantir.metric.schema.MetricNamespace;
import com.palantir.metric.schema.MetricSchema;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/** {@link MarkdownRenderer} consumes consolidated metric schemas from a distribution to produce metrics in markdown. */
public final class MarkdownRenderer {

    /** Returns rendered markdown based on the provided schemas. */
    public static String render(String localCoordinate, Map<String, List<MetricSchema>> schemas) {
        StringBuilder buffer = new StringBuilder().append("# Metrics\n");
        namespaces(localCoordinate, schemas).forEach(section -> render(section, buffer));
        return CharMatcher.whitespace().trimFrom(buffer.toString());
    }

    private static void render(Section section, StringBuilder output) {
        if (section.namespaces().isEmpty()
                || section.namespaces().stream().allMatch(namespace -> namespace.definition().getMetrics().isEmpty())) {
            // Don't render sections without metrics.
            return;
        }
        output.append("\n## ")
                .append(displayName(getName(section.sourceCoordinates())))
                .append("\n\n")
                .append('`')
                .append(getGroupArtifact(section.sourceCoordinates()))
                .append("`\n");
        section.namespaces().forEach(namespace -> render(namespace, output));
    }

    private static void render(Namespace namespace, StringBuilder output) {
        Map<String, MetricDefinition> metrics = namespace.definition().getMetrics();
        if (metrics.isEmpty()) {
            // Don't render namespaces without metrics.
            return;
        }
        output.append("\n### ")
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

    private static ImmutableList<Section> namespaces(String localCoordinate, Map<String, List<MetricSchema>> schemas) {
        return schemas.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(new CoordinateComparator(localCoordinate)))
                .map(entry -> Section.builder()
                        .sourceCoordinates(entry.getKey())
                        .namespaces(entry.getValue().stream()
                                .flatMap(schema -> schema.getNamespaces().entrySet().stream())
                                // Break ties on the namespace name using the documentation, number of metrics, then
                                // MetricNamespace hashCode.
                                .sorted(Map.Entry.<String, MetricNamespace>comparingByKey()
                                        .thenComparing(Map.Entry.comparingByValue(
                                                Comparator.comparing(namespace -> namespace.getDocs().get())))
                                        .thenComparing(Map.Entry.comparingByValue(
                                                Comparator.comparing(namespace -> namespace.getMetrics().size())))
                                        .thenComparing(Map.Entry.comparingByValue(
                                                Comparator.comparing(MetricNamespace::hashCode))))
                                .map(schemaEntry -> Namespace.builder()
                                        .name(schemaEntry.getKey())
                                        .definition(schemaEntry.getValue())
                                        .build())
                                .collect(ImmutableList.toImmutableList()))
                        .build())
                .collect(ImmutableList.toImmutableList());
    }

    private static String getName(String coordinate) {
        List<String> results = Splitter.on(':').splitToList(coordinate);
        return results.size() >= 2 ? results.get(1) : coordinate;
    }

    private static String getGroupArtifact(String coordinate) {
        int lastIndex = coordinate.lastIndexOf(':');
        int firstIndex = coordinate.indexOf(':');
        return lastIndex != firstIndex ? coordinate.substring(0, lastIndex) : coordinate;
    }

    private static String displayName(String name) {
        return Splitter.on(CharMatcher.whitespace().or(CharMatcher.anyOf("-_.")))
                .omitEmptyStrings()
                .trimResults()
                .splitToList(name)
                .stream()
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));
    }

    private static String getGroup(String coordinate) {
        return Splitter.on(':').splitToList(coordinate).get(0);
    }

    /**
     * Comparator which prefers sections from the local projects group. Metrics defined in the local project should be
     * rendered first.
     */
    private static final class CoordinateComparator implements Comparator<String> {

        private final String localCoordinate;

        CoordinateComparator(String localCoordinate) {
            this.localCoordinate = localCoordinate;
        }

        @Override
        public int compare(String first, String second) {
            String firstGroup = getGroup(first);
            String secondGroup = getGroup(second);
            if (Objects.equals(firstGroup, localCoordinate) || Objects.equals(secondGroup, localCoordinate)) {
                if (Objects.equals(firstGroup, secondGroup)) {
                    return 0;
                }
                return Objects.equals(firstGroup, localCoordinate) ? -1 : 1;
            }
            return first.compareTo(second);
        }
    }

    private MarkdownRenderer() {}
}

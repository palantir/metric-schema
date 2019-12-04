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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.palantir.metric.schema.Documentation;
import com.palantir.metric.schema.MetricDefinition;
import com.palantir.metric.schema.MetricNamespace;
import com.palantir.metric.schema.MetricSchema;
import com.palantir.metric.schema.MetricType;
import org.junit.jupiter.api.Test;

class MarkdownRendererTest {

    @Test
    void testSimple() {
        MetricSchema firstSchema = MetricSchema.builder()
                .namespaces("namespace", MetricNamespace.builder()
                        .docs(Documentation.of("namespace docs"))
                        .metrics("metric", MetricDefinition.builder()
                                .type(MetricType.METER)
                                .docs(Documentation.of("metric docs"))
                                .build())
                        .build())
                .namespaces("anamespace", MetricNamespace.builder()
                        .docs(Documentation.of("namespace docs"))
                        .metrics("metric", MetricDefinition.builder()
                                .type(MetricType.METER)
                                .docs(Documentation.of("metric docs"))
                                .build())
                        .build())
                .build();
        MetricSchema secondSchema = MetricSchema.builder()
                .namespaces("secondSchema", MetricNamespace.builder()
                        .docs(Documentation.of("namespace docs"))
                        .metrics("metric", MetricDefinition.builder()
                                .type(MetricType.METER)
                                .docs(Documentation.of("metric docs"))
                                .build())
                        .build())
                .build();
        String firstMarkdown = MarkdownRenderer.render("com.palantir:test", ImmutableMap.of(
                "com.palantir:test:1.0.0", ImmutableList.of(firstSchema, secondSchema)));
        String secondMarkdown = MarkdownRenderer.render("com.palantir:test", ImmutableMap.of(
                "com.palantir:test:1.0.0",
                // reverse order should produce the same results
                ImmutableList.of(secondSchema, firstSchema)));
        assertThat(firstMarkdown)
                .isEqualTo("# Metrics\n"
                        + "\n"
                        + "## Test\n"
                        + "\n"
                        + "`com.palantir:test:1.0.0`\n"
                        + "\n"
                        + "### anamespace\n"
                        + "namespace docs\n"
                        + "- `anamespace.metric` (meter): metric docs\n"
                        + "\n"
                        + "### namespace\n"
                        + "namespace docs\n"
                        + "- `namespace.metric` (meter): metric docs\n"
                        + "\n"
                        + "### secondSchema\n"
                        + "namespace docs\n"
                        + "- `secondSchema.metric` (meter): metric docs")
                .isEqualTo(secondMarkdown);
    }

    @Test
    void testMultipleNamespacesWithSameName() {
        MetricSchema firstSchema = MetricSchema.builder()
                .namespaces("namespace", MetricNamespace.builder()
                        .docs(Documentation.of("namespace docs"))
                        .metrics("metric1", MetricDefinition.builder()
                                .type(MetricType.METER)
                                .docs(Documentation.of("metric docs 1"))
                                .build())
                        .build())
                .build();
        MetricSchema secondSchema = MetricSchema.builder()
                .namespaces("namespace", MetricNamespace.builder()
                        .docs(Documentation.of("namespace docs"))
                        .metrics("metric2", MetricDefinition.builder()
                                .type(MetricType.METER)
                                .docs(Documentation.of("metric docs 2"))
                                .build())
                        .build())
                .build();
        String firstMarkdown = MarkdownRenderer.render("com.palantir:test", ImmutableMap.of(
                "com.palantir:test:1.0.0", ImmutableList.of(firstSchema, secondSchema)));
        String secondMarkdown = MarkdownRenderer.render("com.palantir:test", ImmutableMap.of(
                "com.palantir:test:1.0.0",
                // reverse order should produce the same results
                ImmutableList.of(secondSchema, firstSchema)));
        assertThat(firstMarkdown)
                .isEqualTo("# Metrics\n"
                        + "\n"
                        + "## Test\n"
                        + "\n"
                        + "`com.palantir:test:1.0.0`\n"
                        + "\n"
                        + "### namespace\n"
                        + "namespace docs\n"
                        + "- `namespace.metric1` (meter): metric docs 1\n"
                        + "\n"
                        + "### namespace\n"
                        + "namespace docs\n"
                        + "- `namespace.metric2` (meter): metric docs 2")
                .isEqualTo(secondMarkdown);
    }

    @Test
    void testTagged() {
        String markdown = MarkdownRenderer.render(
                "com.palantir:test", ImmutableMap.of("com.palantir:test:1.0.0", ImmutableList.of(MetricSchema.builder()
                        .namespaces("namespace", MetricNamespace.builder()
                                .docs(Documentation.of("namespace docs"))
                                .metrics("metric", MetricDefinition.builder()
                                        .type(MetricType.METER)
                                        .tags("service")
                                        .tags("endpoint")
                                        .docs(Documentation.of("metric docs"))
                                        .build())
                                .build())
                        .build())));
        assertThat(markdown).isEqualTo("# Metrics\n"
                + "\n"
                + "## Test\n"
                + "\n"
                + "`com.palantir:test:1.0.0`\n"
                + "\n"
                + "### namespace\n"
                + "namespace docs\n"
                + "- `namespace.metric` tagged `service`, `endpoint` (meter): metric docs");
    }

    @Test
    void testEmptyNamespacesExcluded() {
        String markdown = MarkdownRenderer.render(
                "com.palantir:test", ImmutableMap.of("com.palantir:test", ImmutableList.of(MetricSchema.builder()
                        .namespaces("com.foo.namespace", MetricNamespace.builder()
                                .docs(Documentation.of("Foo namespace docs"))
                                .metrics("metric", MetricDefinition.builder()
                                        .type(MetricType.METER)
                                        .docs(Documentation.of("metric docs"))
                                        .build())
                                .build())
                        .namespaces("empty", MetricNamespace.builder()
                                .docs(Documentation.of("empty namespace docs"))
                                .build())
                        .build())));
        assertThat(markdown).isEqualTo("# Metrics\n"
                + "\n"
                + "## Test\n"
                + "\n"
                + "`com.palantir:test`\n"
                + "\n"
                + "### com.foo.namespace\n"
                + "Foo namespace docs\n"
                + "- `com.foo.namespace.metric` (meter): metric docs");
    }
}

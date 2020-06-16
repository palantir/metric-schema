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

package com.palantir.metric.schema;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import org.junit.jupiter.api.Test;

class ValidatorTest {

    private static final Documentation DOCS = Documentation.of("Example documentation.");

    @Test
    void testValidateNamespace_empty() {
        assertThatThrownBy(() -> Validator.validate(MetricSchema.builder()
                        .namespaces("", MetricNamespace.builder().docs(DOCS).build())
                        .build()))
                .isInstanceOf(SafeIllegalArgumentException.class)
                .hasMessageContaining("Namespace must not be empty");
    }

    @Test
    void testValidateNamespace_doublePeriod() {
        assertThatThrownBy(() -> Validator.validate(MetricSchema.builder()
                        .namespaces("a..b", MetricNamespace.builder().docs(DOCS).build())
                        .build()))
                .isInstanceOf(SafeIllegalArgumentException.class)
                .hasMessageContaining("Namespace must match pattern");
    }

    @Test
    void testValidateNamespace_whitespace() {
        assertThatThrownBy(() -> Validator.validate(MetricSchema.builder()
                        .namespaces("a b", MetricNamespace.builder().docs(DOCS).build())
                        .build()))
                .isInstanceOf(SafeIllegalArgumentException.class)
                .hasMessageContaining("Namespace must match pattern");
    }

    @Test
    void testValidateNamespace_newline() {
        assertThatThrownBy(() -> Validator.validate(MetricSchema.builder()
                        .namespaces("a\nb", MetricNamespace.builder().docs(DOCS).build())
                        .build()))
                .isInstanceOf(SafeIllegalArgumentException.class)
                .hasMessageContaining("Namespace must match pattern");
    }

    @Test
    void testEmptyMetricName() {
        assertThatThrownBy(() -> Validator.validate(MetricSchema.builder()
                        .namespaces(
                                "test",
                                MetricNamespace.builder()
                                        .docs(DOCS)
                                        .metrics(
                                                "",
                                                MetricDefinition.builder()
                                                        .docs(DOCS)
                                                        .type(MetricType.COUNTER)
                                                        .build())
                                        .build())
                        .build()))
                .isInstanceOf(SafeIllegalArgumentException.class)
                .hasMessageContaining("MetricDefinition names must not be empty");
    }

    @Test
    void testUnknownType() {
        assertThatThrownBy(() -> Validator.validate(MetricSchema.builder()
                        .namespaces(
                                "test",
                                MetricNamespace.builder()
                                        .docs(DOCS)
                                        .metrics(
                                                "name",
                                                MetricDefinition.builder()
                                                        .docs(DOCS)
                                                        .type(MetricType.valueOf("other"))
                                                        .tags("")
                                                        .build())
                                        .build())
                        .build()))
                .isInstanceOf(SafeIllegalArgumentException.class)
                .hasMessageContaining("Unknown metric type");
    }

    @Test
    void testEmptyTag() {
        assertThatThrownBy(() -> Validator.validate(MetricSchema.builder()
                        .namespaces(
                                "test",
                                MetricNamespace.builder()
                                        .docs(DOCS)
                                        .metrics(
                                                "name",
                                                MetricDefinition.builder()
                                                        .docs(DOCS)
                                                        .type(MetricType.COUNTER)
                                                        .tags("")
                                                        .build())
                                        .build())
                        .build()))
                .isInstanceOf(SafeIllegalArgumentException.class)
                .hasMessageContaining("MetricDefinition tags must not be empty");
    }

    @Test
    void testBlankTopLevelDocs() {
        assertThatThrownBy(() -> Validator.validate(MetricSchema.builder()
                        .namespaces(
                                "test",
                                MetricNamespace.builder()
                                        .docs(Documentation.of("\t \n"))
                                        .build())
                        .build()))
                .isInstanceOf(SafeIllegalArgumentException.class)
                .hasMessageContaining("Documentation must not be blank");
    }

    @Test
    void testBlankMetricDocs() {
        assertThatThrownBy(() -> Validator.validate(MetricSchema.builder()
                        .namespaces(
                                "test",
                                MetricNamespace.builder()
                                        .docs(DOCS)
                                        .metrics(
                                                "test",
                                                MetricDefinition.builder()
                                                        .type(MetricType.METER)
                                                        .docs(Documentation.of("\t \n"))
                                                        .build())
                                        .build())
                        .build()))
                .isInstanceOf(SafeIllegalArgumentException.class)
                .hasMessageContaining("Documentation must not be blank");
    }

    @Test
    void testValidNamespace_singleGroup() {
        Validator.validate(MetricSchema.builder()
                .namespaces("test", MetricNamespace.builder().docs(DOCS).build())
                .build());
    }

    @Test
    void testValidNamespace_multipleGroups() {
        Validator.validate(MetricSchema.builder()
                .namespaces("test0.test1", MetricNamespace.builder().docs(DOCS).build())
                .build());
    }

    @Test
    void testLastSegmentStartsWithUppercase() {
        Validator.validate(MetricSchema.builder()
                .namespaces("test0.Test", MetricNamespace.builder().docs(DOCS).build())
                .build());
    }

    @Test
    void testLowerCamelShortName() {
        assertThatThrownBy(() -> Validator.validate(MetricSchema.builder()
                        .namespaces(
                                "test0.test1",
                                MetricNamespace.builder()
                                        .docs(DOCS)
                                        .shortName("abcdEfg")
                                        .build())
                        .build()))
                .isInstanceOf(SafeIllegalArgumentException.class)
                .hasMessageContaining("ShortName must match pattern");
    }

    @Test
    void testMetricNameWithNumbers() {
        assertThatCode(() -> Validator.validate(MetricSchema.builder()
                        .namespaces(
                                "os",
                                MetricNamespace.builder()
                                        .docs(DOCS)
                                        .metrics(
                                                "load.1",
                                                MetricDefinition.builder()
                                                        .docs(DOCS)
                                                        .type(MetricType.GAUGE)
                                                        .build())
                                        .build())
                        .build()))
                .doesNotThrowAnyException();
    }
}

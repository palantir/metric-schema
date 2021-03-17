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

import com.palantir.logsafe.Preconditions;
import com.palantir.logsafe.SafeArg;
import com.palantir.metric.schema.Documentation;
import com.palantir.metric.schema.MetricNamespace;
import com.palantir.metric.schema.MetricSchema;
import com.palantir.metric.schema.MetricType;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

final class Validator {

    private static final String SHORT_NAME_PATTERN = "[A-Z][a-zA-Z0-9]+";
    private static final String NAME_SEGMENT_PATTERN = "[a-z0-9][a-zA-Z0-9\\-]*";
    private static final String LAST_NAME_SEGMENT_PATTERN = "[a-zA-Z0-9][a-zA-Z0-9\\-]*";
    private static final String NAME_PATTERN = "(" + NAME_SEGMENT_PATTERN + "\\.)*" + LAST_NAME_SEGMENT_PATTERN;
    private static final Pattern NAME_PREDICATE = Pattern.compile(NAME_PATTERN);
    private static final Pattern SHORT_NAME_PREDICATE = Pattern.compile(SHORT_NAME_PATTERN);

    static void validate(MetricSchema schema) {
        Preconditions.checkNotNull(schema, "MetricSchema is required");
        schema.getNamespaces().forEach(Validator::validateNamespace);
    }

    private static void validateNamespace(String namespace, com.palantir.metric.schema.MetricNamespace namespaceValue) {
        Preconditions.checkArgument(
                !namespace.isEmpty(),
                "Namespace must not be empty",
                // Provide enough data to figure out which schema is missing a namespace
                SafeArg.of("namespaceValue", namespaceValue));
        Preconditions.checkArgument(
                NAME_PREDICATE.matcher(namespace).matches(),
                "Namespace must match pattern",
                SafeArg.of("pattern", NAME_PATTERN));
        validateShortName(namespaceValue);
        validateDocumentation(namespaceValue.getDocs());
        namespaceValue.getMetrics().forEach((name, definition) -> {
            Preconditions.checkNotNull(definition, "MetricDefinition is required", SafeArg.of("namespace", namespace));
            Preconditions.checkArgument(
                    !name.isEmpty(), "MetricDefinition names must not be empty", SafeArg.of("namespace", namespace));
            Preconditions.checkArgument(
                    NAME_PREDICATE.matcher(name).matches(),
                    "MetricDefinition names must match pattern",
                    SafeArg.of("pattern", NAME_PATTERN),
                    SafeArg.of("invalidTagName", name));
            Preconditions.checkArgument(
                    MetricType.Value.UNKNOWN != definition.getType().get(),
                    "Unknown metric type",
                    SafeArg.of("namespace", namespace),
                    SafeArg.of("definition", definition));
            validateDocumentation(definition.getDocs());
            definition.getTags().forEach(tag -> {
                Preconditions.checkArgument(
                        !tag.isEmpty(),
                        "MetricDefinition tags must not be empty",
                        SafeArg.of("namespace", namespace),
                        SafeArg.of("definition", definition));
                Preconditions.checkArgument(
                        NAME_PREDICATE.matcher(tag).matches(),
                        "MetricDefinition tags must match pattern",
                        SafeArg.of("pattern", NAME_PATTERN));
            });
        });
    }

    private static void validateShortName(MetricNamespace namespace) {
        namespace
                .getShortName()
                .ifPresent(shortName -> Preconditions.checkArgument(
                        SHORT_NAME_PREDICATE.matcher(shortName).matches(),
                        "ShortName must match pattern",
                        SafeArg.of("pattern", SHORT_NAME_PATTERN),
                        SafeArg.of("invalidShortName", shortName)));
    }

    private static void validateDocumentation(Documentation documentation) {
        Preconditions.checkArgument(StringUtils.isNotBlank(documentation.get()), "Documentation must not be blank");
    }

    private Validator() {}
}

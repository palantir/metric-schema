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

import com.google.common.base.CaseFormat;
import com.google.common.base.Splitter;
import com.palantir.logsafe.Preconditions;
import org.apache.commons.lang3.StringUtils;

/** Utility functionality to escape metric values for generated java code. */
final class Custodian {

    private static final Splitter splitter =
            Splitter.onPattern("[^a-zA-Z0-9]").trimResults().omitEmptyStrings();

    /** Sanitizes a tag value into a valid java parameter or method name. */
    static String sanitizeName(String input) {
        Preconditions.checkNotNull(input, "Input string is required");
        String sanitized = StringUtils.uncapitalize(anyToUpperCamel(input));
        return escapeIfNecessary(sanitized);
    }

    static String anyToUpperCamel(String input) {
        Preconditions.checkNotNull(input, "Input string is required");
        return String.join(
                "", CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL).convertAll(splitter.split(input)));
    }

    private static String escapeIfNecessary(String input) {
        Preconditions.checkNotNull(input, "Input string is required");
        Preconditions.checkArgument(!input.isEmpty(), "Input must not be empty");
        return ReservedNames.isValid(input) ? input : escape(input);
    }

    private static String escape(String input) {
        Preconditions.checkArgument(!input.isEmpty(), "Empty values cannot be escaped");
        return input + '_';
    }

    private Custodian() {}
}

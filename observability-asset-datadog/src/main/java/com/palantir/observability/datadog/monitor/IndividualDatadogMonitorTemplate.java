/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.observability.datadog.monitor;

import org.immutables.value.Value;

@Value.Immutable
public interface IndividualDatadogMonitorTemplate extends DatadogMonitorTemplate {
    Double threshold();

    DatadogComparator comparator();

    String query();

    enum DatadogComparator {
        GREATER(false, false),
        GREATER_OR_EQUAL(false, true),
        LESS(true, false),
        LESS_OR_EQUAL(true, true);

        private final boolean lessThan;
        private final boolean equal;

        DatadogComparator(boolean lessThan, boolean equal) {
            this.lessThan = lessThan;
            this.equal = equal;
        }

        boolean isLessThan() {
            return lessThan;
        }

        boolean isEqual() {
            return equal;
        }
    }
}

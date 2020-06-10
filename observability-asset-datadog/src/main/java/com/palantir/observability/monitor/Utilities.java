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

package com.palantir.observability.monitor;

final class Utilities {
    private Utilities() {
        /* Utility class */
    }

    /**
     * Transform the monitor into an equivalent query that resolves to 1 when it should fire, and resolves to 0
     * when it should not.
     *
     * To do this we use the heaviside function to transform the difference of the query and its threshold
     * into 1 if the query is greater than the threshold or 0 if is less.
     * The heaviside function is: H(x) = (1 + (x / abs(x))) / 2
     *
     * Because we chose to return 1 if the query is greater than the threshold we must handle the comparator of the
     * monitor firing on less than, by multiplying the heaviside function by negative one in that specific case.
     *
     * Finally, in cases where the query value equals the threshold, the heaviside function will contain a
     * divide-by-0. Datadog drops these datapoints entirely. Thus in this case we must default to 1 or 0 depending
     * on whether the comparator indicates that the monitor should fire on equality or not respectively.
     *
     * For our purposes we see the following transformation:
     *   queryM comparator thresholdT
     *   ->
     *   default(
     *     ((1 + ((M-T) / abs(M-T))) / 2) * (comparator contains "<" ? -1 : 1),
     *     comparator is <= or >= ? 1 : 0)
     *
     * @param monitor The monitor to flatten
     * @return The same monitor represented as a heaviside function
     */
    static String flattenQuery(DatadogMonitorTemplate monitor) {
        String difference = difference(monitor.query(), monitor.threshold().toString());
        String heaviside = heaviside(difference);
        String normalizedToGreaterThan = monitor.comparator().isLessThan() ? additiveInverse(heaviside) : heaviside;
        String defaultOnEqualityValue = monitor.comparator().isEqual() ? "1" : "0";

        return String.format("default(%s, %s)", normalizedToGreaterThan, defaultOnEqualityValue);
    }

    static DatadogMonitorTemplate createFlatMonitor(String flatQuery) {
        return ImmutableDatadogMonitorTemplate.builder()
                .query(flatQuery)
                .comparator(DatadogMonitorTemplate.MonitorComparator.GREATER)
                .threshold(FlatQueryValue.ZERO.getValue())
                .build();
    }

    private static String difference(String left, String right) {
        return String.format("%s-%s", left, right);
    }

    private static String sign(String query) {
        return String.format("(%s)/abs(%s)", query, query);
    }

    private static String heaviside(String query) {
        return String.format("(1+(%s))/2", sign(query));
    }

    private static String additiveInverse(String query) {
        return String.format("(%s)*-1", query);
    }
}

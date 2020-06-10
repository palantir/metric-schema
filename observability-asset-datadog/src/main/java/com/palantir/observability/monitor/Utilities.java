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

import com.palantir.metric.monitor.EqualityQuery;

final class Utilities {
    private Utilities() {
        /* Utility class */
    }

    /**
     * Transform the monitor into an equivalent query that resolves to 1 when it should fire, and resolves to 0
     * when it should not.
     *
     * To do this we use the heaviside function to create a "flat" query that fires when 1 and doesn't on 0.
     * The heaviside function is a step function whose value is 0 for negative arguments and 1 for positive arguments:
     *   H(x) = (1 + (x / abs(x))) / 2
     * Therefore, for monitors whose query is meant to be greater than the threshold we use the the query minus the
     * threshold as the argument to the heaviside function, and if it is a less than comparator then the threshold
     * minus the query.
     *
     * In cases where the query value equals the threshold, the heaviside function will contain a
     * divide-by-0. Datadog drops these datapoints entirely. Thus in this case we must default to 1 or 0 depending
     * on whether the comparator indicates that the monitor should fire on equality or not respectively.
     *
     * For our purposes we see the following transformation:
     *   for queryM comparator thresholdT, where D = comparator contains "<" ? T-M : M-T
     *   ->
     *   default(
     *     ((1 + ((D) / abs(D))) / 2),
     *     comparator is <= or >= ? 1 : 0)
     *
     * @param monitor The monitor to flatten
     * @return The same monitor represented as a flat query
     */
    static String flattenQuery(DatadogMonitorTemplate monitor) {
        String difference = monitor.comparator().isLessThan()
                ? difference(monitor.threshold().toString(), monitor.query())
                : difference(monitor.query(), monitor.threshold().toString());
        String heaviside = heaviside(difference);
        String defaultOnEqualityValue = monitor.comparator().isEqual() ? "1" : "0";
        return String.format("default(%s, %s)", heaviside, defaultOnEqualityValue);
    }

    /**
     * Similar to flattenQuery, this method will take advantage of the datadog mechanic to drop datapoints on a
     * divide-by-zero to infer equality via a variant of the heaviside function. Specifically:
     * - equality can be assumed if the difference between the query and threshold equals zero
     * - dividing the difference by itself gives a positive one for non-zero values of the difference
     * - subtracting one will then map the non-zero values to zero, per flat query invariants
     * - can then default to one to fill in the dropped divide-by-zero case that happens on equality
     *
     * So we get:
     *   queryM == thresholdT
     *   ->
     *   default(
     *     ((M-T)/(M-T)) - 1,
     *     1)
     *
     * @param query The equality query to flatten
     * @return The same query represented as a flat query
     */
    static String flattenEqualityQuery(EqualityQuery query) {
        String difference = difference(query.getMetric(), Integer.toString(query.getValue()));
        String nonEqualMapToZero = String.format("((%s)/(%s))-1", difference, difference);
        return String.format("default(%s, %s)", nonEqualMapToZero, "1");
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
}

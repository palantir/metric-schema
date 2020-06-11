/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.observability.datadog.monitor;

import java.time.Duration;
import org.immutables.value.Value;

@Value.Immutable
interface Rollup {
    RollupAggregation aggregation();

    // TODO(tpetracca): this maybe needs to be stricter; want it to be a divisor of evaluation window sizes
    // default to 30 seconds because *most* products produce metrics at that frequency
    default Duration duration() {
        return Duration.ofSeconds(30);
    }

    @Value.Derived
    default String getQueryString() {
        return String.format(".rollup(%s,%s)", aggregation().getQueryString(), duration().getSeconds());
    }

    enum RollupAggregation {
        AVG("avg"),
        MIN("min"),
        MAX("max"),
        SUM("sum");

        private final String queryString;

        RollupAggregation(String queryString) {
            this.queryString = queryString;
        }

        String getQueryString() {
            return queryString;
        }
    }
}

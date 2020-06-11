/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.observability.datadog.monitor.evaluation;

enum TimeAggregator {
    AVG("avg"),
    SUM("sum"),
    MIN("min"),
    MAX("max");
    // ChangeAggregator would likely be a different class
    // unclear that we need to support them; current unused in monitor-templates; should investigate further
//  CHANGE("change"),
//  PERCENT_CHANGE("pct_change");

    private final String queryString;

    TimeAggregator(String queryString) {
        this.queryString = queryString;
    }

    String queryString() {
        return queryString;
    }
}

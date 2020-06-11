/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.observability.datadog.monitor.evaluation;

import java.time.Duration;

enum TimeWindow {
    LAST_5M("last_5m", Duration.ofMinutes(5)),
    LAST_10M("last_10m", Duration.ofMinutes(10)),
    LAST_15M("last_15m", Duration.ofMinutes(15)),
    LAST_30M("last_30m", Duration.ofMinutes(30)),
    LAST_1H("last_1h", Duration.ofHours(1)),
    LAST_2H("last_2h", Duration.ofHours(2)),
    LAST_4H("last_4h", Duration.ofHours(4)),
    LAST_1D("last_1d", Duration.ofDays(1));

    private final String queryString;
    private final long durationSeconds;

    TimeWindow(String queryString, Duration duration) {
        this.queryString = queryString;
        this.durationSeconds = duration.getSeconds();
    }

    String getQueryString() {
        return queryString;
    }

    long getDurationSeconds() {
        return durationSeconds;
    }
}

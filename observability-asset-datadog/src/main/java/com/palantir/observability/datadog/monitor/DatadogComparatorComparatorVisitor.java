/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.observability.datadog.monitor;

import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import com.palantir.metric.monitor.Comparator;
import com.palantir.observability.datadog.monitor.IndividualDatadogMonitorTemplate.DatadogComparator;

enum DatadogComparatorComparatorVisitor implements Comparator.Visitor<DatadogComparator> {
    INSTANCE;

    @Override
    public DatadogComparator visitEqualTo() {
        throw new SafeIllegalArgumentException("There is no DatadogComparator for EQUAL_TO");
    }

    @Override
    public DatadogComparator visitGreaterThan() {
        return DatadogComparator.GREATER;
    }

    @Override
    public DatadogComparator visitGreaterThanOrEqualTo() {
        return DatadogComparator.GREATER_OR_EQUAL;
    }

    @Override
    public DatadogComparator visitLessThan() {
        return DatadogComparator.LESS;
    }

    @Override
    public DatadogComparator visitLessThanOrEqualTo() {
        return DatadogComparator.LESS_OR_EQUAL;
    }

    @Override
    public DatadogComparator visitUnknown(String unknownValue) {
        throw new SafeIllegalArgumentException("Unknown Comparator value", SafeArg.of("value", unknownValue));
    }
}

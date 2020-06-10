/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.observability.monitor;

import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import com.palantir.metric.monitor.Comparator;
import com.palantir.metric.monitor.Threshold;
import com.palantir.observability.monitor.IndividualDatadogMonitorTemplate.DatadogComparator;
import org.immutables.value.Value;

/**
 * In order to support functionality that Datadog does not provide out of the box, it is helpful to be able to
 * transform an arbitrary monitor definition such that it becomes a query that can evaluate to one of two things:
 * - zero - indicating that the monitor should not fire
 * -  one - indicating that the monitor should fire
 *
 * Any flat query query can then be described as a monitor where the query string is said transformation of the
 * original query that should fire when the value returned is [strictly] greater than zero.
 *
 * This makes it easier to support workflows or features such as:
 * - conditional operators: "x AND y" can be represented as "flat(x) * flat(y) > 0"
 * - strict equality comparator: "m == y" can be represented as "flat(m == y) > 0"
 */
@Value.Immutable
public abstract class FlatQuery {
    @Value.Parameter
    abstract String query();

    @Value.Derived
    IndividualDatadogMonitorTemplate monitor() {
        return ImmutableIndividualDatadogMonitorTemplate.builder()
                .query(query())
                .comparator(DatadogComparator.GREATER)
                .threshold(FlatQueryValue.ZERO.getValue())
                .build();
    }

    // build from internal datadog-centric objects
    static FlatQuery of(String query, Double threshold, DatadogComparator comparator) {
        return ImmutableFlatQuery.of(Utilities.flattenNonEqualToQuery(query, threshold, comparator));
    }

    // build from conjure api objects
    static FlatQuery of(String query, Threshold threshold, Comparator comparator) {
        ComparatorVisitor cv = new ComparatorVisitor(query, threshold);
        return comparator.accept(cv);
    }

    private static class ComparatorVisitor implements Comparator.Visitor<FlatQuery> {
        final String query;
        final Threshold threshold;

        ComparatorVisitor(String query, Threshold threshold) {
            this.query = query;
            this.threshold = threshold;
        }

        @Override
        public FlatQuery visitEqualTo() {
            return ImmutableFlatQuery.of(Utilities.flattenEqualToQuery(query, threshold.getThreshold()));
        }

        @Override
        public FlatQuery visitGreaterThan() {
            return of(query, threshold.getThreshold(), DatadogComparator.GREATER);
        }

        @Override
        public FlatQuery visitGreaterThanOrEqualTo() {
            return of(query, threshold.getThreshold(), DatadogComparator.GREATER_OR_EQUAL);
        }

        @Override
        public FlatQuery visitLessThan() {
            return of(query, threshold.getThreshold(), DatadogComparator.LESS);
        }

        @Override
        public FlatQuery visitLessThanOrEqualTo() {
            return of(query, threshold.getThreshold(), DatadogComparator.LESS_OR_EQUAL);
        }

        @Override
        public FlatQuery visitUnknown(String unknownValue) {
            throw new SafeIllegalArgumentException("Unknown Comparator value", SafeArg.of("value", unknownValue));
        }
    }
}

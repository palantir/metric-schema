/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.observability.monitor;

import org.immutables.value.Value;

@Value.Immutable
interface CompositeDatadogMonitorTemplate extends DatadogMonitorTemplate {
    DatadogMonitorTemplate left();
    DatadogMonitorTemplate right();
    //DatadogConditionalOperator operator();
}

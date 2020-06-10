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

import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import com.palantir.metric.monitor.ConditionalOperator;

/**
 * Implementation of conditionals in Datadog where we push all the conditional logic into a single monitor by
 * transforming each of the "component monitors" into a FlatQuery. This way the conditional relationship between the
 * components can be easily represented via arithmetic.
 *
 * Perceived pros:
 * - Monitor is represented by a literal single monitor in Datadog, allowing for easier integration with (and simpler
 * logic within) monitor-management and vendor-metrics-provider
 *
 * Perceived cons:
 * - User experience navigating the Datadog UI is severely diminished. They now view a relatively complex query that
 * only results in values of 0 or 1, making it significantly harder to understand why a monitor is or is not firing
 */
final class DatadogSingleMonitorConditionalOperatorVisitor implements ConditionalOperator.Visitor<FlatQuery> {
    private final FlatQuery left;
    private final FlatQuery right;

    DatadogSingleMonitorConditionalOperatorVisitor(FlatQuery left, FlatQuery right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public FlatQuery visitAnd() {
        String query = String.format("(%s)*(%s)", left, right);
        return ImmutableFlatQuery.of(query);
    }

    @Override
    public FlatQuery visitUnknown(String unknownValue) {
        throw new SafeIllegalArgumentException("Unknown ConditionalOperator value", SafeArg.of("value", unknownValue));
    }
}

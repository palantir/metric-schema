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

final class DatadogConditionalOperatorVisitor implements ConditionalOperator.Visitor<DatadogMonitorTemplate> {
    private final DatadogMonitorTemplate left;
    private final DatadogMonitorTemplate right;

    DatadogConditionalOperatorVisitor(DatadogMonitorTemplate left, DatadogMonitorTemplate right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public DatadogMonitorTemplate visitAnd() {
        // TODO(tpetracca): in a world of nested conditionals we're going to do a lot un-necessary
        // "flattening". Really what we care about is that both left and right should fire
        // when their value is 1 and not when 0. This is already true of any DMP generated from a
        // ConditionalQuery and thus those don't need to be "flattened".
        //
        // Could wrap the "String query" of DMP in a class with a boolean that tracks if its query is
        // "flattened" or not.
        //
        // Alternatively could make it a union type of FlatQuery / NotFlatQuery. The visitor would flatten
        // one and not the other.
        String query = String.format("(%s)*(%s)", Utilities.flattenQuery(left), Utilities.flattenQuery(right));
        return Utilities.createFlatMonitor(query);
    }

    @Override
    public DatadogMonitorTemplate visitUnknown(String unknownValue) {
        throw new SafeIllegalArgumentException("Unknown ConditionalOperator value", SafeArg.of("value", unknownValue));
    }
}

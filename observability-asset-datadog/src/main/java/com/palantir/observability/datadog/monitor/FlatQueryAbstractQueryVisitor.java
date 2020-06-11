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

package com.palantir.observability.datadog.monitor;

import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import com.palantir.metric.monitor.AbstractQuery;
import com.palantir.metric.monitor.ConditionalQuery;
import com.palantir.metric.monitor.StandardQuery;

enum FlatQueryAbstractQueryVisitor implements AbstractQuery.Visitor<FlatQuery> {
    INSTANCE;

    @Override
    public FlatQuery visitQuery(StandardQuery value) {
        return value.accept(DatadogStandardQueryVisitor.INSTANCE);
    }

    @Override
    public FlatQuery visitConditional(ConditionalQuery value) {
        FlatQuery left = value.getLeft().accept(FlatQueryAbstractQueryVisitor.INSTANCE);
        FlatQuery right = value.getRight().accept(FlatQueryAbstractQueryVisitor.INSTANCE);
        DatadogSingleMonitorConditionalOperatorVisitor operatorVisitor = new DatadogSingleMonitorConditionalOperatorVisitor(left, right);
        return value.getOperator().accept(operatorVisitor);
    }

    @Override
    public FlatQuery visitUnknown(String unknownType) {
        throw new SafeIllegalArgumentException("Unknown AbstractQuery type", SafeArg.of("type", unknownType));
    }
}

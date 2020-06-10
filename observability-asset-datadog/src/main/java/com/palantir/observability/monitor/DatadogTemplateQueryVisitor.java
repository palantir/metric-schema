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
import com.palantir.metric.monitor.EqualityQuery;
import com.palantir.metric.monitor.RateQuery;
import com.palantir.metric.monitor.SimpleQuery;
import com.palantir.metric.monitor.TemplateQuery;

enum DatadogTemplateQueryVisitor implements TemplateQuery.Visitor<DatadogMonitorTemplate> {
    INSTANCE;

    @Override
    public DatadogMonitorTemplate visitRate(RateQuery _value) {
        // TODO(tpetracca): impl
        throw new SafeIllegalArgumentException("RateQuery is not yet supported");
    }

    @Override
    public DatadogMonitorTemplate visitEquality(EqualityQuery value) {
        return Utilities.createFlatMonitor(Utilities.flattenEqualityQuery(value));
    }

    @Override
    public DatadogMonitorTemplate visitSimple(SimpleQuery _value) {
        // TODO(tpetracca): impl
        throw new SafeIllegalArgumentException("SimpleQuery is not yet supported");
    }

    @Override
    public DatadogMonitorTemplate visitUnknown(String unknownType) {
        throw new SafeIllegalArgumentException("Unknown TemplateQuery value", SafeArg.of("type", unknownType));
    }
}

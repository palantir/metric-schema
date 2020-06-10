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
import com.palantir.metric.monitor.AbstractDefinition;
import com.palantir.metric.monitor.AbstractQuery;
import com.palantir.metric.monitor.NoDataDefinition;

enum DatadogAbstractDefinitionVisitor implements AbstractDefinition.Visitor<IndividualDatadogMonitorTemplate> {
    INSTANCE;

    @Override
    public IndividualDatadogMonitorTemplate visitQuery(AbstractQuery value) {
        return value.accept(FlatQueryAbstractQueryVisitor.INSTANCE).monitor();
    }

    @Override
    public IndividualDatadogMonitorTemplate visitNoData(NoDataDefinition _value) {
        // TODO(tpetracca): impl
        throw new SafeIllegalArgumentException("not sure how no data monitors work in vmp yet");
    }

    @Override
    public IndividualDatadogMonitorTemplate visitUnknown(String unknownType) {
        throw new SafeIllegalArgumentException("Unknown AbstractDefinition type", SafeArg.of("type", unknownType));
    }
}

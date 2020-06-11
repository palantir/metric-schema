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

import com.palantir.conjure.java.lib.SafeLong;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import com.palantir.metric.monitor.DifferenceOverTime;
import com.palantir.metric.monitor.SimpleQuery;
import com.palantir.metric.monitor.TemplateQuery;
import java.util.OptionalInt;

enum DatadogTemplateQueryVisitor implements TemplateQuery.Visitor<FlatQuery> {
    INSTANCE;

    @Override
    public FlatQuery visitDifferenceOverTime(DifferenceOverTime value) {
        String query = getDifferenceOverTimeQuery(value.getQuery(), value.getBuckets(), value.getTimeWindow());
        return FlatQuery.of(
                query,
                Long.valueOf(value.getDelta().longValue()).doubleValue(),
                value.getComparator().accept(DatadogComparatorComparatorVisitor.INSTANCE));
    }

    @Override
    public FlatQuery visitSimple(SimpleQuery _value) {
        // TODO(tpetracca): impl
        throw new SafeIllegalArgumentException("SimpleQuery is not yet supported");
    }

    @Override
    public FlatQuery visitUnknown(String unknownType) {
        throw new SafeIllegalArgumentException("Unknown TemplateQuery value", SafeArg.of("type", unknownType));
    }

    private static String getDifferenceOverTimeQuery(String query, OptionalInt buckets, SafeLong timeWindow) {

    }
}

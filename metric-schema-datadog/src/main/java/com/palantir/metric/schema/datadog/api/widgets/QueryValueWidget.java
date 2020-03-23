/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.metric.schema.datadog.api.widgets;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.palantir.metric.schema.datadog.api.Request;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * https://docs.datadoghq.com/graphing/widgets/timeseries/.
 *
 * <p>Not yet supported: yaxis, events, markers
 */
@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonDeserialize(as = ImmutableQueryValueWidget.class)
@JsonSerialize(as = ImmutableQueryValueWidget.class)
public interface QueryValueWidget extends BaseWidget {

    String TYPE = "query_value";

    @Override
    @Value.Default
    default String type() {
        return TYPE;
    }

    Optional<String> title();

    List<Request> requests();

    @Value.Check
    default void check() {
        if (requests().size() != 1) {
            throw new IllegalArgumentException();
        }
    }

    static Builder builder() {
        return new Builder();
    }

    class Builder extends ImmutableQueryValueWidget.Builder {}
}

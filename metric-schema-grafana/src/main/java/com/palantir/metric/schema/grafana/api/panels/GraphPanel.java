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

package com.palantir.metric.schema.grafana.api.panels;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonDeserialize(as = ImmutableGraphPanel.class)
@JsonSerialize(as = ImmutableGraphPanel.class)
public interface GraphPanel extends Panel {

    String TYPE = "graph";

    @Override
    @Value.Default
    default String type() {
        return TYPE;
    }

    String title();

    List<Target> targets();

    @Value.Immutable
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonDeserialize(as = ImmutableTarget.class)
    @JsonSerialize(as = ImmutableTarget.class)
    interface Target {

        String expr();

        @Value.Default
        default String refId() {
            return "A";
        }

        static Target of(String expr) {
            return ImmutableTarget.builder().expr(expr).build();
        }

    }

    static Builder builder() {
        return new Builder();
    }

    class Builder extends ImmutableGraphPanel.Builder {}
}

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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonDeserialize(as = ImmutableGridPosition.class)
@JsonSerialize(as = ImmutableGridPosition.class)
public interface GridPosition {

    int MAX_WIDTH = 24;

    @JsonProperty("x")
    Optional<Integer> xPos();

    @JsonProperty("y")
    Optional<Integer> yPos();

    /** Each unit represents 1/24th of the page. **/
    @Value.Default
    @JsonProperty("w")
    default int width() {
        return MAX_WIDTH / 2;
    }

    /** Each unit represents 30 pixels. **/
    @Value.Default
    @JsonProperty("h")
    default int height() {
        return 8;
    }

    static Builder builder() {
        return new Builder();
    }

    class Builder extends ImmutableGridPosition.Builder {}

}

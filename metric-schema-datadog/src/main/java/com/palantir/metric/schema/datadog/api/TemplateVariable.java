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

package com.palantir.metric.schema.datadog.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonDeserialize(as = ImmutableTemplateVariable.class)
@JsonSerialize(as = ImmutableTemplateVariable.class)
public interface TemplateVariable {

    String name();

    @JsonProperty("default")
    @Value.Default
    default String defaultValue() {
        return "*";
    }

    @Value.Default
    default String prefix() {
        return name();
    }

    static TemplateVariable of(String name) {
        return builder().name(name).build();
    }

    static Builder builder() {
        return new Builder();
    }

    class Builder extends ImmutableTemplateVariable.Builder {}

}

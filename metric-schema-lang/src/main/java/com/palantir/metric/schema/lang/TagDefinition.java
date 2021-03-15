/*
 * (c) Copyright 2021 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.metric.schema.lang;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import org.immutables.value.Value.Immutable;

@Immutable
public interface TagDefinition {
    String tagName();

    Set<String> values();

    @JsonCreator
    static TagDefinition valueOf(String tagName) {
        return ImmutableTagDefinition.builder().tagName(tagName).build();
    }

    @JsonCreator
    static TagDefinition valueOf(@JsonProperty("tagName") String tagName, @JsonProperty("values") Set<String> values) {
        return ImmutableTagDefinition.builder().tagName(tagName).values(values).build();
    }
}

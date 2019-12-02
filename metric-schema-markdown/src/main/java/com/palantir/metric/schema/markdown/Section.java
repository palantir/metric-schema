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

package com.palantir.metric.schema.markdown;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
        overshadowImplementation = true,
        jdkOnly = true,
        get = {"get*", "is*"})
@JsonDeserialize(as = ImmutableSection.class)
@JsonSerialize(as = ImmutableSection.class)
interface Section {

    String sourceCoordinates();

    List<Namespace> namespaces();

    class Builder extends ImmutableSection.Builder {}

    static Builder builder() {
        return new Builder();
    }
}

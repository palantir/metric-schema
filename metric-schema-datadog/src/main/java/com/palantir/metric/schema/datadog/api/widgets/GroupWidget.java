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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.palantir.metric.schema.datadog.api.LayoutType;
import com.palantir.metric.schema.datadog.api.Widget;
import java.util.List;
import org.immutables.value.Value;

/** https://docs.datadoghq.com/graphing/widgets/group/. */
@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonDeserialize(as = ImmutableGroupWidget.class)
@JsonSerialize(as = ImmutableGroupWidget.class)
public interface GroupWidget extends BaseWidget {

    String TYPE = "group";

    @Override
    @Value.Default
    default String type() {
        return TYPE;
    }

    String title();

    @JsonProperty("layout_type")
    @Value.Default
    default LayoutType layoutType() {
        return LayoutType.ORDERED;
    }

    List<Widget> widgets();

    static Builder builder() {
        return new Builder();
    }

    class Builder extends ImmutableGroupWidget.Builder {}
}

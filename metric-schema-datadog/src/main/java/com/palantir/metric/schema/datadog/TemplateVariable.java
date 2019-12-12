/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.metric.schema.datadog;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

// https://docs.datadoghq.com/graphing/graphing_json/#template-variable-schema
@Value.Immutable
@JsonSerialize(as = ImmutableTemplateVariable.class)
public interface TemplateVariable {
    String name();

    @JsonProperty("default")
    String defaultValue();

    String prefix();

    class Builder extends ImmutableTemplateVariable.Builder {}

    static Builder builder() {
        return new Builder();
    }

    static TemplateVariable of(String name) {
        return builder().name(name).defaultValue("*").prefix(name).build();
    }
}

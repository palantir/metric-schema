/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.metric.schema.datadog;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableRequest.class)
public interface Request {

    @JsonProperty("q")
    Query query();

    @JsonProperty("display_type")
    DisplayType displayType();

    class Builder extends ImmutableRequest.Builder {}

    static Builder builder() {
        return new Builder();
    }

    enum DisplayType {
        line
    }
}

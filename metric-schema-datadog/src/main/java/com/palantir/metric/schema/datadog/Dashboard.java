/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.metric.schema.datadog;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;

// https://docs.datadoghq.com/graphing/graphing_json/
@Value.Immutable
@JsonSerialize(as = ImmutableDashboard.class)
public interface Dashboard {
    String title();

    String description();

    @JsonProperty("layout_type")
    String layoutType();

    @JsonProperty("template_variables")
    List<TemplateVariable> templateVariables();

    List<Widget> widgets();

    class Builder extends ImmutableDashboard.Builder {}

    static Builder builder() {
        return new Builder();
    }
}

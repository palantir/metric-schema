/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.metric.schema.datadog;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

// https://docs.datadoghq.com/graphing/graphing_json/request_json/
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(WidgetDefinition.TimeSeries.class)})
public interface WidgetDefinition {

    @JsonDeserialize(as = ImmutableTimeSeries.class)
    @JsonTypeName("timeseries")
    @Value.Immutable
    interface TimeSeries extends WidgetDefinition {
        String title();

        List<Request> requests();
    }
}

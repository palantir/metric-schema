/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.metric.schema.datadog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableWidget.class)
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public interface Widget {

    WidgetDefinition definition();

    Optional<Layout> layout();

    @Value.Immutable
    @JsonSerialize(as = ImmutableLayout.class)
    @SuppressWarnings("checkstyle:MethodName")
    interface Layout {
        int x();

        int y();

        int width();

        int height();

        class Builder extends ImmutableLayout.Builder {}

        static Builder builder() {
            return new Builder();
        }
    }

    class Builder extends ImmutableWidget.Builder {}

    static Builder builder() {
        return new Builder();
    }
}

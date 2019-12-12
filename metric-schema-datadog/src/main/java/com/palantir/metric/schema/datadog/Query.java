/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.metric.schema.datadog;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableQuery.class)
public interface Query {

    String metric();

    List<String> filters();

    Aggregator aggregator();

    List<String> aggregationProperties();

    // TODO(forozco): nest functions
    List<String> functions();

    class Builder extends ImmutableQuery.Builder {}

    static Builder builder() {
        return new Builder();
    }

    @JsonValue
    @Value.Derived
    default String toQueryString() {
        String filterString =
                filters().isEmpty() ? "{*}" : filters().stream().collect(Collectors.joining(", ", "{", "}"));
        String aggregatorString = aggregationProperties().isEmpty()
                ? ""
                : aggregationProperties().stream().collect(Collectors.joining(", ", " by {", "}"));
        String innerQuery = aggregator() + ":" + metric() + filterString + aggregatorString + ".as_count()";
        return functions().stream().map(function -> function + "(").collect(Collectors.joining())
                + innerQuery
                + functions().stream().map(f -> ")").collect(Collectors.joining());
    }

    enum Aggregator {
        avg,
        min,
        max,
        sum
    }
}

/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.metric.schema.datadog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.palantir.conjure.java.serialization.ObjectMappers;
import java.io.IOException;
import org.junit.Test;

public class QueryTest {
    private static final ObjectMapper mapper = ObjectMappers.newServerObjectMapper();

    @Test
    public void raw_metric() throws IOException {
        Query query = Query.builder().metric("com.palantir.metrics").aggregator(Query.Aggregator.sum).build();
        assertThat(mapper.writeValueAsString(query)).isEqualTo("\"sum:com.palantir.metrics{*}.as_count()\"");
    }

    @Test
    public void creates_a_query() throws IOException {
        Query query = Query.builder()
                .metric("com.palantir.metrics")
                .addFilters("$deployment", "$env")
                .aggregator(Query.Aggregator.sum)
                .addAggregationProperties("host")
                .build();
        assertThat(mapper.writeValueAsString(query))
                .isEqualTo("\"sum:com.palantir.metrics{$deployment, $env} by {host}.as_count()\"");
    }

    @Test
    public void query_with_functions() throws IOException {
        Query query = Query.builder()
                .metric("com.palantir.metrics")
                .addFilters("$deployment", "$env")
                .aggregator(Query.Aggregator.sum)
                .addAggregationProperties("host")
                .addFunctions("per_second", "abs")
                .build();
        assertThat(mapper.writeValueAsString(query))
                .isEqualTo("\"per_second(abs(sum:com.palantir.metrics{$deployment, $env} by {host}.as_count()))\"");
    }
}

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

package com.palantir.metric.schema.grafana.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.palantir.metric.schema.grafana.api.panels.Panel;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Grafana dashboard schema as per https://github.com/grafana/grafana/blob/master/docs/sources/reference/dashboard.md.
 */
@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(as = ImmutableGrafanaDashboard.class)
@JsonSerialize(as = ImmutableGrafanaDashboard.class)
public interface GrafanaDashboard {

    @Value.Default
    default Integer schemaVersion() {
        return 0;
    }

    Optional<Integer> id();

    Optional<String> uid();

    String title();

    List<String> tags();

    Optional<DashboardStyle> style();

    Optional<Templating> templating();

    List<Panel> panels();

    static Builder builder() {
        return new Builder();
    }

    class Builder extends ImmutableGrafanaDashboard.Builder {}
}

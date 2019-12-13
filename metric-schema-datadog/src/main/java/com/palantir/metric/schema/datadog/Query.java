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

package com.palantir.metric.schema.datadog;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.palantir.metric.schema.Aggregation;
import com.palantir.metric.schema.datadog.api.TemplateVariable;
import java.util.Set;
import java.util.stream.Collectors;

public final class Query {

    private Query() {}

    public static Metric of(String name) {
        return new Metric(name);
    }

    public static final class Metric {
        private final String query;

        private Metric(String query) {
            this.query = query;
        }

        public SelectedMetric selectFromEverywhere() {
            return selectFrom(ImmutableSet.of());
        }

        public SelectedMetric selectFrom(Set<Selector> selectors) {
            String fromSelector = selectors.isEmpty()
                    ? "*"
                    : Joiner.on(',').join(selectors.stream().map(Selector::selector).collect(Collectors.toSet()));
            return new SelectedMetric(query + "{" + fromSelector + "}");
        }

    }

    public static final class SelectedMetric {
        private final String query;

        private SelectedMetric(String query) {
            this.query = query;
        }

        public AggregatedMetric aggregate(Aggregation aggregation) {
            StringBuilder sb = new StringBuilder();
            sb.append(aggregation.getFunction().toString().toLowerCase());
            sb.append(":");
            sb.append(query);
            if (!aggregation.getGroupBy().isEmpty()) {
                sb.append(" by {");
                sb.append(Joiner.on(',').join(aggregation.getGroupBy()));
                sb.append("}");
            }
            return new AggregatedMetric(sb.toString());
        }

    }

    public static final class AggregatedMetric {
        private final String query;

        private AggregatedMetric(String query) {
            this.query = query;
        }

        String build() {
            return query;
        }
    }

    public interface Selector {

        String selector();

    }

    public static final class TemplateSelector implements Selector {

        private final TemplateVariable templateVariable;

        private TemplateSelector(TemplateVariable templateVariable) {
            this.templateVariable = templateVariable;
        }

        @Override
        public String selector() {
            return "$" + templateVariable.name();
        }

        public static TemplateSelector of(TemplateVariable templateVariable) {
            return new TemplateSelector(templateVariable);
        }

    }

    public static final class TagSelector implements Selector {

        private final String tagName;
        private final String tagValue;

        private TagSelector(String tagName, String tagValue) {
            this.tagName = tagName;
            this.tagValue = tagValue;
        }

        @Override
        public String selector() {
            return tagName + ":" + tagValue;
        }

        public static TagSelector of(String tagName, String tagValue) {
            return new TagSelector(tagName, tagValue);
        }

    }

}

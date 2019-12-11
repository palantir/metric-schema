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

import com.palantir.metric.schema.datadog.api.Request;

public final class RequestBuilder {

    private RequestBuilder() {}

    public static Meter meter(String name) {
        return new Meter(name);
    }

    public static Timer timer(String name) {
        return new Timer(name);
    }

    public static class Meter {
        private final String name;

        private Meter(String name) {
            this.name = name;
        }
    }

    public static class Timer {
        private final String name;

        private Timer(String name) {
            this.name = name;
        }

        public Metric p95() {
            return new Metric(name + ".p95");
        }

        public Metric p99() {
            return new Metric(name + ".p95");
        }

        public Metric p999() {
            return new Metric(name + ".p95");
        }
    }

    public static class Metric {
        private final String query;

        private Metric(String query) {
            this.query = query;
        }

        Request build() {
            return Request.builder().query(query).build();
        }
    }

}

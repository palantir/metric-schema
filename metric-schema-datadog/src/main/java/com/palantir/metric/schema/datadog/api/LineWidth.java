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

package com.palantir.metric.schema.datadog.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.UnsafeArg;
import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public enum LineWidth {
    NORMAL("normal"),
    THICK("thick"),
    THIN("thin");

    private static final Map<String, LineWidth> ENUM_MAP;

    static {
        Map<String, LineWidth> map = new ConcurrentHashMap<>();
        for (LineWidth layoutType : LineWidth.values()) {
            map.put(layoutType.getName(), layoutType);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    private final String name;

    LineWidth(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    @JsonCreator
    public static LineWidth fromString(String name) {
        return Optional.ofNullable(ENUM_MAP.get(name))
                .orElseThrow(() -> new SafeIllegalArgumentException("Unknown enum value",
                        UnsafeArg.of("found", name),
                        SafeArg.of("expected", ENUM_MAP.keySet())));
    }

}

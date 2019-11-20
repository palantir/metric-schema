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

package com.palantir.metric.schema;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.palantir.conjure.java.serialization.ObjectMappers;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

final class SchemaParser {

    private static final Supplier<SchemaParser> SUPPLIER = Suppliers.memoize(SchemaParser::new);

    static SchemaParser get() {
        return SUPPLIER.get();
    }

    private final ObjectMapper yamlMapper = ObjectMappers.withDefaultModules(new ObjectMapper(new YAMLFactory()))
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    MetricSchema parseFile(Path file) {
        try (InputStream stream = Files.newInputStream(file)) {
            return yamlMapper.readValue(stream, MetricSchema.class);
        } catch (IOException e) {
            throw new SafeRuntimeException("Failed to parse file", e, SafeArg.of("file", file));
        }
    }

    ImmutableList<MetricSchema> parseDirectory(Path directory) {
        if (!Files.isDirectory(directory)) {
            return ImmutableList.of();
        }
        try (Stream<Path> steam = Files.list(directory)) {
            return steam.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".yml"))
                    .map(this::parseFile)
                    .collect(ImmutableList.toImmutableList());
        } catch (IOException e) {
            throw new SafeRuntimeException("Failed to parse files in directory", e, SafeArg.of("directory", directory));
        }
    }
}

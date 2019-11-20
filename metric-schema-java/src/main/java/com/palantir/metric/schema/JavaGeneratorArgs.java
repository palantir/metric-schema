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

import java.nio.file.Path;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
        visibility = Value.Style.ImplementationVisibility.PACKAGE,
        overshadowImplementation = true,
        jdkOnly = true,
        get = {"get*", "is*"})
public abstract class JavaGeneratorArgs {

    /**
     * Input directory for metric schema files with the
     *
     * <pre>yml</pre>
     *
     * extension.
     */
    abstract Set<Path> inputs();

    /** Output directory for generated java code. */
    abstract Path output();

    /** The default Java package name for generated classes. */
    abstract String defaultPackageName();

    public static final class Builder extends ImmutableJavaGeneratorArgs.Builder {}

    public static Builder builder() {
        return new Builder();
    }
}

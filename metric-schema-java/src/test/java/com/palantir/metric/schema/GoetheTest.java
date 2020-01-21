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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.junit.jupiter.api.Test;

public class GoetheTest {

    @Test
    public void testFormatAndEmit() {
        JavaFile javaFile = JavaFile.builder(
                        "com.palantir.foo",
                        TypeSpec.classBuilder("Foo")
                                .addStaticBlock(CodeBlock.builder()
                                        .addStatement("type oops name = bar")
                                        .build())
                                .build())
                .build();
        assertThatThrownBy(() -> Goethe.format(javaFile))
                .hasMessageContaining("Failed to format 'com.palantir.foo.Foo'")
                .hasMessageContaining("';' expected")
                .hasMessageContaining(
                        "" // newline to align the output
                                + "    type oops name = bar;\n"
                                + "            ^");
    }
}

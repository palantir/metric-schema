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

package com.palantir.metric.schema.gradle;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

public class MetricSchemaDataDogExtension {

    private final Property<String> title;
    private final Property<String> description;
    private final MapProperty<String, String> selectedTags;
    private final ListProperty<String> templateVariables;

    public MetricSchemaDataDogExtension(Project project) {
        this.title = project.getObjects().property(String.class);
        this.description = project.getObjects().property(String.class);
        this.selectedTags = project.getObjects().mapProperty(String.class, String.class);
        this.templateVariables = project.getObjects().listProperty(String.class);
    }

    public final Property<String> getTitle() {
        return title;
    }

    public final Property<String> getDescription() {
        return description;
    }

    public final MapProperty<String, String> getSelectedTags() {
        return selectedTags;
    }

    public final ListProperty<String> getTemplateVariables() {
        return templateVariables;
    }
}

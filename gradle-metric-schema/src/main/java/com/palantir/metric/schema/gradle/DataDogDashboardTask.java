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

import com.palantir.metric.schema.Dashboard;
import com.palantir.metric.schema.datadog.DataDogRenderer;
import org.gradle.api.tasks.CacheableTask;

@CacheableTask
public class DataDogDashboardTask extends AbstractDashboardTask {

    @Override
    protected final String dashboardExtension() {
        return "datadog.json";
    }

    @Override
    protected final Object renderDashboard(Dashboard dashboard) {
        return DataDogRenderer.render(dashboard);
    }
}

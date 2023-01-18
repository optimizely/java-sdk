/**
 *
 *    Copyright 2019, 2023, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.config;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicProjectConfigManager implements ProjectConfigManager {

    private final AtomicReference<ProjectConfig> projectConfigReference = new AtomicReference<>();

    @Override
    public ProjectConfig getConfig() {
        return projectConfigReference.get();
    }

    /**
     * Access to current cached project configuration.
     *
     * @return {@link ProjectConfig}
     */
    @Override
    public ProjectConfig getCachedConfig() {
        return projectConfigReference.get();
    }

    public void setConfig(ProjectConfig projectConfig) {
        projectConfigReference.set(projectConfig);
    }
}

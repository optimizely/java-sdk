/**
 *
 *    Copyright 2019, Optimizely and contributors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of a ProjectConfigManager that returns the first non-null
 * ProjectConfig from a list of delegate ProjectConfigManagers.
 *
 * The list of managers are traversed in the order they are added to the builder.
 */
public class FallbackProjectConfigManager implements ProjectConfigManager {

    private final List<ProjectConfigManager> delegates;

    private FallbackProjectConfigManager (List<ProjectConfigManager> delegates) {
        this.delegates = Collections.unmodifiableList(delegates);
    }

    @Override
    public ProjectConfig getConfig() {
        for (ProjectConfigManager delegate: delegates) {
            ProjectConfig projectConfig = delegate.getConfig();

            if (projectConfig != null) {
                return projectConfig;
            }
        }

        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<ProjectConfigManager> delegates = new ArrayList<>();

        private Builder() {

        }

        public Builder add(ProjectConfigManager projectConfigManager) {
            delegates.add(projectConfigManager);
            return this;
        }

        public Builder addAll(Iterable<ProjectConfigManager> projectConfigManagers) {
            for (ProjectConfigManager projectConfigManager: projectConfigManagers) {
                add(projectConfigManager);
            }
            return this;
        }

        public Builder clear() {
            delegates.clear();
            return this;
        }

        public FallbackProjectConfigManager build() {
            return new FallbackProjectConfigManager(delegates);
        }

    }
}

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

import javax.annotation.Nullable;

public interface ProjectConfigManager {
    /**
     * Implementations of this method should block until a datafile is available.
     *
     * @return ProjectConfig
     */
    ProjectConfig getConfig();

    /**
     * Implementations of this method should not block until a datafile is available, instead return current cached project configuration.
     * return null if ProjectConfig is not ready at the moment.
     *
     * NOTE: To use ODP segments, implementation of this function is required to return current project configuration.
     * @return ProjectConfig
     */
    @Nullable
    ProjectConfig getCachedConfig();

    /**
     * Implementations of this method should return SDK key. If there is no SDKKey then it should return null.
     *
     * NOTE: To update ODP segments configuration via polling, it is required to return sdkKey.
     * @return String
     */
    @Nullable
    String getSDKKey();
}


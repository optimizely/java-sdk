/**
 * Copyright 2025, Optimizely
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.cmab;

import javax.annotation.Nullable;

/**
 * Configuration for CMAB client operations.
 * Contains only retry configuration since HTTP client is handled separately.
 */
public class CmabClientConfig {
    private final RetryConfig retryConfig;

    public CmabClientConfig(@Nullable RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }

    @Nullable
    public RetryConfig getRetryConfig() {
        return retryConfig;
    }

    /**
     * Creates a config with default retry settings.
     */
    public static CmabClientConfig withDefaultRetry() {
        return new CmabClientConfig(RetryConfig.defaultConfig());
    }

    /**
     * Creates a config with no retry.
     */
    public static CmabClientConfig withNoRetry() {
        return new CmabClientConfig(null);
    }
}
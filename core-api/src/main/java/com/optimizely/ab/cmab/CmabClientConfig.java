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
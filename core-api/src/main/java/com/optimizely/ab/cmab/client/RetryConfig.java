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
package com.optimizely.ab.cmab.client;
/**
 * Configuration for retry behavior in CMAB client operations.
 */
public class RetryConfig {
    private final int maxRetries;
    private final long backoffBaseMs;
    private final double backoffMultiplier;
    private final int maxTimeoutMs;
    
    /**
     * Creates a RetryConfig with custom retry and backoff settings.
     *
     * @param maxRetries         Maximum number of retry attempts
     * @param backoffBaseMs      Base delay in milliseconds for the first retry
     * @param backoffMultiplier  Multiplier for exponential backoff (e.g., 2.0 for doubling)
     * @param maxTimeoutMs       Maximum total timeout in milliseconds for all retry attempts
     */
    public RetryConfig(int maxRetries, long backoffBaseMs, double backoffMultiplier, int maxTimeoutMs) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries cannot be negative");
        }
        if (backoffBaseMs < 0) {
            throw new IllegalArgumentException("backoffBaseMs cannot be negative");
        }
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
        }
        if (maxTimeoutMs < 0) {
            throw new IllegalArgumentException("maxTimeoutMs cannot be negative");
        }

        this.maxRetries = maxRetries;
        this.backoffBaseMs = backoffBaseMs;
        this.backoffMultiplier = backoffMultiplier;
        this.maxTimeoutMs = maxTimeoutMs;
    }

    /**
     * Creates a RetryConfig with default backoff settings and timeout (1 second base, 2x multiplier, 10 second timeout).
     *
     * @param maxRetries Maximum number of retry attempts
     */
    public RetryConfig(int maxRetries) {
        this(maxRetries, 1000, 2.0, 10000); // Default: 1 second base, exponential backoff, 10 second timeout
    }

    /**
     * Creates a default RetryConfig with 3 retries and exponential backoff.
     */
    public static RetryConfig defaultConfig() {
        return new RetryConfig(3);
    }

    /**
     * Creates a RetryConfig with no retries (single attempt only).
     */
    public static RetryConfig noRetry() {
        return new RetryConfig(0, 0, 1.0, 0);
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getBackoffBaseMs() {
        return backoffBaseMs;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public int getMaxTimeoutMs() {
        return maxTimeoutMs;
    }

    /**
     * Calculates the delay for a specific retry attempt.
     *
     * @param attemptNumber The attempt number (0-based, so 0 = first retry)
     * @return Delay in milliseconds
     */
    public long calculateDelay(int attemptNumber) {
        if (attemptNumber < 0) {
            return 0;
        }
        return (long) (backoffBaseMs * Math.pow(backoffMultiplier, attemptNumber));
    }

    @Override
    public String toString() {
        return String.format("RetryConfig{maxRetries=%d, backoffBaseMs=%d, backoffMultiplier=%.1f, maxTimeoutMs=%d}",
            maxRetries, backoffBaseMs, backoffMultiplier, maxTimeoutMs);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        RetryConfig that = (RetryConfig) obj;
        return maxRetries == that.maxRetries &&
            backoffBaseMs == that.backoffBaseMs &&
            maxTimeoutMs == that.maxTimeoutMs &&
            Double.compare(that.backoffMultiplier, backoffMultiplier) == 0;
    }

    @Override
    public int hashCode() {
        int result = maxRetries;
        result = 31 * result + Long.hashCode(backoffBaseMs);
        result = 31 * result + Double.hashCode(backoffMultiplier);
        result = 31 * result + Integer.hashCode(maxTimeoutMs);
        return result;
    }
}

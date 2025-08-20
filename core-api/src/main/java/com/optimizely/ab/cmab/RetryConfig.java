package com.optimizely.ab.cmab;
/**
 * Configuration for retry behavior in CMAB client operations.
 */
public class RetryConfig {
    private final int maxRetries;
    private final long backoffBaseMs;
    private final double backoffMultiplier;

    /**
     * Creates a RetryConfig with custom retry and backoff settings.
     *
     * @param maxRetries         Maximum number of retry attempts
     * @param backoffBaseMs      Base delay in milliseconds for the first retry
     * @param backoffMultiplier  Multiplier for exponential backoff (e.g., 2.0 for doubling)
     */
    public RetryConfig(int maxRetries, long backoffBaseMs, double backoffMultiplier) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries cannot be negative");
        }
        if (backoffBaseMs < 0) {
            throw new IllegalArgumentException("backoffBaseMs cannot be negative");
        }
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
        }

        this.maxRetries = maxRetries;
        this.backoffBaseMs = backoffBaseMs;
        this.backoffMultiplier = backoffMultiplier;
    }

    /**
     * Creates a RetryConfig with default backoff settings (1 second base, 2x multiplier).
     *
     * @param maxRetries Maximum number of retry attempts
     */
    public RetryConfig(int maxRetries) {
        this(maxRetries, 1000, 2.0); // Default: 1 second base, exponential backoff
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
        return new RetryConfig(0);
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
        return String.format("RetryConfig{maxRetries=%d, backoffBaseMs=%d, backoffMultiplier=%.1f}",
            maxRetries, backoffBaseMs, backoffMultiplier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        RetryConfig that = (RetryConfig) obj;
        return maxRetries == that.maxRetries &&
            backoffBaseMs == that.backoffBaseMs &&
            Double.compare(that.backoffMultiplier, backoffMultiplier) == 0;
    }

    @Override
    public int hashCode() {
        int result = maxRetries;
        result = 31 * result + Long.hashCode(backoffBaseMs);
        result = 31 * result + Double.hashCode(backoffMultiplier);
        return result;
    }
}

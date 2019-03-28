package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.concurrent.Executor;

public class BatchingConfig {
    static final int MAX_BATCH_SIZE_DEFAULT = 10;
    static final Duration MAX_BATCH_OPEN_MS_DEFAULT = Duration.ofMillis(250);

    private final Integer maxBatchSize;
    private final Duration maxBatchAge;
    private final Integer maxInFlightBatches;
    private final boolean flushOnShutdown;
    private final Executor executor;

    private BatchingConfig(Builder builder) {
        Assert.notNull(builder, "builder");
        maxBatchSize = builder.maxBatchSize;
        maxBatchAge = builder.maxBatchOpen;
        maxInFlightBatches = builder.maxInflgithBatches;
        flushOnShutdown = builder.flushOnShutdown;
        executor = builder.executor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BatchingConfig config) {
        Assert.notNull(config, "config");
        Builder builder = new Builder();
        builder.maxBatchSize = config.getMaxBatchSize();
        builder.maxBatchOpen = config.getMaxBatchAge();
        builder.maxInflgithBatches = config.getMaxInFlightBatches();
        builder.flushOnShutdown = config.isFlushOnShutdown();
        builder.executor = config.getExecutor();
        return builder;
    }

    public Integer getMaxBatchSize() {
        return maxBatchSize;
    }

    public Duration getMaxBatchAge() {
        return maxBatchAge;
    }

    public Integer getMaxInFlightBatches() {
        return maxInFlightBatches;
    }

    public boolean isFlushOnShutdown() {
        return flushOnShutdown;
    }

    public Executor getExecutor() {
        return executor;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("BatchingConfig{");
        sb.append("maxBatchSize=").append(maxBatchSize);
        sb.append(", maxBatchAge=").append(maxBatchAge);
        sb.append(", maxInFlightBatches=").append(maxInFlightBatches);
        sb.append(", flushOnShutdown=").append(flushOnShutdown);
        sb.append(", executor=").append(executor);
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {
        private Integer maxBatchSize = MAX_BATCH_SIZE_DEFAULT;
        private Duration maxBatchOpen = MAX_BATCH_OPEN_MS_DEFAULT;
        private Integer maxInflgithBatches;
        private boolean flushOnShutdown;
        private Executor executor;

        private Builder() {
        }

        public Builder maxBatchSize(@Nullable Integer maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
            return this;
        }

        public Builder maxBatchOpen(@Nullable Duration maxBatchOpen) {
            this.maxBatchOpen = maxBatchOpen;
            return this;
        }

        public Builder maxInflightBatches(@Nullable Integer maxInflightBatches) {
            this.maxInflgithBatches = maxInflightBatches;
            return this;
        }

        public Builder flushOnShutdown(boolean flushOnShutdown) {
            this.flushOnShutdown = flushOnShutdown;
            return this;
        }

        public Builder executor(@Nonnull Executor executor) {
            this.executor = Assert.notNull(executor, "executor");
            return this;
        }

        public BatchingConfig build() {
            return new BatchingConfig(this);
        }
    }
}

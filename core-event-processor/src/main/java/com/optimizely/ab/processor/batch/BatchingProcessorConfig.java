package com.optimizely.ab.processor.batch;

import com.optimizely.ab.common.internal.Assert;

import java.time.Duration;
import java.util.concurrent.Executor;

public class BatchingProcessorConfig {
    private static final int MAX_BATCH_SIZE_DEFAULT = 10;
    private static final Duration MAX_BATCH_OPEN_MS_DEFAULT = Duration.ofMillis(250);
    private static final int MAX_INFLIGHT_BATCHES_DEFAULT = 5;

    private int maxBatchSize;
    private Duration maxBatchAge;
    private int maxBatchInFlight;
    private boolean flushOnShutdown;
    private Executor executor;

    private BatchingProcessorConfig(Builder builder) {
        maxBatchSize = Math.max(0, builder.maxBatchSize);
        maxBatchAge = builder.maxBatchOpen;
        maxBatchInFlight = Math.max(1, builder.maxInflgithBatches);
        flushOnShutdown = builder.flushOnShutdown;
        executor = builder.executor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BatchingProcessorConfig copy) {
        Builder builder = new Builder();
        builder.maxBatchSize = copy.getMaxBatchSize();
        builder.maxBatchOpen = copy.getMaxBatchAge();
        builder.maxInflgithBatches = copy.getMaxInFlightBatches();
        builder.flushOnShutdown = copy.isFlushOnShutdown();
        builder.executor = copy.getExecutor();
        return builder;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public Duration getMaxBatchAge() {
        return maxBatchAge;
    }

    public int getMaxInFlightBatches() {
        return maxBatchInFlight;
    }

    public boolean isFlushOnShutdown() {
        return flushOnShutdown;
    }

    public Executor getExecutor() {
        return executor;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("QueueBufferConfig{");
        sb.append("maxBatchSize=").append(maxBatchSize);
        sb.append(", maxBatchAge=").append(maxBatchAge);
        sb.append(", maxInflgithBatches=").append(maxBatchInFlight);
        sb.append(", flushOnShutdown=").append(flushOnShutdown);
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {
        private int maxBatchSize = MAX_BATCH_SIZE_DEFAULT;
        private Duration maxBatchOpen = MAX_BATCH_OPEN_MS_DEFAULT;
        private int maxInflgithBatches = MAX_INFLIGHT_BATCHES_DEFAULT;
        private boolean flushOnShutdown;
        private Executor executor;

        private Builder() {
        }

        public Builder maxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
            return this;
        }

        public Builder maxBatchOpenMs(long maxBatchOpenMs) {
            this.maxBatchOpen = Duration.ofMillis(maxBatchOpenMs);
            return this;
        }

        public Builder maxBatchOpen(Duration maxBatchOpen) {
            this.maxBatchOpen = maxBatchOpen;
            return this;
        }

        public Builder maxInflightBatches(int maxInflightBatches) {
            this.maxInflgithBatches = maxInflightBatches;
            return this;
        }

        public Builder flushOnShutdown(boolean flushOnShutdown) {
            this.flushOnShutdown = flushOnShutdown;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = Assert.notNull(executor, "executor");
            return this;
        }

        public BatchingProcessorConfig build() {
            return new BatchingProcessorConfig(this);
        }
    }
}

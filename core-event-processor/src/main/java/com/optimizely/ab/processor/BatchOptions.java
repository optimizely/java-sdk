package com.optimizely.ab.processor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.concurrent.Executor;

public interface BatchOptions {
    /**
     * @return a positive number the buffer size that triggers flush, otherwise null if size should not be enforced
     */
    @Nullable
    Integer getMaxBatchSize();

    /**
     * @return a positive duration for buffer age that triggers flush, otherwise null if age should not be enforced.
     */
    @Nullable
    Duration getMaxBatchAge();

    /**
     * Limits the number of in-flight batches (threads) In-flight batches can accumulate when batches are produced
     * faster than they can be flushed to sink.
     */
    @Nullable
    Integer getMaxBatchInFlight();

    /**
     * @return true if buffer should flush when shutdown is requested
     */
    boolean shouldFlushOnShutdown();

    /**
     * TODO move out so this is only stateless/scalar options
     * @return executor that batching tasks will be submitted to.
     */
    @Nonnull
    Executor getExecutor();
}

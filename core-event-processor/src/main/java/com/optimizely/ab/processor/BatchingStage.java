package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.processor.batch.BatchingProcessor;
import com.optimizely.ab.processor.batch.BatchingProcessorConfig;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

public class BatchingStage<T> implements ProcessingStage<T, T> {
    private final BatchingProcessorConfig config;
    private final Executor executor;

    public BatchingStage(BatchingProcessorConfig config, Executor executor) {
        this.config = Assert.notNull(config, "config");
        this.executor = Assert.notNull(executor, "executor");
    }

    @Nonnull
    @Override
    public Processor<T> create(@Nonnull Processor<? super T> sink) {
        return new BatchingProcessor<>(config, sink);
    }
}

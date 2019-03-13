package com.optimizely.ab.processor.internal;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.processor.EventChannel;
import com.optimizely.ab.processor.EventSink;
import com.optimizely.ab.processor.EventStage;
import com.optimizely.ab.processor.disruptor.DisruptorEventChannel;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;

/**
 * Buffering stage designed for high throughput and low latency.
 *
 * It uses the <a href="https://lmax-exchange.github.io/disruptor/">LMAX Disruptor</a> library
 * for inter-thread communication without locks.
 */
class EventBufferStage<T> implements EventStage<T, T> {
    static final int DEFAULT_BUFFER_CAPACITY = 1024;
    static final int DEFAULT_MAX_BATCH_SIZE = 50;

    // TODO extract an interface
    private LogEventProcessor<?> config;

    EventBufferStage(final LogEventProcessor<?> config) {
        this.config = Assert.notNull(config, "config");
    }

    @Nonnull
    @Override
    public EventChannel<T> createSource(@Nonnull EventSink<T> sink) {
        ThreadFactory threadFactory = config.getThreadFactory();
        if (threadFactory == null) {
            threadFactory = DaemonThreadFactory.INSTANCE;
        }

        WaitStrategy waitStrategy = config.getWaitStrategy();
        if (waitStrategy == null) {
            waitStrategy = new BlockingWaitStrategy();
        }

        Integer batchMaxSize = config.getBatchMaxSize();
        if (batchMaxSize == null) {
            batchMaxSize = DEFAULT_MAX_BATCH_SIZE;
        }

        Integer bufferCapacity = config.getBufferSize();
        if (bufferCapacity == null) {
            bufferCapacity = DEFAULT_BUFFER_CAPACITY;
        }

        return new DisruptorEventChannel<>(
            sink,
            batchMaxSize,
            bufferCapacity,
            threadFactory,
            waitStrategy,
            DisruptorEventChannel.LoggingExceptionHandler.getInstance()
        );
    }
}

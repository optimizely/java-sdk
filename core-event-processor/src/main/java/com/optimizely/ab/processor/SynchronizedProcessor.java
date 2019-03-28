package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Creates a synchronized (thread-safe) processor that allows only one thread to invoke {@link Processor} methods at a
 * time.
 *
 * @param <T> the type of input elements
 */
class SynchronizedProcessor<T> implements Processor<T> {
    private final Processor<T> processor;
    protected final Object processMutex;

    /**
     * @param processor the processor to wrap
     */
    public SynchronizedProcessor(Processor<T> processor) {
        this(processor, new Object());
    }

    /**
     * @param processor the processor to wrap
     * @param processMutex the processMutex object to synchronize on
     */
    protected SynchronizedProcessor(Processor<T> processor, Object processMutex) {
        this.processor = Assert.notNull(processor, "processor");
        this.processMutex = Assert.notNull(processMutex, "processMutex");
    }

    @Override
    public void process(@Nonnull T element) {
        synchronized (processMutex) {
            processor.process(element);
        }
    }

    @Override
    public void processBatch(@Nonnull Collection<? extends T> elements) {
        synchronized (processMutex) {
            processor.processBatch(elements);
        }
    }
}

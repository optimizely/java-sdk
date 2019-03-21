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
    private final Object mutex;

    /**
     * @param processor the processor to wrap
     */
    SynchronizedProcessor(Processor<T> processor) {
        this.processor = Assert.notNull(processor, "processor");
        this.mutex = this;
    }

    /**
     * @param processor the processor to wrap
     * @param mutex the mutex object to synchronize on
     */
    SynchronizedProcessor(Processor<T> processor, Object mutex) {
        this.processor = Assert.notNull(processor, "processor");
        this.mutex = Assert.notNull(mutex, "mutex");
    }

    @Override
    public void process(@Nonnull T element) {
        synchronized (mutex) {
            processor.process(element);
        }
    }

    @Override
    public void processBatch(@Nonnull Collection<? extends T> elements) {
        synchronized (mutex) {
            processor.processBatch(elements);
        }
    }
}

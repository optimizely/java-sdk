/**
 *    Copyright 2019, Optimizely Inc. and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
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

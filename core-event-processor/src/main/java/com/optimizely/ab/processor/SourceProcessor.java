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
import com.optimizely.ab.common.lifecycle.LifecycleAware;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Abstract implementation of basic behavior for non-terminal {@link Processor}
 * for intermediate operations of a processing flow.
 *
 * Maintains reference to a downstream {@link Processor}.
 *
 * @param <T> the type of input elements
 * @param <R> the type of output elements; the type of elements accepted by downstream.
 */
public abstract class SourceProcessor<T, R> extends BaseProcessor<T> implements LifecycleAware {

    private final Processor<? super R> sink;

    public SourceProcessor(Processor<? super R> sink) {
        this.sink = Assert.notNull(sink, "sink");
    }

    /**
     * Propagates the lifecycle signal to downstream
     */
    @Override
    public final void onStart() {
        LifecycleAware.start(getSink());
        afterStart();
    }

    /**
     * Overridable method to carry out initialization tasks when started.
     */
    protected void afterStart() {
        // no-op by default
    }

    /**
     * Propagates the lifecycle signal to downstream
     */
    @Override
    public final boolean onStop(long timeout, TimeUnit unit) {
        boolean thisResult = beforeStop();
        return LifecycleAware.stop(getSink(), timeout, unit) && thisResult;
    }

    /**
     * Overridable method to carry out shutdown tasks when stopped.
     *
     * @see LifecycleAware#onStop(long, TimeUnit)
     */
    protected boolean beforeStop() {
        return true;
    }


    /**
     * Sends a value downstream, if it is not {@code null}.
     *
     * @param element value to emit if non-null
     */
    protected void emitElementIfPresent(R element) {
        if (element == null) {
            logger.debug("Prevented null element from being emitted");
            return;
        }

        emitElement(element);
    }

    /**
     * Sends a non-null value downstream
     *
     * @param element value to emit
     */
    protected void emitElement(@Nonnull R element) {
        sink.process(element);
    }

    /**
     * Sends a batch of values downstream.
     *
     * @param elements values to emit
     */
    protected void emitBatch(@Nonnull Collection<? extends R> elements) {
        sink.processBatch(elements);
    }

    protected Processor<? super R> getSink() {
        return sink;
    }
}

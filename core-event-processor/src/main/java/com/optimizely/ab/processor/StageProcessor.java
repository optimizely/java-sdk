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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class of basic behavior for processing (non-terminal) stage in a pipeline.
 *
 * @param <T> the type of input elements
 * @param <R> the type of output elements; the type of elements accepted by downstream.
 */
public abstract class StageProcessor<T, R> implements Stage<T, R>, LifecycleAware {
    // Share logger with subclasses
    protected static final Logger logger = LoggerFactory.getLogger(StageProcessor.class);

    private Processor<? super R> sink;

    @Override
    public void configure(Processor<? super R> sink) {
        if (this.sink != null) {
            if (sink == null) {
                logger.info("Clearing configured sink (before: {})", this.sink);
            } else if (this.sink != sink) {
                logger.info("Replacing configured sink (before: {}, after: {})", this.sink, sink);
            }
        }
        this.sink = sink;
    }

    @Override
    public void onStart() {
        // no-op by default
    }

    /**
     * Propagates the lifecycle signal to downstream
     */
    @Override
    public boolean onStop(long timeout, TimeUnit unit) {
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
        getSink().process(element);
    }

    /**
     * Sends a batch of values downstream.
     *
     * @param elements values to emit
     */
    protected void emitBatch(@Nonnull Collection<? extends R> elements) {
        getSink().processBatch(elements);
    }

    protected Processor<? super R> getSink() {
        Assert.state(sink != null, "Sink has not been set");
        return sink;
    }
}

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

import com.optimizely.ab.common.LifecycleAware;
import com.optimizely.ab.common.internal.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class AbstractEventChannel<T, R> implements EventChannel<T>, Consumer<T> {
    /**
     * Share with subclasses
     */
    protected static final Logger logger = LoggerFactory.getLogger(AbstractEventChannel.class);

    private final EventSink<R> sink;

    public AbstractEventChannel(EventSink<R> sink) {
        this.sink = Assert.notNull(sink, "sink");
    }

    @Override
    public void onStart() {
        LifecycleAware.start(getSink());
    }

    @Override
    public boolean onStop(long timeout, TimeUnit unit) {
        return LifecycleAware.stop(getSink(), timeout, unit);
    }

    @Override
    public void accept(T event) {
        put(event);
    }

    @Override
    public void putBatch(Collection<? extends T> events) {
        for (final T item : events) {
            put(item);
        }
    }

    @Override
    public boolean isBlocking() {
        return false;
    }

    @Override
    public boolean isFull() {
        return false;
    }

    /**
     * @param item object to emit if non-null
     */
    protected void emitItemIfPresent(R item) {
        if (item == null) {
            logger.debug("Prevented null item from being emitted");
            return;
        }

        emitItem(item);
    }

    protected void emitItem(@Nonnull R item) {
        sink.put(item);
    }

    protected void emitBatch(@Nonnull Collection<? extends R> items) {
        sink.putBatch(items);
    }

    protected EventSink<R> getSink() {
        return sink;
    }
}

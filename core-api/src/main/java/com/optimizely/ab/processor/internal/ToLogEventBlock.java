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
package com.optimizely.ab.processor.internal;

import com.optimizely.ab.common.callback.Callback;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.processor.ProcessorBlock;
import com.optimizely.ab.processor.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An operation that converts {@link EventBatch} into an {@link LogEvent}.
 *
 * Supports receiving batches of elements and packing into a single {@link LogEvent}.
 * Uses {@link EventContextKey} to separate a batch into sub-groups in the case that
 * the whole batch does not map cleanly into a single {@link LogEvent}. For example,
 * if batch contains events from different revisions.
 *
 * @see EventContextKey
 */
class ToLogEventBlock extends Blocks.Source<LogEvent> implements ProcessorBlock<EventBatch, LogEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ToLogEventBlock.class);

    private final Function<EventBatch, LogEvent> logEventFactory;
    private final Supplier<Callback<EventBatch>> callbackProvider;

    /**
     * @param logEventFactory converts a {@link EventBatch} into {@link LogEvent}
     * @param callbackProvider optional completion callback provider for individual
     */
    ToLogEventBlock(
        Function<EventBatch, LogEvent> logEventFactory,
        Supplier<Callback<EventBatch>> callbackProvider
    ) {
        this.logEventFactory = Assert.notNull(logEventFactory, "logEventFactory");
        this.callbackProvider = callbackProvider;
    }

    // simple 1:1 conversion
    @Override
    public void post(@Nonnull EventBatch element) {
        LogEvent logEvent = logEventFactory.apply(element);
        logEvent.setCallback(getUpstreamCallback());

        emit(logEvent);
    }

    /**
     * Converts a batch of elements into as few {@link LogEvent} as possible while preserving
     * the fields captured by {@link EventContextKey}. In most cases, the product should be
     * a single {@link LogEvent}.
     */
    @Override
    public void postBatch(@Nonnull Collection<? extends EventBatch> items) {
        Map<EventContextKey, Collection<EventBatch>> grouping = new HashMap<>();
        for (EventBatch msg : items) {
            EventContextKey key = EventContextKey.create(msg);

            grouping.computeIfAbsent(key, k -> new ArrayList<>()).add(msg);
        }

        logger.debug("Split {} batched elements into {} groups", items.size(), grouping.size());

        for (Map.Entry<EventContextKey, Collection<EventBatch>> entry : grouping.entrySet()) {
            EventBatch.Builder eventBatch = entry.getKey().toEventBatchBuilder();

            final Collection<EventBatch> group = entry.getValue();
            for (EventBatch single : group) {
                eventBatch.addVisitors(single.getVisitors());
            }

            LogEvent logEvent = logEventFactory.apply(eventBatch.build());
            logEvent.setCallback(getBatchCallback(group));

            emit(logEvent);
        }
    }

    private Callback<EventBatch> getUpstreamCallback() {
        if (callbackProvider != null) {
            return callbackProvider.get();
        }
        return null;
    }

    /**
     * Creates a callback to attach to the {@link LogEvent} that gets emitted for a batch of elements.
     *
     * This fans-out the success/failure callback so that the {@link Callback} that's passed receives the original batched
     * elements, individually.
     *
     * The reason we want to callback per-item because it allows callbacks to be unaware of batching and enable an element
     * to tracked from end-to-end.
     *
     * @param originals the elements to pass {@code callback} when the returned callback is completed.
     * @return a callback that notifies
     */
    private Callback<EventBatch> getBatchCallback(Collection<EventBatch> originals) {
        final Callback<EventBatch> upstreamCallback = getUpstreamCallback();
        if (upstreamCallback == null || originals.isEmpty()) {
            return null;
        }

        // optimization: avoid unnecessary wrapper
        if (originals.size() == 1) {
            return upstreamCallback;
        }

        return new BatchCallback<>(upstreamCallback, originals);
    }

    private static class BatchCallback<T> implements Callback<T> {
        private final Callback<T> elementCallback;
        private final Collection<T> batchElements;

        public BatchCallback(
            Callback<T> elementCallback,
            Collection<T> batchElements
        ) {
            this.batchElements = batchElements;
            this.elementCallback = elementCallback;
        }

        @Override
        public void success(T batchedElement) {
            for (final T original : batchElements) {
                elementCallback.success(original);
            }
        }

        @Override
        public void failure(T batchedElement, @Nonnull Throwable ex) {
            for (final T source : batchElements) {
                elementCallback.failure(source, ex);
            }
        }
    }
}

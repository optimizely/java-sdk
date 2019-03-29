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

import com.optimizely.ab.api.EventContext;
import com.optimizely.ab.common.callback.Callback;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.processor.Processor;
import com.optimizely.ab.processor.StageProcessor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Converts one or more {@link EventBatch} into {@link LogEvent}.
 *
 * Inputs are grouped prior to merging when multiple events are sent using {@link Processor#processBatch(Collection)}
 * to avoid {@link EventContext} collisions.
 *
 * @see EventContextKey
 */
class EventBatchMergeStage extends StageProcessor<EventBatch, LogEvent> {
    private final Function<EventBatch, LogEvent> eventFactory;
    private final Supplier<Callback<EventBatch>> callbackSupplier;

    EventBatchMergeStage(
        Function<EventBatch, LogEvent> eventFactory,
        Supplier<Callback<EventBatch>> callbackSupplier
    ) {
        this.eventFactory = Assert.notNull(eventFactory, "eventFactory");
        this.callbackSupplier = Assert.notNull(callbackSupplier, "callbackSupplier");
    }

    @Override
    public void process(EventBatch element) {
        LogEvent logEvent = eventFactory.apply(element);
        logEvent.setCallback(callbackSupplier.get());
        getSink().process(logEvent);
    }

    @Override
    public void processBatch(Collection<? extends EventBatch> items) {
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

            LogEvent logEvent = eventFactory.apply(eventBatch.build());

            // callback is expected to be called with the original elements of combined batch.
            logEvent.setCallback(createFanoutCallback(callbackSupplier.get(), group));

            getSink().process(logEvent);
        }
    }

    private Callback<EventBatch> createFanoutCallback(
        Callback<EventBatch> delegate,
        Collection<EventBatch> group
    ) {
        return new Callback<EventBatch>() {
            @Override
            public void success(EventBatch bundled) {
                for (final EventBatch source : group) {
                    delegate.success(source);
                }
            }

            @Override
            public void failure(EventBatch context, @Nonnull Throwable ex) {
                for (final EventBatch source : group) {
                    delegate.failure(source, ex);
                }
            }
        };
    }

}

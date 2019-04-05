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
import com.optimizely.ab.processor.ActorBlock;
import com.optimizely.ab.processor.SourceBlock;
import com.optimizely.ab.processor.TargetBlock;
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
 * A group {@link LogEvent} from {@link EventBatch} objects/collections.
 *
 * Inputs are grouped prior to merging when multiple events are sent using {@link TargetBlock#postBatch(Collection)}
 * to avoid {@link EventContext} collisions.
 *
 * @see EventContextKey
 */
class ToLogEventBlock extends SourceBlock.Base<LogEvent> implements ActorBlock<EventBatch, LogEvent> {
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

        if (callbackProvider != null) {
            logEvent.setCallback(callbackProvider.get());
        }

        target().post(logEvent);
    }

    /**
     *
     * @param items
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

            if (callbackProvider != null) {
                // callback is expected to be called with the original elements of combined batch.
                logEvent.setCallback(createFanoutCallback(callbackProvider.get(), group));
            }

            target().post(logEvent);
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

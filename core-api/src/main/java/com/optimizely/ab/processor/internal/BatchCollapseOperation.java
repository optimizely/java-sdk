package com.optimizely.ab.processor.internal;

import com.optimizely.ab.api.EventContext;
import com.optimizely.ab.common.Callback;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.processor.BaseEventOperator;
import com.optimizely.ab.processor.EventOperator;
import com.optimizely.ab.processor.EventSink;
import com.optimizely.ab.processor.EventOperation;

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
 * Inputs are grouped prior to merging when multiple events are sent using {@link EventSink#sendBatch(Collection)}
 * to avoid {@link EventContext} collisions.
 *
 * @see EventContextKey
 */
class BatchCollapseOperation implements EventOperation<EventBatch, LogEvent> {
    private final Function<EventBatch, LogEvent> eventFactory;
    private final Supplier<Callback<EventBatch>> callbackSupplier;

    BatchCollapseOperation(
        Function<EventBatch, LogEvent> eventFactory,
        Supplier<Callback<EventBatch>> callbackSupplier
    ) {
        this.eventFactory = Assert.notNull(eventFactory, "eventFactory");
        this.callbackSupplier = Assert.notNull(callbackSupplier, "callbackSupplier");
    }

    @Nonnull
    @Override
    public EventOperator<EventBatch> create(@Nonnull EventSink<LogEvent> sink) {
        return new BatchLogEventOperator(sink);
    }

    /**
     * Builds and emits {@link LogEvent} from one or more {@link EventBatch} inputs.
     */
    class BatchLogEventOperator extends BaseEventOperator<EventBatch, LogEvent> {
        public BatchLogEventOperator(EventSink<LogEvent> sink) {
            super(sink);
        }

        @Override
        public void send(EventBatch element) {
            getSink().send(eventFactory.apply(element));
        }

        @Override
        public void sendBatch(Collection<? extends EventBatch> events) {
            Map<EventContextKey, Collection<EventBatch>> grouping = new HashMap<>();
            for (EventBatch msg : events) {
                EventContextKey key = EventContextKey.create(msg);

                grouping.computeIfAbsent(key, k -> new ArrayList<>()).add(msg);
            }

            logger.debug("Split {} batched elements into {} groups", events.size(), grouping.size());

            for (Map.Entry<EventContextKey, Collection<EventBatch>> entry : grouping.entrySet()) {
                EventBatch.Builder eventBatch = entry.getKey().toEventBatchBuilder();

                final Collection<EventBatch> group = entry.getValue();
                for (EventBatch single : group) {
                    eventBatch.addVisitors(single.getVisitors());
                }

                LogEvent logEvent = eventFactory.apply(eventBatch.build());

                // callback is expected to be called with the original elements of combined batch.
                logEvent.setCallback(createFanoutCallback(callbackSupplier.get(), group));

                getSink().send(logEvent);
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
}

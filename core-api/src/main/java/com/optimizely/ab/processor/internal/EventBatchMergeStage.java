package com.optimizely.ab.processor.internal;

import com.optimizely.ab.api.EventContext;
import com.optimizely.ab.common.callback.Callback;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.processor.AbstractProcessor;
import com.optimizely.ab.processor.Processor;
import com.optimizely.ab.processor.ProcessingStage;

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
class EventBatchMergeStage implements ProcessingStage<EventBatch, LogEvent> {
    private final Function<EventBatch, LogEvent> eventFactory;
    private final Supplier<Callback<EventBatch>> callbackSupplier;

    EventBatchMergeStage(
        Function<EventBatch, LogEvent> eventFactory,
        Supplier<Callback<EventBatch>> callbackSupplier
    ) {
        this.eventFactory = Assert.notNull(eventFactory, "eventFactory");
        this.callbackSupplier = Assert.notNull(callbackSupplier, "callbackSupplier");
    }

    @Nonnull
    @Override
    public Processor<EventBatch> create(@Nonnull Processor<LogEvent> sink) {
        return new BatchLogEventOperator(sink);
    }

    /**
     * Builds and emits {@link LogEvent} from one or more {@link EventBatch} inputs.
     */
    class BatchLogEventOperator extends AbstractProcessor<EventBatch, LogEvent> {
        public BatchLogEventOperator(Processor<LogEvent> sink) {
            super(sink);
        }

        @Override
        public void process(EventBatch element) {
            getSink().process(eventFactory.apply(element));
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
}

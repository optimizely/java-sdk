package com.optimizely.ab.processor.internal;

import com.optimizely.ab.common.Callback;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.processor.AbstractEventChannel;
import com.optimizely.ab.processor.EventChannel;
import com.optimizely.ab.processor.EventSink;
import com.optimizely.ab.processor.EventStage;
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
 * Collapses batched inputs into a single output
 */
class BatchCollapseStage implements EventStage<EventBatch, LogEvent> {
    private static final Logger logger = LoggerFactory.getLogger(BatchLogEventChannel.class);

    private final Function<EventBatch, LogEvent> eventFactory;
    private final Supplier<Callback<EventBatch>> callbackSupplier;

    BatchCollapseStage(
        Function<EventBatch, LogEvent> eventFactory,
        Supplier<Callback<EventBatch>> callbackSupplier
    ) {
        this.eventFactory = Assert.notNull(eventFactory, "eventFactory");
        this.callbackSupplier = Assert.notNull(callbackSupplier, "callbackSupplier");
    }

    @Nonnull
    @Override
    public EventChannel<EventBatch> createSource(@Nonnull EventSink<LogEvent> sink) {
        return new BatchLogEventChannel(sink);
    }

    /**
     * Builds and emits {@link LogEvent} from one or more {@link EventBatch} inputs.
     */
    class BatchLogEventChannel extends AbstractEventChannel<EventBatch, LogEvent> {
        public BatchLogEventChannel(EventSink<LogEvent> sink) {
            super(sink);
        }

        @Override
        public void put(EventBatch item) {
            getSink().put(eventFactory.apply(item));
        }

        @Override
        public void putBatch(Collection<? extends EventBatch> events) {
            Map<EventContextKey, Collection<EventBatch>> grouping = new HashMap<>();
            for (EventBatch msg : events) {
                EventContextKey key = EventContextKey.create(msg);

                grouping.computeIfAbsent(key, k -> new ArrayList<>()).add(msg);
            }

            logger.debug("Split {} batched items into {} groups", events.size(), grouping.size());

            for (Map.Entry<EventContextKey, Collection<EventBatch>> entry : grouping.entrySet()) {
                EventBatch.Builder eventBatch = entry.getKey().toEventBatchBuilder();

                final Collection<EventBatch> group = entry.getValue();
                for (EventBatch single : group) {
                    eventBatch.addVisitors(single.getVisitors());
                }

                LogEvent logEvent = eventFactory.apply(eventBatch.build());

                // callback is expected to be called with the original items of combined batch.
                logEvent.setCallback(createFanoutCallback(callbackSupplier.get(), group));

                getSink().put(logEvent);
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

        @Override
        public boolean isBlocking() {
            return false;
        }

        @Override
        public boolean isFull() {
            return false;
        }
    }
}

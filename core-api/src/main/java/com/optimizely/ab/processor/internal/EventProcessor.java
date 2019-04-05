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

import com.optimizely.ab.common.callback.AggregateCallback;
import com.optimizely.ab.common.callback.Callback;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.common.plugin.Plugin;
import com.optimizely.ab.common.plugin.PluginSupport;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.EventFactory;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.processor.ActorBlock;
import com.optimizely.ab.processor.BatchOptions;
import com.optimizely.ab.processor.Block;
import com.optimizely.ab.processor.Blocks;
import com.optimizely.ab.processor.Processor;
import com.optimizely.ab.processor.TargetBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Configures and builds the event processing pipeline for dispatching Optimizely
 * events to an {@link EventHandler}.
 *
 * @param <T> the type of elements fed into the processor
 */
public class EventProcessor<T> implements PluginSupport {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessor.class);

    private EventHandler eventHandler;

    private CompositeConsumer<T> transformers = new CompositeConsumer<>();

    private UnaryOperatorChain<EventBatch> interceptors = new UnaryOperatorChain<>();

    private Function<? super T, ? extends EventBatch> eventBatchConverter;

    /**
     * Block that handles event batching.
     * Receives elements from (application) threads producing into this processor.
     * By default, no buffer is used in order to be consistent with past releases. This may change in the future.
     */
    private ActorBlock<EventBatch, EventBatch> batchBlock = Blocks.identity();

    /**
     * Callbacks to be invoked when an event is finished being dispatched to report success or failure status.
     */
    private AggregateCallback<EventBatch> callbacks = new AggregateCallback<>();

    private ThreadFactory threadFactory;

    private Function<EventBatch, LogEvent> eventFactory = (new EventFactory())::createLogEvent; // TODO do not default

    private BiConsumer<LogEvent, Throwable> logEventExceptionHandler = (logEvent, err) -> {
        logger.error("Error dispatching event: {}", logEvent, err);
    };

    public Processor<T> build() {
        ActorBlock<T, T> transformProcessor = getTransformProcessor();
        ActorBlock<T, EventBatch> eventBatchConverter = getEventBatchConverter();
        ActorBlock<EventBatch, EventBatch> interceptProcessor = getInterceptProcessor();
        ActorBlock<EventBatch, EventBatch> batchProcessor = getBatchProcessor();
        ActorBlock<EventBatch, LogEvent> logEventConverter = getLogEventConverter();
        TargetBlock<LogEvent> eventHandler = getEventHandler();

        transformProcessor.linkTo(eventBatchConverter);
        eventBatchConverter.linkTo(interceptProcessor);
        interceptProcessor.linkTo(batchProcessor);
        batchProcessor.linkTo(logEventConverter);
        logEventConverter.linkTo(eventHandler);

        List<Block> blocks = Arrays.asList(
            transformProcessor,
            eventBatchConverter,
            interceptProcessor,
            batchProcessor,
            logEventConverter
        );

        return new Processor<>(transformProcessor, blocks);
    }

    public EventProcessor<T> plugin(Plugin<EventProcessor<T>> plugin) {
        Assert.notNull(plugin, "plugin");
        plugin.configure(this);
        return this;
    }

    public EventProcessor<T> transformer(final Consumer<T> action) {
        Assert.notNull(action, "transformer");
        this.transformers.add((element -> {
            try {
                action.accept(element);
            } catch (RuntimeException e) {
                logger.warn("Suppressing exception thrown from event transform action: {}", action, e);
            }
        }));
        return this;
    }

    public EventProcessor<T> interceptor(final Predicate<EventBatch> filter) {
        Assert.notNull(filter, "filter");
        this.interceptors.add(element -> {
            try {
                if (!filter.test(element)) {
                    return null; // drops the event
                }
            } catch (RuntimeException e) {
                logger.warn("Suppressing exception thrown from event interceptor filter: {}", filter, e);
            }
            return element;
        });
        return this;
    }

    /**
     * Registers a callback to be notified of event dispatch completion
     */
    public EventProcessor<T> callback(Callback<EventBatch> callback) {
        this.callbacks.add(Assert.notNull(callback, "callback"));
        return this;
    }

    /**
     * Registers a callback to be notified of successful event dispatch completion
     */
    public EventProcessor<T> callback(Consumer<EventBatch> success) {
        return callback(Callback.from(success, (c, ex) -> {}));
    }

    public EventProcessor<T> eventFactory(EventFactory eventFactory) {
        return eventFactory(Assert.notNull(eventFactory, "eventFactory")::createLogEvent);
    }

    EventProcessor<T> eventFactory(Function<EventBatch, LogEvent> eventFactory) {
        this.eventFactory = Assert.notNull(eventFactory, "eventFactory");
        return this;
    }

    public EventProcessor<T> eventHandler(EventHandler eventHandler) {
        this.eventHandler = Assert.notNull(eventHandler, "eventHandler");
        return this;
    }

    public EventProcessor<T> batchConfig(BatchOptions config) {
        this.batchBlock = Blocks.batch(Assert.notNull(config, "config"));
        return this;
    }

    EventProcessor<T> batchBlock(ActorBlock<EventBatch, EventBatch> block) {
        this.batchBlock = Assert.notNull(block, "block");
        return this;
    }

    EventProcessor<T> converter(Function<? super T, ? extends EventBatch> converter) {
        this.eventBatchConverter = Assert.notNull(converter, "eventBatchConverter");
        return this;
    }

    ActorBlock<T, T> getTransformProcessor() {
        if (transformers == null || transformers.isEmpty()) {
            logger.trace("No transformers are configured");
            return Blocks.identity();
        }
        return Blocks.effect(transformers);
    }

    ActorBlock<T, EventBatch> getEventBatchConverter() {
        return Blocks.map(eventBatchConverter);
    }

    ActorBlock<EventBatch, EventBatch> getInterceptProcessor() {
        if (interceptors == null || interceptors.isEmpty()) {
            logger.trace("No interceptors are configured");
            return Blocks.identity();
        }

        return Blocks.map(interceptors);
    }

    ActorBlock<EventBatch, EventBatch> getBatchProcessor() {
        return batchBlock;
    }

    ActorBlock<EventBatch, LogEvent> getLogEventConverter() {
        Assert.state(eventFactory != null, "EventFactory has not been configured");
        return new ToLogEventBlock(eventFactory, () -> callbacks);
    }

    /**
     * @return the {@link TargetBlock} at the end of pipeline
     */
    TargetBlock<LogEvent> getEventHandler() {
        Assert.state(eventHandler != null, "EventHandler has not been configured");
        return new EventHandlerSink(eventHandler, logEventExceptionHandler);
    }

    /**
     * Terminating block that adapts an {@link EventHandler} to the {@link TargetBlock} interface.
     */
    static class EventHandlerSink implements TargetBlock<LogEvent> {
        private static final Logger logger = LoggerFactory.getLogger(EventHandlerSink.class);

        private final EventHandler eventHandler;
        private final BiConsumer<LogEvent, Throwable> exceptionHandler;

        EventHandlerSink(
            EventHandler eventHandler,
            @Nullable BiConsumer<LogEvent, Throwable> exceptionHandler
        ) {
            this.eventHandler = Assert.notNull(eventHandler, "eventHandler");
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        public void post(@Nonnull LogEvent logEvent) {
            handle(logEvent);
        }

        @Override
        public void postBatch(@Nonnull Collection<? extends LogEvent> batch) {
            for (final LogEvent logEvent : batch) {
                handle(logEvent);
            }
        }

        private void handle(LogEvent logEvent) {
            try {
                logger.trace("Invoking {}", eventHandler);

                eventHandler.dispatchEvent(logEvent);

                logger.trace("Finished invoking event handler");
            } catch (Exception e) {
                if (exceptionHandler != null) {
                    exceptionHandler.accept(logEvent, e);
                } else {
                    logger.warn("Error while dispatching to {}", eventHandler, e);
                    throw new RuntimeException("Failed to invoke EventHandler", e);
                }
            }
        }
    }
}


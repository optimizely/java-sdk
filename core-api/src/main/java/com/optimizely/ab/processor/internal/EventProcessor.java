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

import com.optimizely.ab.common.callback.CompositeCallback;
import com.optimizely.ab.common.callback.Callback;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.common.plugin.Plugin;
import com.optimizely.ab.common.plugin.PluginSupport;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.processor.ActorBlock;
import com.optimizely.ab.processor.BatchOptions;
import com.optimizely.ab.processor.Blocks;
import com.optimizely.ab.processor.TargetBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Processor that receives events produced by Optimizely SDK public interfaces, i.e. namely conversions and impressions,
 * and processes them before being sent to Optimizely ingestion services via an {@link EventHandler}.
 *
 * The processor consists of a series of well-defined stages and this class provides an interface to configure
 * their parameters and extensions.
 *
 * @param <T> the type of elements fed into the processor
 */
public class EventProcessor<T> implements PluginSupport {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessor.class);

    /**
     * Handler group that transforming stage.
     */
    private CompositeConsumer<T> transformers = new CompositeConsumer<>();

    /**
     * Handler group for intercepting stage.
     */
    private CompositeUnaryOperator<EventBatch> interceptors = new CompositeUnaryOperator<>();

    /**
     * Block that handles event batching.
     *
     * Receives elements from (application) threads producing into this processor.
     * By default, no buffer is used in order to be consistent with past releases. This may change in the future.
     */
    private Supplier<ActorBlock<EventBatch, EventBatch>> batchProcessorProvider = Blocks::identity;

    /**
     * Callbacks to be invoked when an event is finished being dispatched to report success or failure status.
     */
    private CompositeCallback<EventBatch> callbacks = new CompositeCallback<>();

    private ThreadFactory threadFactory;

    // converts the generic input into EventBatch objects
    private Function<? super T, ? extends EventBatch> eventBatchConverter;

    // converts EventBatch to LogEvent that's passed to EventHandler
    private Function<EventBatch, LogEvent> logEventConverter;

    /**
     * Creates the pipeline for processing events and sinks to specified {@link EventHandler}.
     */
    public TargetBlock<T> build(EventHandler eventHandler) {
        return build(eventHandlerAdapter(eventHandler));
    }

    public TargetBlock<T> build(TargetBlock<LogEvent> sink) {
        ActorBlock<T, T> transformProcessor = getTransformProcessor();
        ActorBlock<T, EventBatch> eventBatchConverter = getEventBatchConverter();
        ActorBlock<EventBatch, EventBatch> interceptProcessor = getInterceptProcessor();
        ActorBlock<EventBatch, EventBatch> batchProcessor = batchProcessorProvider.get();
        ActorBlock<EventBatch, LogEvent> logEventConverter = getLogEventConverter();

        transformProcessor.linkTo(eventBatchConverter);
        eventBatchConverter.linkTo(interceptProcessor);
        interceptProcessor.linkTo(batchProcessor);
        batchProcessor.linkTo(logEventConverter);
        logEventConverter.linkTo(sink);

        Blocks.startAll(
            transformProcessor,
            eventBatchConverter,
            interceptProcessor,
            batchProcessor,
            logEventConverter,
            sink
        );

        return transformProcessor; // the front of the pipeline
    }

    static ActorBlock<LogEvent, LogEvent> eventHandlerAdapter(final EventHandler eventHandler) {
        return Blocks.action(logEvent -> {
            try {
                eventHandler.dispatchEvent(logEvent);
            } catch (Exception e) {
                logger.error("Unexpected exception in event dispatcher", e);
            }
        });
    }

    /**
     * Configures the thread factory that will be used to start new threads needed by processor.
     */
    public EventProcessor<T> threadFactory(ThreadFactory threadFactory) {
        this.threadFactory = Assert.notNull(threadFactory, "threadFactory");
        return this;
    }

    /**
     * Registers an action (mutation) to be performed on each element before processing.
     */
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

    /**
     * Registers a predicate that allows events to be filtered.
     * Predicates receive elements after transformer actions have been performed; this allows predicate logic
     * to assume the tested element is "stable".
     *
     * Note: this should not be included in public API until a semantic model class is used for predicate.
     */
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
     * Registers a callback to be notified of successful or failed event dispatch.
     */
    public EventProcessor<T> callback(Callback<EventBatch> callback) {
        this.callbacks.add(Assert.notNull(callback, "callback"));
        return this;
    }

    /**
     * Registers a callback to be notified of successful event dispatch.
     */
    public EventProcessor<T> callback(Consumer<EventBatch> success) {
        return callback(Callback.from(success, (c, ex) -> {}));
    }

    /**
     * Configures batching with the specified parameters
     */
    public EventProcessor<T> batchOptions(final BatchOptions options) {
        if (options != null) {
            this.batchProcessorProvider = () -> Blocks.batch(options, threadFactory);
        } else {
            this.batchProcessorProvider = Blocks::identity;
        }
        return this;
    }

    /**
     * Installs a plugin as a mutation on this instance.
     */
    public EventProcessor<T> plugin(Plugin<EventProcessor<T>> plugin) {
        Assert.notNull(plugin, "plugin");
        plugin.configure(this);
        return this;
    }

    /**
     * Configures the provider used for the batching
     */
    EventProcessor<T> batchProcessorProvider(Supplier<ActorBlock<EventBatch, EventBatch>> provider) {
        this.batchProcessorProvider = Assert.notNull(provider, "provider");
        return this;
    }

    EventProcessor<T> eventBatchConverter(Function<? super T, ? extends EventBatch> converter) {
        this.eventBatchConverter = Assert.notNull(converter, "eventBatchConverter");
        return this;
    }

    EventProcessor<T> logEventConverter(Function<EventBatch, LogEvent> eventFactory) {
        this.logEventConverter = Assert.notNull(eventFactory, "logEventConverter");
        return this;
    }

    protected ActorBlock<T, T> getTransformProcessor() {
        if (transformers == null || transformers.isEmpty()) {
            logger.trace("No transformers are configured");
            return Blocks.identity();
        }
        return Blocks.action(transformers);
    }

    protected ActorBlock<T, EventBatch> getEventBatchConverter() {
        return Blocks.map(eventBatchConverter);
    }

    protected ActorBlock<EventBatch, EventBatch> getInterceptProcessor() {
        if (interceptors == null || interceptors.isEmpty()) {
            logger.trace("No interceptors are configured");
            return Blocks.identity();
        }

        return Blocks.map(interceptors);
    }

    protected ActorBlock<EventBatch, LogEvent> getLogEventConverter() {
        return new ToLogEventBlock(logEventConverter, () -> callbacks);
    }
}


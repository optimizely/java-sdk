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
import com.optimizely.ab.processor.BatchingConfig;
import com.optimizely.ab.processor.BatchingProcessor;
import com.optimizely.ab.processor.BehaviorProcessor;
import com.optimizely.ab.processor.IdentityProcessor;
import com.optimizely.ab.processor.InterceptProcessor;
import com.optimizely.ab.processor.Pipeline;
import com.optimizely.ab.processor.Processor;
import com.optimizely.ab.processor.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Configures the processing flow for dispatching Optimizely events to an {@link EventHandler}.
 *
 * Consists of the following main processing stages:
 *
 * <ol>
 *   <li>Transform</li>
 *   <li>Intercept</li>
 *   <li>Buffer</li>
 *   <li>Batch</li>
 * </ol>
 *
 * @param <T> the type of elements fed into the processor
 */
public class EventProcessor<T> implements PluginSupport {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessor.class);

    private EventHandler eventHandler;

    /**
     * List of consumers to be invoked (in natural order) during the Transform Stage.
     */
    private List<Consumer<? super T>> transformers = new ArrayList<>();

    /**
     * List of interceptors to be invoked (in natural order) during the Intercept Stage.
     */
    private List<InterceptProcessor.Handler<EventBatch>> interceptors = new ArrayList<>();

    /**
     * Converts output elements of Transform Stage to input elements of Intercept Stage.
     */
    private Function<? super T, ? extends EventBatch> converter;

    /**
     * Buffering stage that receives elements from (application) threads producing into this processor.
     *
     * This stage can be asynchronous to enable batching of events.
     *
     * By default, no buffer is used in order to be consistent with previous releases. This may change in the future.
     */
    private Stage<EventBatch, EventBatch> bufferStage = new IdentityProcessor<>();

    /**
     * Callbacks to be invoked when an event is finished being dispatched to report success or failure status.
     */
    private AggregateCallback<EventBatch> callbacks = new AggregateCallback<>();

    private Function<EventBatch, LogEvent> eventFactory = (new EventFactory())::createLogEvent; // TODO do not default

    private BiConsumer<LogEvent, Throwable> logEventExceptionHandler = (logEvent, err) -> {
        logger.error("Error dispatching event: {}", logEvent, err);
    };

    public EventProcessor<T> plugin(Plugin<EventProcessor<T>> plugin) {
        Assert.notNull(plugin, "plugin");
        plugin.configure(this);
        return this;
    }

    public EventProcessor<T> transformer(Consumer<T> transformer) {
        this.transformers.add(Assert.notNull(transformer, "transformer"));
        return this;
    }

    public EventProcessor<T> interceptor(Predicate<EventBatch> filter) {
        Assert.notNull(filter, "interceptor");
        this.interceptors.add(input -> {
            if (!filter.test(input)) {
                return null;
            }
            return input;
        });
        return this;
    }

    public EventProcessor<T> callback(Callback<EventBatch> callback) {
        this.callbacks.add(Assert.notNull(callback, "callback"));
        return this;
    }

    public EventProcessor<T> callback(Consumer<EventBatch> success) {
        return callback(Callback.from(success, (c, ex) -> {}));
    }

    public EventProcessor<T> eventFactory(Function<EventBatch, LogEvent> eventFactory) {
        this.eventFactory = Assert.notNull(eventFactory, "eventFactory");
        return this;
    }

    public EventProcessor<T> eventFactory(EventFactory eventFactory) {
        return eventFactory(Assert.notNull(eventFactory, "eventFactory")::createLogEvent);
    }

    public EventProcessor<T> eventHandler(EventHandler eventHandler) {
        this.eventHandler = Assert.notNull(eventHandler, "eventHandler");
        return this;
    }

    /**
     * Configures the wait strategy for ring buffer consumer
     */
    public EventProcessor<T> bufferStage(Stage<EventBatch, EventBatch> bufferStage) {
        this.bufferStage = Assert.notNull(bufferStage, "bufferStage");
        return this;
    }

    public EventProcessor<T> bufferStage(BatchingConfig config) {
        this.bufferStage = new BatchingProcessor<>(Assert.notNull(config, "config"));
        return this;
    }

    /**
     * Configures the conversion stage between transformers and interceptors.
     */
    EventProcessor<T> converter(Function<? super T, ? extends EventBatch> converter) {
        this.converter = Assert.notNull(converter, "converter");
        return this;
    }

    public Processor<T> build() {
        return Pipeline.buildWith(new BehaviorProcessor<>(new CompositeConsumer<>(transformers)))
            .andThen(Stage.of(converter))
            .andThen(new InterceptProcessor<>(interceptors))
            .andThen(bufferStage)
            .andThen(new EventBatchMergeStage(eventFactory, () -> callbacks))
            .build(new EventHandlerSink(eventHandler, logEventExceptionHandler));
    }

    /**
     * Terminal processor that adapts an {@link EventHandler} to the {@link Processor} interface.
     */
    static class EventHandlerSink implements Processor<LogEvent> {
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
        public void process(LogEvent logEvent) {
            handle(logEvent);
        }

        @Override
        public void processBatch(Collection<? extends LogEvent> batch) {
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


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

import com.optimizely.ab.common.Plugin;
import com.optimizely.ab.common.PluginSupport;
import com.optimizely.ab.common.Callback;
import com.optimizely.ab.common.internal.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class EventProcessor<T, U, R, S extends EventProcessor<T, U, R, S>> implements PluginSupport {
    private EventStage<U, R> channelFactory;
    private List<EventTransformer<T>> transformers;
    private List<EventInterceptor<U>> interceptors;
    private EventCallbackChain<U> callbacks;
    private Function<? super T, ? extends U> converter;
    private EventSink<R> sink;

    public EventProcessor() {
        this.transformers = new ArrayList<>();
        this.interceptors = new ArrayList<>();
        this.callbacks = EventCallbackChain.create();
    }

    /**
     * Configures the conversion step between transformers and interceptors.
     */
    public S converter(Function<? super T, ? extends U> converter) {
        this.converter = Assert.notNull(converter, "converter");
        return self();
    }

    public S transformer(EventTransformer<T> transformer) {
        this.transformers.add(Assert.notNull(transformer, "transformer"));
        return self();
    }

    S interceptor(EventInterceptor<U> interceptor) {
        this.interceptors.add(Assert.notNull(interceptor, "interceptor"));
        return self();
    }

    public S filterInterceptor(Predicate<U> filter) {
        Assert.notNull(filter, "interceptor");
        this.interceptors.add(input -> {
            if (!filter.test(input)) {
                return null;
            }
            return input;
        });
        return self();
    }

    public S callback(Callback<U> callback) {
        this.callbacks.add(Assert.notNull(callback, "callback"));
        return self();
    }

    public S callback(Consumer<U> success) {
        return callback(Callback.from(success, (c, ex) -> {}));
    }

    public S channelFactory(EventStage<U, R> channelFactory) {
        this.channelFactory = Assert.notNull(channelFactory, "channelFactory");
        return self();
    }

    public S sink(EventSink<R> sink) {
        this.sink = Assert.notNull(sink, "sink");
        return self();
    }

    public S plugin(Plugin<S> plugin) {
        Assert.notNull(plugin, "plugin");
        plugin.configure(self());
        return self();
    }

    // EventProcessor.java
    public EventChannel<T> build() {
        return getTransformStage()
            .andThen(getConverterStage())
            .andThen(getInterceptStage())
            .andThen(getBatchStage())
            .createSource(getSink());
    }

    protected EventStage<T, T> getTransformStage() {
        return EventStage.transformers(transformers);
    }

    protected EventStage<T, U> getConverterStage() {
        return EventStage.mapper(converter);
    }

    protected EventStage<U, U> getInterceptStage() {
        return EventStage.interceptors(interceptors);
    }

    protected EventStage<U, R> getBatchStage() {
        // consider returning default buffer?
        return channelFactory;
    }

    protected EventSink<R> getSink() {
        return sink;
    }

    protected Callback<U> getCallback() {
        return callbacks;
    }

    @SuppressWarnings("unchecked")
    protected S self() {
        return (S) this;
    }

    /**
     * Handles {@link Callback} invocations to support application-specific behavior.
     */
    public interface EventCallbackHandler<T> {
        void handleSuccess(List<Callback<T>> callbacks, T input);

        void handleFailure(List<Callback<T>> callbacks, T input, Throwable error);
    }

    /**
     * A composite {@link Callback} that performs on a list of delegates using a {@link EventCallbackHandler}.
     */
    static class EventCallbackChain<T> extends AbstractSequentialList<Callback<T>> implements Callback<T> {

        private EventCallbackHandler<T> handler;

        private List<Callback<T>> callbacks;

        public static <T> EventCallbackChain<T> create() {
            return create(new ContinuingEventCallbackHandler<>());
        }

        public static <T> EventCallbackChain<T> create(EventCallbackHandler<T> handler) {
            return new EventCallbackChain<>(handler);
        }

        public static <T> EventCallbackChain<T> copy(EventCallbackChain<T> other) {
            return new EventCallbackChain<>(other.handler, new CopyOnWriteArrayList<>(other.callbacks));
        }

        EventCallbackChain(EventCallbackHandler<T> handler) {
            this(handler, new LinkedList<>());
        }

        EventCallbackChain(EventCallbackHandler<T> handler, List<Callback<T>> callbacks) {
            this.handler = Assert.notNull(handler, "handler");
            this.callbacks = Assert.notNull(callbacks, "callbacks");
        }

        @Override
        public void success(T context) {
            handler.handleSuccess(callbacks, context);
        }

        @Override
        public void failure(T context, Throwable throwable) {
            handler.handleFailure(callbacks, context, throwable);
        }

        @Override
        public ListIterator<Callback<T>> listIterator(int index) {
            return callbacks.listIterator(index);
        }

        @Override
        public int size() {
            return callbacks.size();
        }

    }

    /**
     * Handler that catches any exception thrown from {@link Callback}, logs it, then continues
     */
    public static class ContinuingEventCallbackHandler<T> implements EventCallbackHandler<T> {
        private static final Logger logger = LoggerFactory.getLogger(ContinuingEventCallbackHandler.class);

        @Override
        public void handleSuccess(List<Callback<T>> callbacks, T input) {
            if (callbacks.isEmpty()) {
                return;
            }

            logger.trace("Invoking {} 'success' callbacks", callbacks.size());
            for (final Callback<T> callback : callbacks) {
                try {
                    callback.success(input);
                } catch (RuntimeException e) {
                    logger.warn("Ignoring exception thrown from 'success' handler", e);
                }
            }
        }

        @Override
        public void handleFailure(List<Callback<T>> callbacks, T input, Throwable error) {
            if (callbacks.isEmpty()) {
                return;
            }

            logger.trace("Invoking {} 'failure' callbacks", callbacks.size());
            for (final Callback<T> callback : callbacks) {
                try {
                    callback.failure(input, error);
                } catch (RuntimeException e) {
                    if (e.equals(error)) { // follow suit if rethrown
                        throw e;
                    }
                    logger.warn("Ignoring exception thrown from 'failure' handler", e);
                }
            }
        }
    }
}

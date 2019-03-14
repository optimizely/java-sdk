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

import com.optimizely.ab.common.callback.Callback;
import com.optimizely.ab.common.callback.AggregateCallback;
import com.optimizely.ab.common.plugin.Plugin;
import com.optimizely.ab.common.plugin.PluginSupport;
import com.optimizely.ab.common.internal.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Configures the processing flow for dispatching Optimizely events.
 *
 * Consists of the following main processing stages:
 *
 * <ol>
 *   <li>Transform</li>
 *   <li>Intercept</li>
 *   <li>Queue</li>
 * </ol>
 *
 * @param <T> the type of elements put into the head of processing flow
 * @param <U> the type of elements received by registered {@link InterceptingStage.InterceptHandler}s and the event queue
 * @param <R> the type of elements received by the terminal {@link Processor}
 * @param <S> the type of subclass; supports the extension of a fluent builder interface.
 */
public class EventProcessorBuilder<T, U, R, S extends EventProcessorBuilder<T, U, R, S>> implements PluginSupport {
    /**
     * List of consumers to be invoked (in natural order) during the Transform Stage.
     */
    private List<Consumer<? super T>> transformers;

    /**
     * List of interceptors to be invoked (in natural order) during the Intercept Stage.
     */
    private List<InterceptingStage.InterceptHandler<U>> interceptors;

    /**
     * Converts output elements of Transform Stage to input elements of Intercept Stage.
     */
    private Function<? super T, ? extends U> converter;

    /**
     * Queue operation that receives output elements from Intercept Stage.
     * Emits output elements to {@link Processor}, either synchronously or asynchronously.
     */
    private ProcessingStage<U, R> queue;

    /**
     * Callbacks to be invoked when an event is finished being dispatched to report success or failure status.
     */
    private AggregateCallback<U> callbacks;

    /**
     * Terminal receiver of the processed output elements.
     */
    private Processor<R> sink;

    public EventProcessorBuilder() {
        this.transformers = new ArrayList<>();
        this.interceptors = new ArrayList<>();
        this.callbacks = new AggregateCallback<>();
    }

    /**
     * Configures the conversion stage between transformers and interceptors.
     */
    public S converter(Function<? super T, ? extends U> converter) {
        this.converter = Assert.notNull(converter, "converter");
        return self();
    }

    public S transformer(Consumer<T> transformer) {
        this.transformers.add(Assert.notNull(transformer, "transformer"));
        return self();
    }

    public S interceptor(Predicate<U> filter) {
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

    public S sink(Processor<R> sink) {
        this.sink = Assert.notNull(sink, "sink");
        return self();
    }

    public S plugin(Plugin<S> plugin) {
        Assert.notNull(plugin, "plugin");
        plugin.configure(self());
        return self();
    }

    // EventProcessor.java
    public Processor<T> build() {
        return getTransformStage()
            .andThen(getConverterStage())
            .andThen(getInterceptStage())
            .andThen(getBatchStage())
            .create(getSink());
    }

    protected ProcessingStage<T, T> getTransformStage() {
        return ProcessingStage.transformers(transformers);
    }

    protected ProcessingStage<T, U> getConverterStage() {
        return ProcessingStage.mapper(converter);
    }

    protected ProcessingStage<U, U> getInterceptStage() {
        return ProcessingStage.interceptors(interceptors);
    }

    protected ProcessingStage<U, R> getBatchStage() {
        // consider returning default buffer?
        return queue;
    }

    protected Processor<R> getSink() {
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

}

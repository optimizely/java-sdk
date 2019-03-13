/**
 * Copyright 2019, Optimizely Inc. and contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Represents an stage in processing flow by producing {@link EventChannel} instances.
 *
 * @param <T> the type of input items
 * @param <R> the type of output items
 */
public interface EventStage<T, R> {
    /**
     * Creates a new channel that outputs
     *
     * @param sink the downstream sink
     * @return a channel that is a sink for items of type {@code T} and a source for items of type {@code R}
     */
    @Nonnull
    EventChannel<T> createSource(@Nonnull EventSink<R> sink);

    /**
     * Creates a composed stage that combines an {@code upstream} stage to this stage.
     *
     * @param <V> the type of input of the {@code upstream} stage, and to stage that's returned.
     * @param upstream the stage to apply to items before this stage
     * @return a composed stage
     * @throws NullPointerException if argument is null
     *
     * @see #andThen(EventStage)
     */
    default <V> EventStage<V, R> compose(EventStage<V, T> upstream) {
        Assert.notNull(upstream, "upstream");
        return sink -> upstream.createSource(createSource(sink));
    }

    /**
     * Creates a composed stage that combines this stage and a {@code downstream} stage.
     *
     * @param <V> the type of output of the {@code downstream} stage, and stage that's returned.
     * @param downstream the stage to apply to items before this stage
     * @return a composed stage
     * @throws NullPointerException if argument is null
     *
     * @see #compose(EventStage)
     */
    default <V> EventStage<T, V> andThen(EventStage<R, V> downstream) {
        Assert.notNull(downstream, "downstream");
        return sink -> createSource(downstream.createSource(sink));
    }

    /**
     * Creates a simple stage from a function that accepts an items and the sink
     * for every items put into the channels produced by the stage.
     *
     * If a batch is put into the a channel, the {@code consumer} is invoked once for each item in batch.
     *
     * @param consumer function invoked on every item put into channels
     * @param <T>      the type of input items
     * @param <R>      the type of output items
     * @return a stage that uses consumer on every event
     * @throws NullPointerException if argument is null
     */
    static <T, R> EventStage<T, R> simple(final BiConsumer<T, EventSink<R>> consumer) {
        Assert.notNull(consumer, "consumer");
        return sink -> new AbstractEventChannel<T, R>(sink) {
            @Override
            public void put(@Nonnull T item) {
                consumer.accept(item, sink);
            }
        };
    }

    /**
     * Creates a stage for channels that produce items that are the results
     * of applying the given function to each put item.
     *
     * If the {@code mapper} returns null, an item will not be emitted to the sink.
     *
     * @param <T>    the type of input items
     * @param <R>    the type of output items
     * @param mapper a transforming function.
     * @return the new stage
     * @throws NullPointerException if argument is null
     */
    static <T, R> EventStage<T, R> mapper(final Function<? super T, ? extends R> mapper) {
        Assert.notNull(mapper, "mapper");
        return sink -> new AbstractEventChannel<T, R>(sink) {
            @Override
            public void put(@Nonnull T item) {
                emitItemIfPresent(mapper.apply(item));
            }
        };
    }

    /**
     * Creates a stage for channels that performing side-effects on input items.
     * Each of the {@code transformers} is invoked on every input item in-order.
     *
     * @param transformers list of transformers
     * @param <T>          the type of input and output items
     * @return the new stage
     * @throws NullPointerException if argument is null
     * @see EventTransformChannel
     */
    static <T> EventStage<T, T> transformers(final List<EventTransformer<T>> transformers) {
        Assert.notNull(transformers, "transformers");
        return sink -> new EventTransformChannel<>(transformers, sink);
    }

    /**
     * Creates a stage for channels that performing filtering or replacement of input items.
     * Each of the {@code interceptors} is invoked on every input item in-order.
     *
     * Each {@link EventInterceptor} is invoked with the value returned from the previous
     * EventInterceptor. The value returned from the last EventInterceptor is emitted to the sink.
     *
     * If a {@link EventInterceptor} returns null, no item will be emitted to the sink and
     * the EventInterceptors that follow it will not be invoked.
     *
     * @param interceptors list of interceptors
     * @param <T>          the type of input and output items
     * @return the new stage
     * @throws NullPointerException if argument is null
     * @see EventInterceptChannel
     */
    static <T> EventStage<T, T> interceptors(final List<EventInterceptor<T>> interceptors) {
        Assert.notNull(interceptors, "interceptors");
        return sink -> new EventInterceptChannel<>(interceptors, sink);
    }
}

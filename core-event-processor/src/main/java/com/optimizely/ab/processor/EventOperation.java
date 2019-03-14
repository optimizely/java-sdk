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
 * Defines a step in processing flow that can be synchronous or asynchronous.
 *
 * Acts as a factory of {@link EventSink} instances when building a processing flow.
 *
 * @param <T> the type of input elements
 * @param <R> the type of output elements
 */
public interface EventOperation<T, R> {
    /**
     * Creates an instance of this operator.
     *
     * @param sink the downstream sink
     * @return a channel that is a sink for elements of type {@code T} and a source for elements of type {@code R}
     */
    @Nonnull
    EventOperator<T> create(@Nonnull EventSink<R> sink);

    /**
     * Creates a composite operation that combines the {@code upstream} operation and this.
     *
     * @param <V> the type of input of the {@code upstream} operation, and to operation that's returned.
     * @param upstream the operation to apply to elements before this operator
     * @return a composed operator
     * @throws NullPointerException if argument is null
     *
     * @see #andThen(EventOperation)
     */
    default <V> EventOperation<V, R> compose(EventOperation<V, T> upstream) {
        Assert.notNull(upstream, "upstream");
        return sink -> upstream.create(create(sink));
    }

    /**
     * Creates a composite operation that combines this and the {@code downstream} operation.
     *
     * @param <V> the type of output of the {@code downstream} operation, and operation that's returned.
     * @param downstream the operation to apply to elements before this operation
     * @return a composite operation
     * @throws NullPointerException if argument is null
     *
     * @see #compose(EventOperation)
     */
    default <V> EventOperation<T, V> andThen(EventOperation<R, V> downstream) {
        Assert.notNull(downstream, "downstream");
        return sink -> create(downstream.create(sink));
    }

//    static <T> EventOperation<T, T> identity() {
//        return sink -> sink;
//    }

    /**
     * Creates a synchronous operation from a function.
     *
     * When a batch of inputs are sent, the {@code consumer} is invoked once for each element in batch.
     *
     * @param consumer function invoked on every element put into channels
     * @param <T>      the type of input elements
     * @param <R>      the type of output elements
     * @return an operation that uses consumer on every event
     * @throws NullPointerException if argument is null
     */
    static <T, R> EventOperation<T, R> simple(final BiConsumer<T, EventSink<R>> consumer) {
        Assert.notNull(consumer, "consumer");
        return sink -> new BaseEventOperator<T, R>(sink) {
            @Override
            public void send(@Nonnull T element) {
                consumer.accept(element, sink);
            }
        };
    }

    /**
     * Creates an operation for channels that produce elements that are the results
     * of applying the given function to each put element.
     *
     * If the {@code mapper} returns null, an element will not be emitted to the sink.
     *
     * @param <T>    the type of input elements
     * @param <R>    the type of output elements
     * @param mapper a transforming function.
     * @return the new operation
     * @throws NullPointerException if argument is null
     */
    static <T, R> EventOperation<T, R> mapper(final Function<? super T, ? extends R> mapper) {
        Assert.notNull(mapper, "mapper");
        return sink -> new BaseEventOperator<T, R>(sink) {
            @Override
            public void send(@Nonnull T element) {
                emitElementIfPresent(mapper.apply(element));
            }
        };
    }

    /**
     * Creates an operation for channels that performing side-effects on input elements.
     * Each of the {@code transformers} is invoked on every input element in-order.
     *
     * @param transformers list of transformers
     * @param <T>          the type of input and output elements
     * @return the new operation
     * @throws NullPointerException if argument is null
     * @see EventTransformOperator
     */
    static <T> EventOperation<T, T> transformers(final List<EventTransformer<T>> transformers) {
        Assert.notNull(transformers, "transformers");
        return sink -> new EventTransformOperator<>(transformers, sink);
    }

    /**
     * Creates an operation for channels that performing filtering or replacement of input elements.
     * Each of the {@code interceptors} is invoked on every input element in-order.
     *
     * Each {@link EventInterceptor} is invoked with the value returned from the previous
     * EventInterceptor. The value returned from the last EventInterceptor is emitted to the sink.
     *
     * If a {@link EventInterceptor} returns null, no element will be emitted to the sink and
     * the EventInterceptors that follow it will not be invoked.
     *
     * @param interceptors list of interceptors
     * @param <T>          the type of input and output elements
     * @return the new operation
     * @throws NullPointerException if argument is null
     * @see EventInterceptOperator
     */
    static <T> EventOperation<T, T> interceptors(final List<EventInterceptor<T>> interceptors) {
        Assert.notNull(interceptors, "interceptors");
        return sink -> new EventInterceptOperator<>(interceptors, sink);
    }
}

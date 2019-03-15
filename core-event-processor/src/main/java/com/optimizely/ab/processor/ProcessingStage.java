/**
 * Copyright 2019, Optimizely Inc. and contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Defines an source or intermediate pipeline stage.
 *
 * Produces a {@link Processor} that performs the stage's work
 * and feeds output to a downstream stage or sink.
 *
 * Contains methods to connect multiple stages together in a simple
 * push-based pipeline flow.
 *
 * @param <T> the type of input elements
 * @param <R> the type of output elements
 */
public interface ProcessingStage<T, R> {
    /**
     * Creates an instance of this operator.
     *
     * @param sink the downstream sink
     * @return a operator that is a sink for elements of type {@code T} and a
     *         source for elements of type {@code R}
     */
    @Nonnull
    Processor<T> create(@Nonnull Processor<R> sink);

    /**
     * Creates a composite stage that combines the {@code upstream} stage and this.
     *
     * @param <V> the type of input of the {@code upstream} stage, and to stage that's returned.
     * @param upstream the stage to apply to elements before this operator
     * @return a composed operator
     * @throws NullPointerException if argument is null
     *
     * @see #andThen(ProcessingStage)
     */
    default <V> ProcessingStage<V, R> compose(ProcessingStage<V, T> upstream) {
        Assert.notNull(upstream, "upstream");
        return sink -> upstream.create(create(sink));
    }

    /**
     * Creates a composite stage that connects this and the {@code downstream} stage.
     *
     * @param <V> the type of output of the {@code downstream} and returned stage
     * @param downstream the stage to apply to elements before this stage
     * @return a composite stage
     * @throws NullPointerException if argument is null
     *
     * @see #compose(ProcessingStage)
     */
    default <V> ProcessingStage<T, V> andThen(ProcessingStage<R, V> downstream) {
        Assert.notNull(downstream, "downstream");
        return sink -> create(downstream.create(sink));
    }

    /**
     * Returns an stage that always returns its input stage.
     *
     * @param <T> the type of the input elements and output elements to the stage
     * @return a stage that always returns its input argument
     */
    static <T> ProcessingStage<T, T> identity() {
        return sink -> sink;
    }

    /**
     * Creates a synchronous stage from a function.
     *
     * When a batch of inputs are sent, the {@code consumer} is invoked once for each element in batch.
     *
     * @param consumer function invoked on every element put into operators
     * @param <T> the type of input elements
     * @param <R> the type of output elements
     * @return an stage that uses consumer on every event
     * @throws NullPointerException if argument is null
     */
    static <T, R> ProcessingStage<T, R> simple(final BiConsumer<T, Processor<R>> consumer) {
        Assert.notNull(consumer, "consumer");
        return sink -> new AbstractProcessor<T, R>(sink) {
            @Override
            public void process(@Nonnull T element) {
                consumer.accept(element, sink);
            }
        };
    }

    /**
     * Creates a synchronous stage that produces elements that are the results
     * of applying the given function to each put element.
     *
     * If the {@code mapper} returns null, an element will not be emitted to the sink.
     *
     * @param <T> the type of input elements
     * @param <R> the type of output elements
     * @param mapper a transforming function.
     * @return the new stage
     * @throws NullPointerException if argument is null
     */
    static <T, R> ProcessingStage<T, R> mapper(final Function<? super T, ? extends R> mapper) {
        Assert.notNull(mapper, "mapper");
        return sink -> new AbstractProcessor<T, R>(sink) {
            @Override
            public void process(@Nonnull T element) {
                emitElementIfPresent(mapper.apply(element));
            }
        };
    }

    /**
     * Creates an synchronous stage that performs side-effects on input elements.
     * Each of the {@code transformers} is invoked on every input element in-order.
     *
     * If an empty list is passed, it is the same as calling {@link #identity()}.
     *
     * @param actions list of transformers
     * @param <T> the type of input and output elements
     * @return the new stage
     * @throws NullPointerException if argument is null
     * @see ForEachStage.ForEachProcessor
     */
    static <T> ProcessingStage<T, T> transformers(final List<Consumer<? super T>> actions) {
        Assert.notNull(actions, "transformers");
        if (actions.isEmpty()) {
            return identity();
        }
        return new ForEachStage<>(actions);
    }

    /**
     * Creates an synchronous stage that performs filtering or replacement of input elements.
     * Each of the {@code handlers} is invoked on every input element in-order.
     *
     * Each {@link InterceptingStage.InterceptHandler} is invoked with the value returned from the previous
     * EventInterceptor. The value returned from the last EventInterceptor is emitted to the sink.
     *
     * If a {@link InterceptingStage.InterceptHandler} returns null, no element will be emitted to the sink and
     * the EventInterceptors that follow it will not be invoked.
     *
     * If an empty list is passed, it is the same as calling {@link #identity()}.
     *
     * @param handlers list of intercept handlers
     * @param <T> the type of input and output elements
     * @return the new stage
     * @throws NullPointerException if argument is null
     * @see InterceptingStage.InterceptProcessor
     */
    static <T> ProcessingStage<T, T> interceptors(final List<InterceptingStage.InterceptHandler<T>> handlers) {
        Assert.notNull(handlers, "handlers");
        if (handlers.isEmpty()) {
            return identity();
        }
        return new InterceptingStage<>(handlers);
    }
}

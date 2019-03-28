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

import com.optimizely.ab.common.internal.Assert;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class consists exclusively of static methods that operate on or return {@link Stage} instances.
 */
public final class Stages {

    /**
     * Creates a stage from a function that takes two arguments: an input and the sink processor. The function will be
     * invoked synchronously for each input element.
     *
     * @param handler function invoked on every element put into operators
     * @param <T> the type of input elements
     * @param <R> the type of output elements
     * @return an stage that uses handler on every event
     * @throws NullPointerException if argument is null
     */
    public static <T, R> Stage<T, R> from(final BiConsumer<T, Processor<? super R>> handler) {
        Assert.notNull(handler, "handler");
        return sink -> new AbstractProcessor<T, R>(sink) {
            @Override
            public void process(@Nonnull T element) {
                handler.accept(element, sink);
            }
        };
    }

    /**
     * Creates a synchronous stage that produces elements that are the results of applying the given function to each put
     * element.
     *
     * @param mapper a transforming function.
     * @param <T> the type of input elements
     * @param <R> the type of output elements
     * @return the new stage
     * @throws NullPointerException if argument is null
     */
    public static <T, R> Stage<T, R> mapping(final Function<? super T, ? extends R> mapper) {
        Assert.notNull(mapper, "mapper");

        return sink -> new AbstractProcessor<T, R>(sink) {
            @Override
            public void process(@Nonnull T element) {
                emitElementIfPresent(mapper.apply(element));
            }
        };
    }

    /**
     * Creates an synchronous stage for behavior triggered on each input element to perform side-effects, such as mutation
     * or logging. Forwards each input element to the sink after invoking the behavior callback.
     *
     * @param onProcess action to accept each input element
     * @param <T> the type of input and output elements
     * @return the new stage
     * @throws NullPointerException if argument is null
     */
    public static <T> Stage<T, T> behavior(final Consumer<? super T> onProcess) {
        Assert.notNull(onProcess, "onProcess");

        return sink -> new AbstractProcessor<T, T>(sink) {
            @Override
            public void process(@Nonnull T element) {
                onProcess.accept(element);
                sink.process(element);
            }
        };
    }

    /**
     * Creates an synchronous stage that performs filtering or replacement of input elements. Each of the {@code handlers}
     * is invoked on every input element in-order.
     *
     * Each {@link InterceptingStage.InterceptHandler} is invoked with the value returned from the previous
     * EventInterceptor. The value returned from the last EventInterceptor is emitted to the sink.
     *
     * If a {@link InterceptingStage.InterceptHandler} returns null, no element will be emitted to the sink and the
     * EventInterceptors that follow it will not be invoked.
     *
     * @param handlers list of intercept handlers
     * @param <T> the type of input and output elements
     * @return the new stage
     * @throws NullPointerException if argument is null
     * @see InterceptingStage.InterceptProcessor
     */
    public static <T> Stage<T, T> interceptors(final List<InterceptingStage.InterceptHandler<T>> handlers) {
        Assert.notNull(handlers, "handlers");
        return new InterceptingStage<>(handlers);
    }

    /**
     * Creates a synchronized (thread-safe) stage backed by the specified stage.
     *
     * @param stage the stage to wrap
     * @param <T> the type of input elements
     * @param <R> the type of output elements
     * @return the synchronized stage
     * @throws NullPointerException if argument is null
     */
    public static <T, R> Stage<T, R> synchronizedStage(final Stage<T, R> stage) {
        Assert.notNull(stage, "stage");
        return sink -> new SynchronizedProcessor<>(stage.getProcessor(sink));
    }

    private Stages() {
        // no instances
    }
}

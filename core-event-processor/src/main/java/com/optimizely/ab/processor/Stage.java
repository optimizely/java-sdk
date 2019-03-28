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
import java.util.function.Function;

/**
 * Composable stage in a processing pipeline. Produces {@link Processor} connected to a downstream sink.
 *
 * @param <T> the type of input elements
 * @param <R> the type of output elements
 */
public interface Stage<T, R> {
    /**
     * Creates an instance of this operator.
     *
     * @param sink the downstream sink
     * @return a operator that is a sink for elements of type {@code T} and a
     *         source for elements of type {@code R}
     */
    @Nonnull
    Processor<T> getProcessor(@Nonnull Processor<? super R> sink);

    /**
     * Creates a composite stage that combines the {@code upstream} stage and this.
     *
     * @param <V> the type of input of the {@code upstream} stage, and to stage that's returned.
     * @param upstream the stage to apply to elements before this operator
     * @return a composed operator
     * @throws NullPointerException if argument is null
     *
     * @see #andThen(Stage)
     */
    default <V> Stage<V, R> compose(Stage<V, T> upstream) {
        Assert.notNull(upstream, "upstream");
        return sink -> upstream.getProcessor(getProcessor(sink));
    }

    /**
     * Creates a composite stage that connects this and the {@code downstream} stage.
     *
     * @param <V> the type of output of the {@code downstream} and returned stage
     * @param downstream the stage to apply to elements before this stage
     * @return a composite stage
     * @throws NullPointerException if argument is null
     *
     * @see #compose(Stage)
     */
    default <V> Stage<T, V> andThen(Stage<? super R, ? extends V> downstream) {
        Assert.notNull(downstream, "downstream");
        return sink -> getProcessor(downstream.getProcessor(sink));
    }

    /**
     * Creates a composite stage that applies a functional transform over the output elements of this stage.
     *
     * @param mapper a function to apply to each element
     * @param <V> the type of output elements
     * @return a composite stage
     * @throws NullPointerException if argument is null
     */
    default <V> Stage<T, V> map(final Function<? super R, ? extends V> mapper) {
        Assert.notNull(mapper, "mapper");
        return andThen(Stages.mapping(mapper));
    }

}

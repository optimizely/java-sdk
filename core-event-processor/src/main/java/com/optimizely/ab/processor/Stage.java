/**
 * Copyright 2019, Optimizely Inc. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @param <T> the type of input elements
 * @param <R> the type of output elements
 */
public interface Stage<T, R> extends Processor<T>, Source<R> {

    /**
     * Creates a {@link Stage} that synchronously applies a function to input elements and emits return value if not
     * null.
     *
     * @param <T> the type of input elements
     * @param <R> the type of output elements
     */
    static <T, R> Stage<T, R> mapping(final Function<? super T, ? extends R> function) {
        Assert.notNull(function, "function");
        return new StageProcessor<T, R>() {
            @Override
            public void process(@Nonnull T element) {
                emitElementIfPresent(function.apply(element));
            }
        };
    }

    /**
     * Creates a pass-through {@link Stage} that immediately emits the elements it receives downstream.
     *
     * @param <T> the type of input and output elements
     */
    static <T> Stage<T, T> identity() {
        return new StageProcessor<T, T>() {
            @Override
            public void process(@Nonnull T element) {
                emitElement(element);
            }
        };
    }


    /**
     * Creates a {@link Stage} to perform an action for side-effects on each input, such as mutation or logging.
     *
     * @param <T> the type of input and output elements
     */
    static <T> Stage<T, T> behavior(final Consumer<? super T> action) {
        Assert.notNull(action, "action");
        return new StageProcessor<T, T>() {
            @Override
            public void process(@Nonnull T element) {
                action.accept(element);
                emitElement(element);
            }
        };
    }
}

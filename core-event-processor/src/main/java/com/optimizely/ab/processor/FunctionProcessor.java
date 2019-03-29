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
import java.util.function.Function;

/**
 * A synchronous processor that applies a function to input elements and emits a non-null return value.
 *
 * @param <T> the type of input elements
 * @param <R> the type of output elements
 */
public class FunctionProcessor<T, R> extends StageProcessor<T, R> {
    private final Function<? super T, ? extends R> function;

    FunctionProcessor(
        Function<? super T, ? extends R> function
    ) {
        this.function = Assert.notNull(function, "function");
    }

    @Override
    public void process(@Nonnull T element) {
        emitElementIfPresent(function.apply(element));
    }
}

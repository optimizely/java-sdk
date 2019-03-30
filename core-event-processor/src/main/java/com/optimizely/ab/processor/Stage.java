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

import java.util.function.Function;

/**
 * @param <T> the type of input elements
 * @param <R> the type of output elements
 */
public interface Stage<T, R> extends Processor<T>, Source<R> {
    static <T, R> Stage<T, R> of(Function<? super T, ? extends R> function) {
        return new FunctionProcessor<>(function);
    }

    static <T> Stage<T, T> identity() {
        return IdentityProcessor.getInstance();
    }
}

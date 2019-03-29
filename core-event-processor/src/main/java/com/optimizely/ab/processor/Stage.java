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

import javax.annotation.Nonnull;

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
}

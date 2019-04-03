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

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Performs a processing task on received elements.
 *
 * @param <T> the type of input elements
 * @see Stage
 */
@FunctionalInterface
public interface Processor<T> {
    /**
     * Pushes a single element to be process
     *
     * @param element the element to push
     */
    void process(@Nonnull T element);

    /**
     * Sends a batch of elements
     *
     * @param elements the elements to put
     */
    default void processBatch(@Nonnull Collection<? extends T> elements) {
        for (final T element : elements) {
            process(element);
        }
    }
}

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
 * Represents a dataflow {@link Block} that is a target (sink) of data.
 *
 * This interface is designed to allow producers to offer input elements to {@link TargetBlock}
 * implementations either individually, via {@link #post}, or (optionally) in bulk,
 * via {@link #postBatch}.
 *
 * @param <TInput> the type of input elements
 */
@FunctionalInterface
public interface TargetBlock<TInput> extends Block {
    /**
     * Posts an element.
     */
    void post(@Nonnull TInput element);

    /**
     * Posts all of the elements in specified collection to this {@link TargetBlock}
     * (optional operation). Default behavior is sequentially invoking {@link #post}
     * with each item in {@code collections}.
     *
     * This operation is not necessarily performed atomically unless specified otherwise
     * in an implementation.So it is possible to fail (throwing an exception) after posting
     * only some of the elements in {@code elements}.
     *
     * The specified collection must not be null and should not contain null elements.
     * Implementations should tolerate {@code collections} being empty (size 0), however
     * it is preferred that caller simply doesn't invoke {@link #postBatch(Collection)}}.
     *
     * @throws NullPointerException if {@code elements} is null
     */
    default void postBatch(@Nonnull Collection<? extends TInput> elements) {
        for (final TInput element : elements) {
            if (element != null) {
                post(element);
            }
        }
    }

    // TODO consider postAsync
}

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
import java.util.function.Function;

/**
 * This class consists exclusively of static methods that operate on or return {@link Stage} instances.
 */
public final class Stages {
    /**
     * Creates a synchronous stage that produces elements that are the results of applying the given function to each put
     * element.
     *
     * @param mapper the mapping function
     * @param <T> the type of input elements
     * @param <R> the type of output elements
     * @return the new stage
     * @throws NullPointerException if argument is null
     */
    public static <T, R> Stage<T, R> mapStage(final Function<? super T, ? extends R> mapper) {
        Assert.notNull(mapper, "mapper");

        return sink -> new SourceProcessor<T, R>(sink) {
            @Override
            public void process(@Nonnull T element) {
                emitElementIfPresent(mapper.apply(element));
            }
        };
    }

    private Stages() {
        // no instances
    }
}

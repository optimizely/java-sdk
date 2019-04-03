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
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * A stateful sink that performs reduction operation of a {@link Collector} over each input element.
 *
 * @param <T> the type of input elements to the reduction operation
 * @param <A> the mutable accumulation type of the reduction operation (often hidden as an implementation detail)
 * @param <R> the result type of the reduction operation
 * @see Collectors
 */
public class CollectorSink<T, A, R> implements Processor<T>, Supplier<R> {
    private final Collector<T, A, R> collector;
    private A state;

    public static <T> CollectorSink<T, ?, List<T>> list() {
        return new CollectorSink<>(Collectors.toList());
    }

    public CollectorSink(Collector<T, A, R> collector) {
        this.collector = Assert.notNull(collector, "collector");
        this.state = collector.supplier().get();
    }

    @Override
    public void process(@Nonnull T element) {
        collector.accumulator().accept(state, element);
    }

    @Override
    public R get() {
        return collector.finisher().apply(state);
    }

    public void clear() {
        state = collector.supplier().get();
    }
}

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
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * This class consists exclusively of static utility methods that operate on or return {@link Block}s.
 */
public final class Blocks {

    /**
     * Creates a block that performs the provided action for every element received, presumably for
     * side-effects, such as mutation or logging.
     *
     * @param <T> the type of input and output elements
     * @return a block that synchronously accepts all elements offered to it, offers to consumer, then forwards to target
     */
    public static <T> ActorBlock<T, T> effect(final Consumer<? super T> action) {
        Assert.notNull(action, "action");
        return new ActorBlock.Base<T, T>() {
            @Override
            public void post(@Nonnull T element) {
                action.accept(element);
                target().post(element);
            }
        };
    }

    /**
     * Creates a block that invokes the provided operator for every element received.
     *
     * @param operator function delegate
     * @param <T> the type of input elements
     * @param <R> the type of output elements
     * @return new block
     */
    public static <T, R> ActorBlock<T, R> map(final Function<? super T, ? extends R> operator) {
        Assert.notNull(operator, "operator");
        return new ActorBlock.Base<T, R>() {
            @Override
            public void post(@Nonnull T element) {
                target().postNullable(operator.apply(element));
            }
        };
    }

    public static <T, R> ActorBlock<T, R> flatMap(final Function<? super T, ? extends Collection<? extends R>> operator) {
        Assert.notNull(operator, "operator");
        return new ActorBlock.Base<T, R>() {
            @Override
            public void post(@Nonnull T element) {
                Collection<? extends R> coll = operator.apply(element);
                if (coll != null) {
                    target().postBatch(coll);
                }
            }
        };
    }

    /**
     * Creates a block that tests each element against a condition and forwards only those that pass.
     *
     * @param condition predicate delegate
     * @param <T> the type of input and output elements
     * @return new block
     */
    public static <T> ActorBlock<T, T> filter(final Predicate<? super T> condition) {
        Assert.notNull(condition, "condition");
        return new ActorBlock.Base<T, T>() {
            @Override
            public void post(@Nonnull T element) {
                if (condition.test(element)) {
                    target().post(element);
                }
            }
        };
    }

    /**
     * @return a new target block that performs a reduction operation
     * @see CollectBlock
     */
    public static <T, A, R> TerminalBlock<T, R> collect(final Collector<? super T, A, R> collector) {
        return new CollectBlock<>(collector);
    }

    /**
     * @return an block (async) that batches inputs into collections, bounded by size and/or time
     * @see BatchBlock
     */
    public static <T> BatchBlock<T> batch(BatchOptions config) {
        return new BatchBlock<>(config);
    }

    /**
     * Creates a pass-through {@link ActorBlock} that immediately emits the elements it receives downstream.
     *
     * @param <T> the type of input and output elements
     */
    public static <T> ActorBlock<T, T> identity() {
        return new ActorBlock.Base<T, T>() {
            @Override
            public void post(@Nonnull T element) {
                target().post(element);
            }
        };
    }


    /**
     * A stateful sink that performs reduction operation of a {@link Collector} over each input element.
     *
     * @param <T> the type of input elements to the reduction operation
     * @param <A> the mutable accumulation type of the reduction operation (often hidden as an implementation detail)
     * @param <R> the result type of the reduction operation
     * @see Collectors
     */
    static class CollectBlock<T, A, R> implements TerminalBlock<T, R> {
        private final Collector<? super T, A, R> collector;
        private A state;

        CollectBlock(Collector<? super T, A, R> collector) {
            this.collector = Assert.notNull(collector, "collector");
            this.state = collector.supplier().get();
        }

        @Override
        public void post(@Nonnull T element) {
            collector.accumulator().accept(state, element);
        }

        @Override
        public R get() {
            return collector.finisher().apply(state);
        }


        @Override
        public void reset() {
            state = collector.supplier().get();
        }
    }

    private Blocks() { /* no instances */ }
}

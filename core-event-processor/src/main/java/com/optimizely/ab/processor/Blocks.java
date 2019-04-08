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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class consists exclusively of static utility methods that operate on or return {@link Block}s.
 */
public final class Blocks {
    private static final IdentityBlock IDENTITY_BLOCK = new IdentityBlock();

    private Blocks() { /* no instances */ }

    /**
     * Creates a pass-through {@link ActorBlock} that immediately emits the elements it receives downstream.
     *
     * @param <T> the type of input and output elements
     */
    @SuppressWarnings("unchecked")
    public static <T> ActorBlock<T, T> identity() {
        return (Actor<T, T>) IDENTITY_BLOCK;
    }

    /**
     * Creates a block that performs the provided action for every element received,
     * then relays the element downstream.
     *
     * Can be used to perform side-effects, such as mutation or logging.
     *
     * @param <T> the type of input and output elements
     * @return a new block
     */
    public static <T> ActorBlock<T, T> action(final Consumer<? super T> action) {
        Assert.notNull(action, "action");
        return new Actor<T, T>() {
            @Override
            public void post(@Nonnull T element) {
                action.accept(element);
                emit(element);
            }
        };
    }

    /**
     * Creates a block that invokes the provided operator for every element received.
     *
     * @param operator function delegate
     * @param <T> the type of input elements
     * @param <R> the type of output elements
     * @return a new block
     */
    public static <T, R> ActorBlock<T, R> map(final Function<? super T, ? extends R> operator) {
        Assert.notNull(operator, "operator");
        return new Actor<T, R>() {
            @Override
            public void post(@Nonnull T element) {
                emit(operator.apply(element));
            }
        };
    }

    public static <T, R> ActorBlock<T, R> flatMap(final Function<? super T, ? extends Collection<? extends R>> operator) {
        Assert.notNull(operator, "operator");
        return new Actor<T, R>() {
            @Override
            public void post(@Nonnull T element) {
                Collection<? extends R> coll = operator.apply(element);
                if (coll != null) {
                    emitBatch(coll);
                }
            }
        };
    }

    /**
     * Creates a block that tests each element against a condition and forwards only those that pass.
     *
     * @param condition predicate delegate
     * @param <T> the type of input and output elements
     * @return a new block
     */
    public static <T> ActorBlock<T, T> filter(final Predicate<? super T> condition) {
        Assert.notNull(condition, "condition");
        return new Actor<T, T>() {
            @Override
            public void post(@Nonnull T element) {
                if (condition.test(element)) {
                    emit(element);
                }
            }
        };
    }

    /**
     * Creates a block that consumes elements using a {@link Collector}, similar to {@link Stream#collect(Collector)}.
     *
     * @param collector describes the reduction
     * @return a new target block that performs a reduction operation
     * @see Collectors
     */
    public static <T, A, R> OutputBlock<T, R> collect(final Collector<? super T, A, R> collector) {
        return new CollectBlock<>(collector);
    }

    /**
     * @return an block (async) that batches inputs into collections, bounded by size and/or time
     * @see BatchBlock
     */
    public static <T> BatchBlock<T> batch(BatchOptions config) {
        return new BatchBlock<>(config, Executors.defaultThreadFactory()); // TODO get a shared thread factory for SDK
    }

    /**
     * @return an block (async) that batches inputs into collections, bounded by size and/or time
     * @see BatchBlock
     */
    public static <T> BatchBlock<T> batch(BatchOptions config, ThreadFactory threadFactory) {
        return new BatchBlock<>(config, threadFactory);
    }

    /**
     * Performs start sequence on the specified blocks
     */
    public static void startAll(Block... blocks) {
        for (int i = blocks.length - 1; i >= 0; i--) {
            Block block = blocks[i];
            if (block != null) {
                block.onStart();
            }
        }
    }

    /**
     * Performs stop sequence on the specified blocks
     */
    public static void stopAll(Block... blocks) {
        for (final Block block : blocks) {
            block.onStop();
        }
    }

    /**
     * Class for pass-through blocks
     */
    private static class IdentityBlock extends Actor<Object, Object> {
        @Override
        public void post(@Nonnull Object element) {
            emit(element);
        }
    }

    /**
     * A stateful sink that performs reduction operation of a {@link Collector} over each input element.
     *
     * @param <T> the type of input elements to the reduction operation
     * @param <A> the mutable accumulation type of the reduction operation (often hidden as an implementation detail)
     * @param <R> the result type of the reduction operation
     * @see Collectors
     */
    static class CollectBlock<T, A, R> implements OutputBlock<T, R> {
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

    /**
     * This class provides skeletal implementation for {@link ActorBlock}
     */
    abstract static class Actor<T, R> extends Source<R> implements ActorBlock<T, R> {
    }

    /**
     * This class provides skeletal implementation for {@link SourceBlock} to interface to minimize the effort required to
     * implement the interface.
     *
     * Maintains link state to {@link TargetBlock} and the associated {@link LinkOptions} and propagates completion signal
     * accordingly.
     *
     * @param <TOutput> the type of output elements; the type of elements accepted by downstream.
     */
    public abstract static class Source<TOutput> implements SourceBlock<TOutput> {
        // currently support only a single target; could change in future
        private TargetBlock<? super TOutput> target;
        private LinkOptions options;

        @Override
        public synchronized void linkTo(TargetBlock<? super TOutput> target, LinkOptions options) {
            this.target = target;
            if (target != null) {
                this.options = (options != null) ? options : LinkOptions.create();
            } else {
                this.options = null;
            }
        }

        @Override
        public void onStart() {
            // no-op
        }

        @Override
        public boolean onStop(long timeout, TimeUnit unit) {
            if (options != null && options.getPropagateCompletion()) {
                target.onStop();
            }
            return true;
        }

        /**
         * @throws IllegalStateException if target not linked
         */
        protected void emit(TOutput element) {
            emit(element, false);
        }

        protected void emit(TOutput element, boolean requireTarget) {
            if (element == null) {
                return;
            }

            if (target == null) {
                if (requireTarget) {
                    throw new IllegalStateException("Not linked to a target");
                }
                return;
            }

            target.post(element);
        }

        /**
         * @throws IllegalStateException if target not linked
         */
        protected void emitBatch(Collection<? extends TOutput> elements) {
            emitBatch(elements, false);
        }

        protected void emitBatch(Collection<? extends TOutput> elements, boolean requireTarget) {
            if (elements == null) {
                return;
            }

            if (target == null) {
                if (requireTarget) {
                    throw new IllegalStateException("Not linked to a target");
                }
                return;
            }

            target.postBatch(elements);
        }
    }
}

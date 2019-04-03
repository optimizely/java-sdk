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
import com.optimizely.ab.common.lifecycle.LifecycleAware;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A sequence of {@link Stage}s that form a processing pipeline.
 *
 * The stages are linked together to form a unified {@link StageProcessor}.
 *
 * @param <T> the type of input elements for first stage in pipeline
 * @param <R> the type of output elements
 */
public class Pipeline<T, R> implements Stage<T, R>, LifecycleAware {
    private final Pipe<T, ?, R> lastStage;
    private Processor<? super T> head;
    private final List<Stage> stages;

    @SuppressWarnings("unchecked")
    public Pipeline(Pipe<T, ?, R> lastStage) {
        this.lastStage = Assert.notNull(lastStage, "lastStage");
        List<Stage> stages = lastStage.stages();
        Assert.argument(!stages.isEmpty(), "stages cannot be empty");
        this.stages = new ArrayList<>(stages); //
        this.head = (Processor<? super T>) this.stages.get(0);
    }

    @Override
    public void configure(Processor<? super R> sink) {
        linkStages(stages, sink);
    }

    /**
     * Starts stages from last to first
     */
    @Override
    public void onStart() {
        for (int i = stages.size() - 1; i >= 0; i--) {
            Stage stage = stages.get(i);
            LifecycleAware.start(stage);
        }
    }

    /**
     * Stops stages from first to last
     */
    @Override
    public boolean onStop(long timeout, TimeUnit unit) {
        for (final Stage stage : stages) {
            LifecycleAware.stop(stage, timeout, unit);
        }
        return true;
    }

    @Override
    public void process(@Nonnull T element) {
        head.process(element);
    }

    @Override
    public void processBatch(@Nonnull Collection<? extends T> elements) {
        head.processBatch(elements);
    }

    /**
     * @return an unmodifiable list of stages in pipeline
     */
    public List<Stage> getStages() {
        return Collections.unmodifiableList(stages);
    }

    /**
     * Links the stages together, flowing into the sink.
     *
     * Configures the stages by traversing in reverse order and configuring the downstream {@link Processor} to be the
     * upstream {@link Stage} in the pipeline.
     *
     * @return the first processor in pipeline
     */
    @SuppressWarnings("unchecked")
    static <R> void linkStages(List<Stage> stages, Processor<? super R> sink) {
        ListIterator<Stage> it = stages.listIterator(stages.size());
        Processor downstream = sink;
        while (it.hasPrevious()) {
            Stage stage = it.previous();
            stage.configure(downstream);
            downstream = stage;
        }
    }

    /**
     * A {@link Pipe} represents a list of {@link Stage} chained together.
     *
     * This class that provides a type-safe fluent interface for chaining {@link Stage} together,
     * with the ability to create {@link Pipeline} objects.
     *
     * Does not support cycles.
     *
     * @param <HEAD_IN> the type of input elements accepted by first {@link Stage} in topology
     * @param <E_IN> the type of input elements accepted by current {@link Stage} in topology
     * @param <E_OUT> the type of output elements emitted by current {@link Stage} in topology
     */
    public abstract static class Pipe<HEAD_IN, E_IN, E_OUT> {

        /**
         * Creates initial {@link Pipe} in a chain
         */
        public static <T, R> Pipe<T, T, R> of(Stage<? super T, ? extends R> head) {
            Assert.notNull(head, "head");
            return new RealPipe<>(head);
        }

        /**
         * @return the {@link Stage} represented by this instance
         */
        abstract Stage<? super E_IN, ? extends E_OUT> get();

        abstract Pipe<HEAD_IN, ?, E_IN> prev();

        abstract boolean isEmpty();

        /**
         * Creates a successor {@link Pipe}.
         *
         * @param stage a stage with input type compatible with output type of this stage
         * @param <U> the output type of the sink {@link Stage}
         * @return a new instance with source of current
         */
        public <U> Pipe<HEAD_IN, E_OUT, U> to(Stage<? super E_OUT, ? extends U> stage) {
            walk(s -> {
                if (s.equals(stage)) {
                    throw new IllegalArgumentException("Cycle detected: " + stage);
                }
            });

            return new RealPipe<>(stage, this);
        }

        /**
         * Returns list of {@link Stage}, starting from inner-most and ending with current.
         * Verifies cycles do not exist;
         *
         * @return a stack of {@link Stage} linked to this,
         * @throws IllegalStateException if cycle is detected in chain
         */
        public List<Stage> stages() {
            LinkedList<Stage> stages = new LinkedList<>();
            walk(stages::push);
            return stages;
        }

        /**
         * @return a {@link Pipeline} instance containing all stages in chain up to and including this stage
         */
        public Pipeline<HEAD_IN, E_OUT> toPipeline() {
            return new Pipeline<>(this);
        }

        /**
         * Walk the list of {@link Stage} in reverse order using recursion (not tail-optimized).
         */
        public void walk(Consumer<Stage> visitor) {
            if (isEmpty()) {
                return;
            }
            visitor.accept(get());
            prev().walk(visitor);
        }

        /**
         * @return an empty {@link Pipe} special-value. Dummy node of sorts.
         */
        @SuppressWarnings("unchecked")
        static <T, R> Pipe<T, ?, R> nil() {
            return (Pipe<T, ?, R>) NilPipe.NIL;
        }

        private static class NilPipe<HEAD_IN, E_IN, E_OUT> extends Pipe<HEAD_IN, E_IN, E_OUT> {
            private static final Pipe NIL = new NilPipe();

            @Override
            Stage<? super E_IN, ? extends E_OUT> get() {
                throw new NoSuchElementException();
            }

            @Override
            Pipe<HEAD_IN, ?, E_IN> prev() {
                throw new NoSuchElementException();
            }

            @Override
            boolean isEmpty() {
                return true;
            }
        }

        private static class RealPipe<HEAD_IN, E_IN, E_OUT> extends Pipe<HEAD_IN, E_IN, E_OUT> {
            private final Stage<? super E_IN, ? extends E_OUT> value;
            private final Pipe<HEAD_IN, ?, E_IN> prev;

            private RealPipe(Stage<? super E_IN, ? extends E_OUT> value) {
                this(value, nil());
            }

            private RealPipe(
                Stage<? super E_IN, ? extends E_OUT> value,
                Pipe<HEAD_IN, ?, E_IN> prev
            ) {
                this.value = Assert.notNull(value, "value");
                this.prev = Assert.notNull(prev, "prev");
            }

            @Override
            Stage<? super E_IN, ? extends E_OUT> get() {
                return value;
            }

            @Override
            Pipe<HEAD_IN, ?, E_IN> prev() {
                return prev;
            }

            @Override
            boolean isEmpty() {
                return false;
            }
        }
    }
}

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
import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;

/**
 * A sequence of {@link Stage}s that form a processing pipeline.
 *
 * The stages are linked together to form a unified {@link StageProcessor}.
 *
 * @param <T> the type of input elements for first stage in pipeline
 * @param <R> the type of output elements
 */
public class Pipeline<T, R> extends StageProcessor<T, R> {
    private final List<Stage> stages;
    private Processor<? super T> head;

    /**
     * Creates an object for creating {@link Pipeline} with a type-safe interface.
     *
     * @param stage the first stage in a pipeline
     * @param <T> the type of input elements accepted by the pipeline
     * @param <R> the type of output elements emitted by first stage in pipeline
     * @return a {@link Chain} for the given stage
     * @throws NullPointerException if argument is null
     */
    public static <T, R> Chain<T, T, R> pipe(Stage<T, R> stage) {
        Assert.notNull(stage, "stage");
        return new Chain<>(null, stage, stage);
    }

    /**
     * Constructs the pipeline from a sequence of stages
     *
     * @param stages the processing stages of pipeline
     */
    @SuppressWarnings("unchecked")
    private Pipeline(final List<Stage> stages) {
        Assert.argument(!stages.isEmpty(), "stages cannot be empty");
        this.stages = Assert.notNull(stages, "stages");
        this.head = (Processor<? super T>) stages.get(0);
    }

    @Override
    public void configure(Processor<? super R> sink) {
        super.configure(sink);
        linkStages(stages, sink);
    }

    @Override
    protected void beforeStart() {
        /// start the head
        LifecycleAware.start(head);
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
     * This class that provides a type-safe fluent interface for chaining {@link Stage} together,
     * with the ability to create {@link Pipeline} objects.
     *
     * @param <PipelineInput> the type of input accepted by pipeline
     * @param <StageInput> the type of input element  the contained stage
     * @param <StageOutput> the type of output
     */
    public static class Chain<PipelineInput, StageInput, StageOutput> implements Supplier<Stage<? super StageInput, ? extends StageOutput>> {
        private final Chain<PipelineInput, ?, ?> upstream;
        private final Processor<PipelineInput> head;
        private final Stage<? super StageInput, ? extends StageOutput> stage;

        private Chain(
            Chain<PipelineInput, ?, ?> upstream,
            Processor<PipelineInput> head,
            Stage<? super StageInput, ? extends StageOutput> stage
        ) {
            this.upstream = upstream;
            this.head = head;
            this.stage = stage;
        }

        /**
         * @return the {@link Stage} represented by this instance
         */
        @Override
        public Stage<? super StageInput, ? extends StageOutput> get() {
            return stage;
        }

        /**
         * @return the instance upstream to this or null when this is in head position
         */
        public Chain<PipelineInput, ?, ?> prev() {
            return upstream;
        }

        /**
         * Returns a new instance linked to this
         *
         * @param stage a stage with input type compatible with output type of this stage
         * @param <U> the type of elements output by passed stage
         * @return a new instance that is linked to this instance
         */
        public <U> Chain<PipelineInput, StageOutput, U> into(Stage<? super StageOutput, ? extends U> stage) {
            return new Chain<>(this, head, stage);
        }

        /**
         * Down-casts the pipeline's input type. Useful if the input type of the pipeline cannot be implicitly inferred by
         * the compiler.
         *
         * @return the current chain with the new end type.
         */
        public <T extends PipelineInput> Chain<T, StageInput, StageOutput> cast(Class<T> inletClass) {
            return (Chain<T, StageInput, StageOutput>) this;
        }

        /**
         * @return an ordered list of all stages in chain up to and including this stage
         */
        public List<Stage> getStages() {
            final List<Stage> stages = new ArrayList<>();

            Chain curr = this;
            while (curr != null) {
                stages.add(curr.stage);
                curr = curr.upstream;
            }

            Collections.reverse(stages);

            return stages;
        }

        /**
         * @return a {@link Pipeline} instance containing all stages in chain up to and including this stage
         */
        public Pipeline<PipelineInput, StageOutput> end() {
            return new Pipeline<>(getStages());
        }
    }
}

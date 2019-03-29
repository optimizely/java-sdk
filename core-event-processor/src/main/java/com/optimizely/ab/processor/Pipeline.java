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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class Pipeline<T, R> implements Stage<T, R> {
    protected Stage<T, ?> first;
    protected Stage<?, R> last;
    protected List<Stage> stages;

    public static <T, R> Builder<T, R> builder() {
        return new Builder<>();
    }

    public static <T, R> Builder<T, R> builder(Pipeline<? super T, ? extends R> pipeline) {
        return new Builder<>(Assert.notNull(pipeline, "pipeline"));
    }

    public static <T, R> Builder<T, R> buildFrom(Stage<T, R> stage) {
        return new Builder<>(stage);
    }

    public <A, B, C> Pipeline<A, C> of(Stage<A, B> s1, Stage<B, C> s2) {
        return new Pipeline<>(s1, s2);
    }

    public <A, B, C, D> Pipeline<A, D> of(Stage<A, B> s1, Stage<B, C> s2, Stage<C, D> s3) {
        return new Pipeline<>(s1, s2, s3);
    }

    public <A, B, C, D, E> Pipeline<A, E> of(Stage<A, B> s1, Stage<B, C> s2, Stage<C, D> s3, Stage<D, E> s4) {
        return new Pipeline<>(s1, s2, s3);
    }

    public <A, B, C, D, E, F> Pipeline<A, F> of(
        Stage<A, B> s1,
        Stage<B, C> s2,
        Stage<C, D> s3,
        Stage<D, E> s4,
        Stage<E, F> s5
    ) {
        return new Pipeline<>(s1, s2, s3, s4, s5);
    }

    Pipeline(Stage... stages) {
        this(new ArrayList<>(Arrays.asList(stages)));
    }

    private Pipeline(final List<Stage> stages) {
        this.stages = Assert.notNull(stages, "stages");
        touchStages();
    }

    /**
     * @return an unmodifiable list of stages in pipeline
     */
    public List<Stage> getStages() {
        return Collections.unmodifiableList(stages);
    }

    @Nonnull
    @Override
    public Processor<T> getProcessor(@Nonnull Processor<? super R> sink) {
        return assemble(stages, sink);
    }

    @SuppressWarnings("unchecked")
    protected static <T, R> Processor<T> assemble(List<Stage> stages, Processor<? super R> sink) {
        ListIterator<Stage> it = stages.listIterator(stages.size());
        Processor current = sink;
        try {
            while (it.hasPrevious()) {
                Stage s = it.previous();
                current = s.getProcessor(current);
            }
            return (Processor<T>) current;
        } catch (ClassCastException e) {
            throw new PipelineDefinitionException("Incompatible stages in pipeline", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void touchStages() {
        int size = stages.size();
        if (size == 0) {
            first = null;
            last = null;
        } else {
            this.first = (Stage<T, ?>) stages.get(0);
            this.last = (Stage<?, R>) stages.get(size - 1);
        }
    }

    /**
     * Fluent builder of a {@link Pipeline}
     *
     * @param <T> the type of input elements for first stage
     * @param <R> the type of output elements for last stage
     */
    @SuppressWarnings("unchecked")
    public static class Builder<T, R> {
        private List<Stage> stages;

        private Builder() {
            this.stages = new ArrayList<>();
        }

        private Builder(Pipeline<? super T, ? extends R> pipeline) {
            this.stages = new ArrayList<>(pipeline.stages);
        }

        private Builder(Stage<? super T, ? extends R> stage) {
            this();
            stages.add(stage);
        }

        public <U> Builder<T, U> andThen(Stage<? super R, ? extends U> stage) {
            if (stage instanceof Pipeline) {
                this.stages.addAll(((Pipeline) stage).stages);
            } else {
                this.stages.add(stage);
            }
            return (Builder<T, U>) this;
        }

        public Pipeline<T, R> build() {
            return new Pipeline<>(stages);
        }

        public Processor<T> getProcessor(Processor<? super R> sink) {
            return build().getProcessor(sink);
        }
    }

    public static class PipelineDefinitionException extends RuntimeException {
        public PipelineDefinitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

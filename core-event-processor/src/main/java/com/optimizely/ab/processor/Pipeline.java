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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class Pipeline<T, R> extends StageProcessor<T, R> {
    protected Stage<T, ?> first;
    protected Stage<?, R> last;
    protected List<Stage> stages;

    public static <T, R> Builder<T, R> builder() {
        return new Builder<>();
    }

    public static <T, R> Builder<T, R> builder(Pipeline<T, R> pipeline) {
        return new Builder<>(Assert.notNull(pipeline, "pipeline"));
    }

    public static <T, R> Builder<T, R> buildWith(Stage<? super T, R> stage) {
        return new Builder<>(stage);
    }

    protected Pipeline(Stage... stages) {
        this(new ArrayList<>(Arrays.asList(stages)));
    }

    private Pipeline(final List<Stage> stages) {
        this.stages = Assert.notNull(stages, "stages");
        touchStages();
    }

    @Override
    public void configure(Processor<? super R> sink) {
        super.configure(sink);
        linkStages(stages, sink);
    }

    @Override
    protected void beforeStart() {
        LifecycleAware.start(first);
    }

    @Override
    public void process(@Nonnull T element) {
        first.process(element);
    }

    @Override
    public void processBatch(@Nonnull Collection<? extends T> elements) {
        first.processBatch(elements);
    }

    /**
     * @return an unmodifiable list of stages in pipeline
     */
    public List<Stage> getStages() {
        return Collections.unmodifiableList(stages);
    }

    @SuppressWarnings("unchecked")
    protected static <T, R> Processor<T> linkStages(List<Stage> stages, Processor<? super R> sink) {
        ListIterator<Stage> it = stages.listIterator(stages.size());
        try {
            Processor current = sink;
            while (it.hasPrevious()) {
                Stage s = it.previous();
                s.configure(sink);
                sink = s;
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
            this.stages.add(stage);
            return (Builder<T, U>) this;
        }

        public Pipeline<T, R> build() {
            return new Pipeline<>(stages);
        }

        public Pipeline<T, R> build(Processor<? super R> sink) {
            Pipeline<T, R> pipeline = build();
            pipeline.configure(sink);
            return pipeline;
        }
    }

    public static class PipelineDefinitionException extends RuntimeException {
        public PipelineDefinitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

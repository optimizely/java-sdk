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
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A synchronous stage that calls a list of actions sequentially on each input element, presumably for side-effects.
 *
 * Feeds the input element to the downstream {@link Processor} after actions have been invoked.
 *
 * Catches any {@link RuntimeException} thrown from an action, logs it, then continues
 *
 * @param <T> the type of input and output elements
 */
public class ForEachStage<T> implements ProcessingStage<T, T> {
    private final List<Consumer<? super T>> actions = new LinkedList<>();

    public ForEachStage(Consumer<? super T> action) {
        this.actions.add(action);
    }

    public ForEachStage(List<Consumer<? super T>> actions) {
        this.actions.addAll(Assert.notNull(actions, "actions"));
    }

    @Nonnull
    @Override
    public Processor<T> create(@Nonnull Processor<T> sink) {
        return new ForEachProcessor<>(actions, sink);
    }

    static class ForEachProcessor<T> extends AbstractProcessor<T, T> {
        private static final String logLabel = ForEachProcessor.class.getSimpleName();

        private final List<Consumer<? super T>> actions;


        private ForEachProcessor(List<Consumer<? super T>> actions, Processor<T> sink) {
            super(sink);
            this.actions = Assert.notNull(actions, "actions");
        }

        @Override
        public void process(@Nonnull T element) {
            logger.trace("[{}] Invoking {} EventTransformer", logLabel, actions.size());

            for (final Consumer<? super T> action : actions) {
                try {
                    action.accept(element);
                } catch (RuntimeException e) {
                    handleException(action, e);
                }
            }

            emitElementIfPresent(element);
        }

        /**
         * Overridable to modify behavior when a transform action throws an exception.
         *
         * @param culprit the action that threw
         * @param ex the thrown exception
         */
        protected void handleException(Consumer<? super T> culprit, RuntimeException ex) {
            logger.warn("[{}] Skipping EventTransformer", logLabel, ex);
        }
    }
}

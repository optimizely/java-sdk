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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A synchronous stage that calls a list of intercept handlers sequentially on each input.
 *
 * If a handler returns {@code null}, the element will be dropped, otherwise the returned value
 * will be passed to the next handler in list if one exists, otherwise to the downstream processor.
 *
 * Catches any {@link RuntimeException} thrown from an interceptor, logs it, then continues
 */
public class InterceptingStage<T> implements ProcessingStage<T, T> {
    private final List<InterceptHandler<T>> interceptors = new LinkedList<>();

    public InterceptingStage(List<InterceptHandler<T>> interceptors) {
        this.interceptors.addAll(Assert.notNull(interceptors, "interceptors"));
    }

    @Nonnull
    @Override
    public Processor<T> create(@Nonnull Processor<? super T> sink) {
        return new InterceptProcessor<>(interceptors, sink);
    }


    public static class InterceptProcessor<T> extends AbstractProcessor<T, T> {
        private static final String logLabel = InterceptProcessor.class.getSimpleName();

        private final List<InterceptHandler<T>> interceptors;

        private InterceptProcessor(List<InterceptHandler<T>> interceptors, Processor<? super T> sink) {
            super(sink);
            this.interceptors = Assert.notNull(interceptors, "interceptors");
        }

        @Override
        public void process(T element) {
            if (interceptors.isEmpty()) {
                emitElementIfPresent(element);
            }

            logger.trace("[{}] Invoking {} interceptors", logLabel, interceptors.size(), element);

            Iterator<InterceptHandler<T>> it = interceptors.iterator();
            while (element != null && it.hasNext()) {
                InterceptHandler<T> interceptor = it.next();

                try {
                    T nextEvent = interceptor.intercept(element);

                    if (nextEvent == null) {
                        logger.debug("[{}] Interceptor filtered event {}", logLabel, element);
                        return;
                    }

                    element = nextEvent;
                } catch (RuntimeException e) {
                    logger.warn("[{}] Skipping interceptor", logLabel, e);
                }
            }

            emitElementIfPresent(element);
        }
    }

    /**
     * Intercepts events being processed; supports monitoring or filtering elements.
     *
     * @param <T> the type of input and output elements
     */
    @FunctionalInterface
    public interface InterceptHandler<T> {
        /**
         * Intercept element
         * @param element the input element
         * @return the element to pass to next step
         */
        T intercept(T element);
    }
}

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
package com.optimizely.ab.processor.internal;

import com.optimizely.ab.common.internal.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Composes a list of consumers as a single consumer.
 *
 * @param <T> the type of input element
 */
class CompositeConsumer<T> implements Consumer<T> {
    private static final Logger logger = LoggerFactory.getLogger(CompositeConsumer.class);

    private final List<Consumer<? super T>> consumers;

    CompositeConsumer(List<Consumer<? super T>> consumers) {
        this.consumers = Assert.notNull(consumers, "consumers");
    }

    @Override
    public void accept(T input) {
        for (final Consumer<? super T> consumer : consumers) {
            try {
                consumer.accept(input);
            } catch (RuntimeException e) {
                handleError(consumer, e);
            }
        }
    }

    protected void handleError(Consumer<? super T> consumer, RuntimeException e) {
        logger.warn("Exception thrown by {}", consumer, e);
    }
}

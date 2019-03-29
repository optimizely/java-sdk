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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.Consumer;

abstract class BaseProcessor<T> implements Processor<T>, Consumer<T> {
    // Share logger with subclasses
    protected static final Logger logger = LoggerFactory.getLogger(Processor.class);

    /**
     * Implements {@link Consumer} interface. Same as calling {@link #process(Object)}
     */
    @Override
    public void accept(T element) {
        process(element);
    }

    /**
     * A default implementation that handles each of the input elements individually.
     */
    @Override
    public void processBatch(Collection<? extends T> elements) {
        for (final T element : elements) {
            process(element);
        }
    }
}

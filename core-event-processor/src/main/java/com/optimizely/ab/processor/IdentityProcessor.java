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

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Simply maps the input to sink without any processing
 *
 * @param <T> the type of input and output elements
 */
public class IdentityProcessor<T> extends SourceProcessor<T, T> {
    public IdentityProcessor(Processor<? super T> sink) {
        super(sink);
    }

    @Override
    public void process(@Nonnull T element) {
        emitElement(element);
    }

    @Override
    public void processBatch(Collection<? extends T> elements) {
        emitBatch(elements);
    }
}

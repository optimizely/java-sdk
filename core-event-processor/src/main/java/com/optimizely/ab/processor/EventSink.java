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
 * Receives elements in a processing flow.
 *
 * This provides a uniform interface for:
 * <ul>
 *   <li>Internal or terminal operations</li>
 *   <li>Synchronous or asynchronous operations</li>
 * </ul>
 *
 * An internal operation will usually have reference to another {@link EventSink} for
 * the downstream operation.
 *
 * @param <T> the type of input elements
 * @see BaseEventOperator
 */
public interface EventSink<T> {
    /**
     * Sends a single element
     *
     * @param element the element to push
     */
    void send(@Nonnull T element);

    /**
     * Sends a batch of elements
     *
     * @param elements the elements to put
     */
    void sendBatch(@Nonnull Collection<? extends T> elements);

    // TODO boolean close()
    // TODO boolean isClosed()

//    static <T> EventSink<T> collection(Collection<? super T> collection) {
//        return new EventSink<T>() {
//            @Override
//            public void send(@Nonnull T element) {
//                collection.add(element);
//            }
//
//            @Override
//            public void sendBatch(@Nonnull Collection<? extends T> elements) {
//                collection.addAll(elements);
//            }
//        };
//    }
}

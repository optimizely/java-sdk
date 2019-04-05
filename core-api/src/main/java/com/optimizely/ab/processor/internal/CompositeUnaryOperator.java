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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * General-purpose composition of {@link UnaryOperator} where each operator after
 * the first receives the element returned from the previous operator in chain.
 *
 * If an operator in {@code chain} returns null, none of the following operators
 * will be invoked and chain will return null.
 *
 * Does not handle operator failure (thrown exceptions).
 *
 * @param <T> the type of input and output elements
 */
class CompositeUnaryOperator<T> implements UnaryOperator<T>, Iterable<UnaryOperator<T>> {
    private final List<UnaryOperator<T>> chain;

    CompositeUnaryOperator() {
        this.chain = new ArrayList<>();
    }

    CompositeUnaryOperator(Collection<UnaryOperator<T>> chain) {
        Assert.notNull(chain, "chain");
        this.chain = new ArrayList<>(chain);
    }

    @Override
    public final T apply(T input) {
        T curr = input;

        for (final UnaryOperator<T> op : chain) {
            T next = op.apply(curr);
            if (next == null) {
                // TODO add callback to notify (log when) dropped event
                return null;
            }
            curr = next;
        }

        return curr;
    }

    @Override
    public Iterator<UnaryOperator<T>> iterator() {
        return chain.iterator();
    }

    public boolean isEmpty() {
        return chain.isEmpty();
    }

    public int size() {
        return chain.size();
    }

    void add(UnaryOperator<T> operator) {
        chain.add(Assert.notNull(operator, "operator"));
    }
}

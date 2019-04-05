/**
 * Copyright 2019, Optimizely Inc. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.optimizely.ab.processor;

/**
 * Represents a dataflow {@link Block} that is both a target for data and a source of data.
 *
 * This interface and its counterparts, {@link SourceBlock} and {@link TargetBlock}, are inspired by .NET TPL. They
 * provide the basic building blocks to define the in-process message passing for coarse-grained dataflow and
 * parallelism.
 *
 * @param <T> the type of input elements
 * @param <R> the type of output elements
 */
public interface ActorBlock<T, R> extends Block, TargetBlock<T>, SourceBlock<R> {
    /**
     * This class provides skeletal implementation for {@link ActorBlock} to interface to minimize the effort required to
     * implement the interface.
     *
     * @param <T>
     * @param <R>
     * @see SourceBlock.Base
     */
    abstract class Base<T, R> extends SourceBlock.Base<R> implements ActorBlock<T, R> {
    }
}

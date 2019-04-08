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

import java.util.Objects;

/**
 * Represents a dataflow {@link Block} that is a source of data.
 *
 * @param <TOutput> the type of output elements
 */
public interface SourceBlock<TOutput> extends Block {
    /**
     * Links the {@link SourceBlock} to the specified {@link TargetBlock}.
     *
     * @param target the target to connect to this source
     * @param options configures the link
     */
    void linkTo(TargetBlock<? super TOutput> target, LinkOptions options);

    /**
     * Links the {@link SourceBlock} to the specified {@link TargetBlock}.
     *
     * @param target the target to connect to this source
     */
    default void linkTo(TargetBlock<? super TOutput> target) {
        linkTo(target, null);
    }

    /**
     * A mutable set of options used to configure a link between {@link Blocks}
     */
    class LinkOptions {
        private boolean propagateCompletion = false;

        public static LinkOptions create() {
            return new LinkOptions();
        }

        private LinkOptions() {
        }

        public boolean getPropagateCompletion() {
            return propagateCompletion;
        }

        public void setPropagateCompletion(boolean propagateCompletion) {
            this.propagateCompletion = propagateCompletion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LinkOptions)) return false;
            final LinkOptions that = (LinkOptions) o;
            return propagateCompletion == that.propagateCompletion;
        }

        @Override
        public int hashCode() {
            return Objects.hash(propagateCompletion);
        }
    }
}

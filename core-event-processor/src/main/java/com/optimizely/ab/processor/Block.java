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

import com.optimizely.ab.common.lifecycle.LifecycleAware;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Represents a block in a dataflow.
 */
public interface Block extends LifecycleAware {
    // TODO CompletableFuture<Void> getFuture() ?

    @Override
    default void onStart() {
        // no-op
    }

    @Override
    default boolean onStop(long timeout, TimeUnit unit) {
        return true;
    }

    /**
     * Represents a disposable link between two {@link Block} instances.
     *
     * Implementations should be thread-safe.
     */
    interface Link extends AutoCloseable {
        /**
         * Disposes of the link (unlinks) the blocks represented by this link. This operation is idempotent and thread-safe
         * (coordinated with {@link #isClosed()}).
         */
        @Override
        void close();

        /**
         * @return true if link has been disposed, otherwise false
         */
        boolean isClosed();
    }

    /**
     * A mutable set of options used to configure a link between {@link Blocks}
     */
    class LinkOptions {
        private boolean propagateCompletion = false;
        // there may be other options here in future

        public static LinkOptions create() {
            return new LinkOptions();
        }

        protected LinkOptions() {
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

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("LinkOptions{");
            sb.append("propagateCompletion=").append(propagateCompletion);
            sb.append('}');
            return sb.toString();
        }
    }
}

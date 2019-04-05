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

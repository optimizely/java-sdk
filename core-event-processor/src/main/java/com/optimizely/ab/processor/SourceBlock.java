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

import com.optimizely.ab.common.internal.Assert;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a dataflow {@link Block} that is a source of data.
 *
 * This interface and its counterparts, {@link TargetBlock} and {@link ActorBlock}, are inspired by .NET TPL. They
 * provide the basic building blocks to define the in-process message passing for coarse-grained dataflow and
 * parallelism.
 *
 * @param <TOutput> the type of output elements
 */
public interface SourceBlock<TOutput> extends Block {

    /**
     * Links the {@link SourceBlock} to the specified {@link TargetBlock}.
     *
     * @param target the target to connect to this source
     * @return a disposable link between this and target
     */
    default Link linkTo(TargetBlock<? super TOutput> target) {
        return linkTo(target, LinkOptions.create());
    }

    /**
     * Links the {@link SourceBlock} to the specified {@link TargetBlock}.
     *
     * @param target the target to connect to this source
     * @param options configures the link
     * @return a disposable link between this and target
     */
    Link linkTo(TargetBlock<? super TOutput> target, LinkOptions options);

    /**
     * This class provides skeletal implementation for {@link SourceBlock} to interface to minimize the effort required to
     * implement the interface.
     *
     * Maintains link state to {@link TargetBlock} and the associated {@link LinkOptions} and propagates completion signal
     * accordingly.
     *
     * @param <TOutput> the type of output elements; the type of elements accepted by downstream.
     */
    abstract class Base<TOutput> implements SourceBlock<TOutput> {
        // currently support only a single target to be linked; could change in future
        private final AtomicReference<TargetLink<TOutput>> state = new AtomicReference<>();

        @Override
        public Link linkTo(TargetBlock<? super TOutput> target, LinkOptions options) {
            TargetLink<TOutput> link = new TargetLink<>(state, target, options);
            link.create();
            return link;
        }

        @Override
        public void onStart() {
            // no-op
        }

        @Override
        public boolean onStop(long timeout, TimeUnit unit) {
            TargetLink link = state.get();
            if (link != null) {
                return link.onStop(timeout, unit);
            }
            return true;
        }

        /**
         * Safely get a {@link TargetBlock}.
         *
         * @throws IllegalStateException if no target is linked
         */
        @Nonnull
        protected TargetBlock<? super TOutput> target() {
            TargetLink<? super TOutput> link = this.state.get();
            Assert.state(link != null, "Target has not been linked");
            return link;
        }

        /**
         * Implements a link to a delegate target
         */
        static class TargetLink<TOutput> implements TargetBlock<TOutput>, Link {
            private final AtomicReference<TargetLink<TOutput>> state;
            private TargetBlock<? super TOutput> target;
            private LinkOptions options;

            TargetLink(
                AtomicReference<TargetLink<TOutput>> state,
                TargetBlock<? super TOutput> target,
                LinkOptions options
            ) {
                this.state = Assert.notNull(state, "state");
                this.target = Assert.notNull(target, "target");
                this.options = (options != null) ? options : LinkOptions.create();
            }

            /**
             * Sets/replaces the current link state with this.
             */
            public void create() {
                // set as current, replacing if one exists
                TargetLink existing = this.state.getAndSet(this);
                if (existing != null) {
                    existing.close();
                }
            }

            protected LinkOptions getOptions() {
                return options;
            }

            @Override
            public void post(@Nonnull TOutput element) {
                Assert.state(target != null, "Link is closed");
                target.post(element);
            }

            @Override
            public void postBatch(@Nonnull Collection<? extends TOutput> elements) {
                Assert.state(target != null, "Link is closed");
                target.postBatch(elements);
            }

            @Override
            public void postNullable(TOutput element) {
                Assert.state(target != null, "Link is closed");
                target.postNullable(element);
            }

            @Override
            public void onStart() {
                Assert.state(target != null, "Link is closed");
                target.onStart();
            }

            @Override
            public boolean onStop(long timeout, TimeUnit unit) {
                Assert.state(target != null, "Link is closed");
                close();
                if (options.getPropagateCompletion()) {
                    return target.onStop(timeout, unit);
                }
                return true;
            }

            @Override
            public void close() {
                state.compareAndSet(this, null);
                target = null;
            }

            @Override
            public boolean isClosed() {
                return (this != state.get());
            }
        }
    }
}

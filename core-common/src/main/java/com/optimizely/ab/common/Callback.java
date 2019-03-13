/**
 * Copyright 2019 Optimizely
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.common;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface Callback<E> {
    /**
     * Called when the action completed successfully
     * @param context
     */
    void success(E context);

    /**
     * Called when the action could not be completed
     */
    void failure(E context, @Nonnull Throwable throwable);


    /**
     * Convenience method to reify a {@link Callback} instance from separate success and failure handler functions.
     *
     * @param onSuccess function invoked on success
     * @param onFailure function invoked on failure
     * @param <E>       the type of object passed to callback
     * @return a reified {@link Callback} instance
     */
    static <E> Callback<E> from(
        final Consumer<E> onSuccess,
        final BiConsumer<E, Throwable> onFailure
    ) {
        return new Callback<E>() {
            @Override
            public void success(E context) {
                if (onSuccess != null) {
                    onSuccess.accept(context);
                }
            }

            @Override
            public void failure(E context, @Nonnull Throwable throwable) {
                if (onFailure != null) {
                    onFailure.accept(context, throwable);
                }
            }
        };
    }

    /**
     * Similar to {@link #from(Consumer, BiConsumer)}, however, handlers are executed on the given {@link Executor}
     */
    static <E> Callback<E> from(
        final Consumer<E> onSuccess,
        final BiConsumer<E, Throwable> onFailure,
        final Executor executor
    ) {
        return new Callback<E>() {
            @Override
            public void success(E context) {
                if (onSuccess != null) {
                    executor.execute(() -> onSuccess.accept(context));
                }
            }

            @Override
            public void failure(E context, @Nonnull Throwable throwable) {
                if (onFailure != null) {
                    executor.execute(() -> onFailure.accept(context, throwable));
                }
            }
        };
    }

    /**
     * @see #from(Consumer, BiConsumer)
     */
    static <E> Callback<E> from(final Consumer<E> onSuccess) {
        return from(onSuccess, (BiConsumer<E, Throwable>) null);
    }

    /**
     * @see #from(Consumer, BiConsumer, Executor)
     */
    static <E> Callback<E> from(final Consumer<E> onSuccess, Executor executor) {
        return from(onSuccess, null, executor);
    }

    /**
     * Convenience method to reify a {@link Callback} instance from separate success and failure handler functions.
     *
     * @param onSuccess function invoked on success
     * @param onFailure function invoked on failure
     * @param <E>       the type of object passed to callback
     * @return a reified {@link Callback} instance
     */
    static <E> Callback<E> from(
        final Runnable onSuccess,
        final Consumer<Throwable> onFailure
    ) {
        return new Callback<E>() {
            @Override
            public void success(E context) {
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }

            @Override
            public void failure(E context, @Nonnull Throwable throwable) {
                if (onFailure != null) {
                    onFailure.accept(throwable);
                }
            }
        };
    }

    /**
     * Similar to {@link #from(Runnable, Consumer)}, however, handlers are executed on the given {@link Executor}
     */
    static <E> Callback<E> from(
        final Runnable onSuccess,
        final Consumer<Throwable> onFailure,
        final Executor executor
    ) {
        return new Callback<E>() {
            @Override
            public void success(E context) {
                if (onSuccess != null) {
                    executor.execute(onSuccess);
                }
            }

            @Override
            public void failure(E context, @Nonnull Throwable throwable) {
                if (onFailure != null) {
                    executor.execute(() -> onFailure.accept(throwable));
                }
            }
        };
    }

    /**
     * @see #from(Runnable)
     */
    static <E> Callback<E> from(final Runnable onSuccess) {
        return from(onSuccess, (Consumer<Throwable>) null);
    }

    /**
     * @see #from(Runnable, Consumer, Executor)
     */
    static <E> Callback<E> from(final Runnable onSuccess, Executor executor) {
        return from(onSuccess, null, executor);
    }
}

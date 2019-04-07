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
package com.optimizely.ab.common.callback;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A callback that handles successful and unsuccessful completion for a type.
 *
 * @param <E> the type of values received by this callback.
 */
public interface Callback<E> {
    /**
     * Called when the action successfully completed.
     */
    void success(E context);

    /**
     * Called when the action unsuccessfully completed.
     */
    void failure(E context, @Nonnull Throwable throwable);

    /**
     * Convenience method to reify a {@link Callback} instance using anonymous functions.
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
}

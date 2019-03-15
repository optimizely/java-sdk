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
package com.optimizely.ab.common.callback;

import com.optimizely.ab.common.internal.Assert;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

/**
 * A wrapper {@link Callback} that executes a delegate callback using an {@link java.util.concurrent.Executor}.
 * @param <T>
 */
public class AsyncCallback<T> implements Callback<T> {
    private final Callback<T> delegate;
    private final Executor executor;

    public AsyncCallback(Callback<T> delegate, Executor executor) {
        this.delegate = Assert.notNull(delegate, "delegate");
        this.executor = Assert.notNull(executor, "executor");
    }

    @Override
    public void success(final T input) {
        executor.execute(() -> delegate.success(input));
    }

    @Override
    public void failure(final T context, @Nonnull final Throwable throwable) {
        executor.execute(() -> delegate.failure(context, throwable));
    }
}

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
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A thread-safe wrapper {@link Callback} that delegates
 */
public class CallbackHolder<E> implements Callback<E> {
    private final AtomicReference<Callback<? super E>> callback;

    public CallbackHolder() {
        this(NoOpCallback.INSTANCE);
    }

    public CallbackHolder(Callback<? super E> callback) {
        this.callback = new AtomicReference<>(Assert.notNull(callback, "callback"));
    }

    public void set(@Nullable Callback<? super E> callback) {
        if (callback == null) {
            clear();
        } else {
            this.callback.set(callback);
        }
    }

    public void clear() {
        callback.set(NoOpCallback.INSTANCE);
    }

    @Override
    public void success(E context) {
        callback.get().success(context);
    }

    @Override
    public void failure(E context, @Nonnull Throwable throwable) {
        callback.get().failure(context, throwable);
    }
}

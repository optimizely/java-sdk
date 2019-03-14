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

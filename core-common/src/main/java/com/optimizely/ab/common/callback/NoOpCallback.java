package com.optimizely.ab.common.callback;

import javax.annotation.Nonnull;

enum NoOpCallback implements Callback<Object> {
    INSTANCE;

    @Override
    public void success(Object context) {
        // no-op
    }

    @Override
    public void failure(Object context, @Nonnull Throwable throwable) {
        // no-op
    }
}

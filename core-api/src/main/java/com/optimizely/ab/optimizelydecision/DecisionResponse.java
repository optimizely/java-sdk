package com.optimizely.ab.optimizelydecision;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DecisionResponse<T> {
    private T result;
    private DecisionReasons reasons;

    public DecisionResponse(@Nullable T result, @Nonnull DecisionReasons reasons) {
        this.result = result;
        this.reasons = reasons;
    }

    @Nullable
    public T getResult() {
        return result;
    }

    @Nonnull
    public DecisionReasons getReasons() {
        return reasons;
    }
}

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

    public static <E> DecisionResponse responseNoReasons(@Nullable E result) {
        return new DecisionResponse(result, DefaultDecisionReasons.newInstance());
    }

    public static DecisionResponse nullNoReasons() {
        return new DecisionResponse(null, DefaultDecisionReasons.newInstance());
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

package com.optimizely.ab;

import javax.annotation.Nonnull;

public class OptimizelyForcedDecision {
    private String variationKey;

    public OptimizelyForcedDecision(@Nonnull String variationKey) {
        this.variationKey = variationKey;
    }

    public String getVariationKey() {
        return variationKey;
    }
}

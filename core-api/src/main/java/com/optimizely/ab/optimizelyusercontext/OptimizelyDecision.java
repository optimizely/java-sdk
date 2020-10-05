package com.optimizely.ab.optimizelyusercontext;

import com.optimizely.ab.optimizelyjson.OptimizelyJSON;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OptimizelyDecision {
    @Nullable
    private final String variationKey;

    private final boolean enabled;

    @Nonnull
    private final OptimizelyJSON variables;

    @Nullable
    private final String ruleKey;

    @Nonnull
    private final String flagKey;

    @Nullable
    private final OptimizelyUserContext userContext;

    @Nonnull
    private String[] reasons;


    public OptimizelyDecision(@Nullable String variationKey,
                              boolean enabled,
                              @Nonnull OptimizelyJSON variables,
                              @Nullable String ruleKey,
                              @Nonnull String flagKey,
                              @Nullable OptimizelyUserContext userContext,
                              @Nonnull String[] reasons) {
        this.variationKey = variationKey;
        this.enabled = enabled;
        this.variables = variables;
        this.ruleKey = ruleKey;
        this.flagKey = flagKey;
        this.userContext = userContext;
        this.reasons = reasons;
    }

    @Nullable
    public String getVariationKey() {
        return variationKey;
    }

    public boolean getEnabled() {
        return enabled;
    }

    @Nonnull
    public OptimizelyJSON getVariables() {
        return variables;
    }

    @Nullable
    public String getRuleKey() {
        return ruleKey;
    }

    @Nonnull
    public String getFlagKey() {
        return flagKey;
    }

    @Nullable
    public OptimizelyUserContext getUserContext() {
        return userContext;
    }

    @Nonnull
    public String[] getReasons() {
        return reasons;
    }

    public static OptimizelyDecision createErrorDecision(@Nonnull String key,
                                                         @Nonnull OptimizelyUserContext user,
                                                         String error) {
        return new OptimizelyDecision(null,
            false,
            null,
            null,
            key,
            user,
            new String[]{error});
    }

    public boolean hasFailed() {
        return variationKey == null;
    }

}

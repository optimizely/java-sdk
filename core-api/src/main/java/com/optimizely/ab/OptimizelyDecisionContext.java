package com.optimizely.ab;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OptimizelyDecisionContext {
    public static final String OPTI_NULL_RULE_KEY = "$opt-null-rule-key";

    private String flagKey;
    private String ruleKey;

    public OptimizelyDecisionContext(@Nonnull String flagKey, @Nullable String ruleKey) {
        this.flagKey = flagKey;
        this.ruleKey = ruleKey;
    }

    public String getFlagKey() {
        return flagKey;
    }

    public String getRuleKey() {
        return ruleKey != null ? ruleKey : OPTI_NULL_RULE_KEY;
    }

    public String getKey() {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(flagKey);
        keyBuilder.append(getRuleKey());
        return keyBuilder.toString();
    }
}

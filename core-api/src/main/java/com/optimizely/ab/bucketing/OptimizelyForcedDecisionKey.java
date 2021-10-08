package com.optimizely.ab.bucketing;

import javax.annotation.Nonnull;

public class OptimizelyForcedDecisionKey {
    private String flagKey;
    private String ruleKey;

    public OptimizelyForcedDecisionKey(@Nonnull String flagKey, String ruleKey) {
        this.flagKey = flagKey;
        this.ruleKey = ruleKey;
    }

    public String getFlagKey() { return flagKey; }

    public String getRuleKey() { return ruleKey; }

    public String toString() {
        StringBuilder keyString = new StringBuilder();
        keyString.append(getFlagKey().hashCode());
        keyString.append(getRuleKey() != null ? getRuleKey().hashCode() : "null");
        return keyString.toString();
    }
}

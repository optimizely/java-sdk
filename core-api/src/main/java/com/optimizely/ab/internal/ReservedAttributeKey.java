package com.optimizely.ab.internal;

public enum ReservedAttributeKey {
    BOT_FILTERING_ATTRIBUTE("$opt_bot_filtering"),
    USER_AGENT_ATTRIBUTE("$opt_user_agent"),
    BUCKETING_ATTRIBUTE("$opt_bucketing_id");

    private final String key;

    ReservedAttributeKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }
}

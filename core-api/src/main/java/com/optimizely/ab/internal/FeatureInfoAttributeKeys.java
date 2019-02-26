package com.optimizely.ab.internal;

public enum FeatureInfoAttributeKeys {
    ENABLED("enabled"),
    SOURCE("source"),
    EVENT("event");

    private final String key;

    FeatureInfoAttributeKeys(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }
}

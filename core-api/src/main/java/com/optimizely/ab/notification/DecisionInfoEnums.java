package com.optimizely.ab.notification;

public class DecisionInfoEnums {
    public enum IsFeatureEnabledDecisionInfo {
        FEATURE_KEY("feature_key"),
        FEATURE_ENABLED("feature_enabled"),
        SOURCE("source");

        private final String key;

        IsFeatureEnabledDecisionInfo(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    public enum GetFeatureVariableDecisionInfo {
        FEATURE_KEY("feature_key"),
        FEATURE_ENABLED("feature_enabled"),
        VARIABLE_KEY("variable_key"),
        VARIABLE_TYPE("variable_type"),
        VARIABLE_VALUE("variable_value"),
        SOURCE("source");

        private final String key;

        GetFeatureVariableDecisionInfo(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    public enum ActivateVariationDecisionInfo {
        EXPERIMENT_KEY("experiment_key"),
        VARIATION_KEY("variation_key");

        private final String key;

        ActivateVariationDecisionInfo(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }
}

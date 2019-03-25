/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.notification;


import com.optimizely.ab.bucketing.FeatureDecision;
import com.optimizely.ab.config.FeatureVariable;

import java.util.HashMap;
import java.util.Map;

public class DecisionNotification {
    protected String type;
    protected String userId;
    protected Map<String, ?> attributes;
    protected Map<String, ?> decisionInfo;

    protected DecisionNotification() {
    }

    protected DecisionNotification(String type,
                                   String userId,
                                   Map<String, ?> attributes,
                                   Map<String, ?> decisionInfo) {
        this.type = type;
        this.userId = userId;
        this.attributes = attributes;
        this.decisionInfo = decisionInfo;
    }

    public String getType() {
        return type;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public Map<String, ?> getDecisionInfo() {
        return decisionInfo;
    }

    public static FeatureVariableDecisionNotificationBuilder newFeatureVariableBuilder() {
        return new FeatureVariableDecisionNotificationBuilder();
    }

    private static class BaseDecisionNotificationBuilder {
        protected String userId;
        protected String type;
        protected Map<String, ?> attributes;
        protected Map<String, Object> decisionInfo;

        public DecisionNotification build() {
            if (attributes == null) {
                attributes = new HashMap<>();
            }

            return new DecisionNotification(
                type,
                userId,
                attributes,
                decisionInfo);
        }
    }

    public static class FeatureVariableDecisionNotificationBuilder extends BaseDecisionNotificationBuilder {

        public static String FEATURE_KEY = "feature_key";
        public static String FEATURE_ENABLED = "feature_enabled";
        public static String SOURCE = "source";
        public static String SOURCE_EXPERIMENT_KEY = "source_experiment_key";
        public static String SOURCE_VARIATION_KEY = "source_variation_key";
        public static String VARIABLE_KEY = "variable_key";
        public static String VARIABLE_TYPE = "variable_type";
        public static String VARIABLE_VALUE = "variable_value";

        private String featureKey;
        private Boolean featureEnabled;
        private FeatureDecision featureDecision;
        private String variableKey;
        private FeatureVariable.VariableType variableType;
        private Object variableValue;

        protected FeatureVariableDecisionNotificationBuilder() {
        }

        public FeatureVariableDecisionNotificationBuilder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public FeatureVariableDecisionNotificationBuilder withAttributes(Map<String, ?> attributes) {
            this.attributes = attributes;
            return this;
        }

        public FeatureVariableDecisionNotificationBuilder withFeatureKey(String featureKey) {
            this.featureKey = featureKey;
            return this;
        }

        public FeatureVariableDecisionNotificationBuilder withFeatureEnabled(Boolean featureEnabled) {
            this.featureEnabled = featureEnabled;
            return this;
        }

        public FeatureVariableDecisionNotificationBuilder withFeatureDecision(FeatureDecision featureDecision) {
            this.featureDecision = featureDecision;
            return this;
        }

        public FeatureVariableDecisionNotificationBuilder withVariableKey(String variableKey) {
            this.variableKey = variableKey;
            return this;
        }

        public FeatureVariableDecisionNotificationBuilder withVariableType(FeatureVariable.VariableType variableType) {
            this.variableType = variableType;
            return this;
        }

        public FeatureVariableDecisionNotificationBuilder withVariableValue(Object variableValue) {
            this.variableValue = variableValue;
            return this;
        }

        public DecisionNotification build() {
            decisionInfo = new HashMap<>();
            decisionInfo.put(FEATURE_KEY, featureKey);
            decisionInfo.put(FEATURE_ENABLED, featureEnabled);
            decisionInfo.put(VARIABLE_KEY, variableKey);
            decisionInfo.put(VARIABLE_TYPE, variableType);
            decisionInfo.put(VARIABLE_VALUE, variableValue);
            if (featureDecision.decisionSource != null && featureDecision.decisionSource.equals(FeatureDecision.DecisionSource.EXPERIMENT)) {
                decisionInfo.put(SOURCE_EXPERIMENT_KEY, featureDecision.experiment.getKey());
                decisionInfo.put(SOURCE_VARIATION_KEY, featureDecision.variation.getKey());
                decisionInfo.put(SOURCE, featureDecision.decisionSource);
            } else {
                decisionInfo.put(SOURCE_EXPERIMENT_KEY, null);
                decisionInfo.put(SOURCE_VARIATION_KEY, null);
                decisionInfo.put(SOURCE, FeatureDecision.DecisionSource.ROLLOUT);
            }

            this.type = NotificationCenter.DecisionNotificationType.FEATURE_VARIABLE.toString();

            return super.build();
        }
    }
}

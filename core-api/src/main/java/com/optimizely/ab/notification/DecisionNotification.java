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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class DecisionNotification {
    protected String type;
    protected String userId;
    protected Map<String, ?> attributes;
    protected Map<String, ?> decisionInfo;

    protected DecisionNotification() {
    }

    protected DecisionNotification(@Nonnull String type,
                                   @Nonnull String userId,
                                   @Nullable Map<String, ?> attributes,
                                   @Nonnull Map<String, ?> decisionInfo) {
        this.type = type;
        this.userId = userId;
        if (attributes == null) {
            attributes = new HashMap<>();
        }
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

    public static class FeatureVariableDecisionNotificationBuilder {

        public static final String FEATURE_KEY = "feature_key";
        public static final String FEATURE_ENABLED = "feature_enabled";
        public static final String SOURCE = "source";
        public static final String SOURCE_INFO = "source_info";
        public static final String EXPERIMENT_KEY = "experiment_key";
        public static final String VARIATION_KEY = "variation_key";
        public static final String VARIABLE_KEY = "variable_key";
        public static final String VARIABLE_TYPE = "variable_type";
        public static final String VARIABLE_VALUE = "variable_value";

        private String featureKey;
        private Boolean featureEnabled;
        private FeatureDecision featureDecision;
        private String variableKey;
        private FeatureVariable.VariableType variableType;
        private Object variableValue;
        private String userId;
        private Map<String, ?> attributes;
        private Map<String, Object> decisionInfo;

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

        public FeatureVariableDecisionNotificationBuilder withFeatureEnabled(boolean featureEnabled) {
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

            Map<String, String> sourceInfo = new HashMap<>();
            if (featureDecision != null && featureDecision.decisionSource != null && FeatureDecision.DecisionSource.FEATURE_TEST.equals(featureDecision.decisionSource)) {
                sourceInfo.put(EXPERIMENT_KEY, featureDecision.experiment.getKey());
                sourceInfo.put(VARIATION_KEY, featureDecision.variation.getKey());
                decisionInfo.put(SOURCE, featureDecision.decisionSource);
            } else {
                decisionInfo.put(SOURCE, FeatureDecision.DecisionSource.ROLLOUT);
            }
            decisionInfo.put(SOURCE_INFO, sourceInfo);

            return new DecisionNotification(
                NotificationCenter.DecisionNotificationType.FEATURE_VARIABLE.toString(),
                userId,
                attributes,
                decisionInfo);
        }
    }
}

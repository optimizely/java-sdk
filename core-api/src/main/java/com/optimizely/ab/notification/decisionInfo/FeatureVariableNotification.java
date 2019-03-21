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

package com.optimizely.ab.notification.decisionInfo;

import com.optimizely.ab.bucketing.FeatureDecision;
import com.optimizely.ab.config.FeatureVariable;
import com.optimizely.ab.notification.NotificationCenter;

import java.util.HashMap;
import java.util.Map;

import static com.optimizely.ab.notification.decisionInfo.DecisionInfoMapConstants.FeatureVariableDecisionBuilder.*;
import static com.optimizely.ab.notification.decisionInfo.DecisionInfoMapConstants.FeatureVariableDecisionBuilder.SOURCE;

public class FeatureVariableNotification extends DecisionNotification {

    public static Builder builder(NotificationCenter notificationCenter) {
        return new Builder(notificationCenter);
    }

    public static class Builder {
        private String userId;
        private Map<String, ?> attributes;
        private Map<String, Object> decisionInfo;
        private NotificationCenter notificationCenter;
        private String featureKey;
        private Boolean featureEnabled;
        private FeatureDecision featureDecision;
        private String variableKey;
        private FeatureVariable.VariableType variableType;
        private Object variableValue;

        public Builder(NotificationCenter notificationCenter) {
            this.notificationCenter = notificationCenter;
        }

        public Builder withFeatureKey(String featureKey) {
            this.featureKey = featureKey;
            return this;
        }

        public Builder withFeatureEnabled(Boolean featureEnabled) {
            this.featureEnabled = featureEnabled;
            return this;
        }

        public Builder withFeatureDecision(FeatureDecision featureDecision) {
            this.featureDecision = featureDecision;
            return this;
        }

        public Builder withVariableKey(String variableKey) {
            this.variableKey = variableKey;
            return this;
        }

        public Builder withVariableType(FeatureVariable.VariableType variableType) {
            this.variableType = variableType;
            return this;
        }

        public Builder withVariableValue(Object variableValue) {
            this.variableValue = variableValue;
            return this;
        }

        public Builder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder withAttributes(Map<String, ?> attributes) {
            this.attributes = attributes;
            return this;
        }

        public DecisionNotification build() {

            if (attributes == null) {
                attributes = new HashMap<>();
            }

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

            return new DecisionNotification(notificationCenter,
                NotificationCenter.DecisionNotificationType.FEATURE_VARIABLE.toString(),
                userId,
                attributes,
                decisionInfo);
        }
    }
}

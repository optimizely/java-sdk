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


import com.optimizely.ab.OptimizelyRuntimeException;
import com.optimizely.ab.bucketing.FeatureDecision;
import com.optimizely.ab.config.Variation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DecisionNotification encapsulates the arguments and responses when using the following methods:
 *
 * activate {@link com.optimizely.ab.Optimizely#activate}
 * getEnabledFeatures {@link com.optimizely.ab.Optimizely#getEnabledFeatures}
 * getFeatureVariableBoolean {@link com.optimizely.ab.Optimizely#getFeatureVariableBoolean}
 * getFeatureVariableDouble {@link com.optimizely.ab.Optimizely#getFeatureVariableDouble}
 * getFeatureVariableInteger {@link com.optimizely.ab.Optimizely#getFeatureVariableInteger}
 * getFeatureVariableString {@link com.optimizely.ab.Optimizely#getFeatureVariableString}
 * getVariation {@link com.optimizely.ab.Optimizely#getVariation}
 * isFeatureEnabled {@link com.optimizely.ab.Optimizely#isFeatureEnabled}
 *
 * @see <a href="https://docs.developers.optimizely.com/full-stack/docs/register-notification-listeners">Notification Listeners</a>
 */
public final class DecisionNotification {
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DecisionNotification{");
        sb.append("type='").append(type).append('\'');
        sb.append(", userId='").append(userId).append('\'');
        sb.append(", attributes=").append(attributes);
        sb.append(", decisionInfo=").append(decisionInfo);
        sb.append('}');
        return sb.toString();
    }

    public static ExperimentDecisionNotificationBuilder newExperimentDecisionNotificationBuilder() {
        return new ExperimentDecisionNotificationBuilder();
    }

    public static class ExperimentDecisionNotificationBuilder {
        public final static String EXPERIMENT_KEY = "experimentKey";
        public final static String VARIATION_KEY = "variationKey";

        private String type;
        private String experimentKey;
        private Variation variation;
        private String userId;
        private Map<String, ?> attributes;
        private Map<String, Object> decisionInfo;

        public ExperimentDecisionNotificationBuilder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public ExperimentDecisionNotificationBuilder withAttributes(Map<String, ?> attributes) {
            this.attributes = attributes;
            return this;
        }

        public ExperimentDecisionNotificationBuilder withExperimentKey(String experimentKey) {
            this.experimentKey = experimentKey;
            return this;
        }

        public ExperimentDecisionNotificationBuilder withType(String type) {
            this.type = type;
            return this;
        }

        public ExperimentDecisionNotificationBuilder withVariation(Variation variation) {
            this.variation = variation;
            return this;
        }

        public DecisionNotification build() {
            if (type == null) {
                throw new OptimizelyRuntimeException("type not set");
            }

            if (experimentKey == null) {
                throw new OptimizelyRuntimeException("experimentKey not set");
            }

            decisionInfo = new HashMap<>();
            decisionInfo.put(EXPERIMENT_KEY, experimentKey);
            decisionInfo.put(VARIATION_KEY, variation != null ? variation.getKey() : null);

            return new DecisionNotification(
                type,
                userId,
                attributes,
                decisionInfo);
        }
    }

    public static FeatureDecisionNotificationBuilder newFeatureDecisionNotificationBuilder() {
        return new FeatureDecisionNotificationBuilder();
    }

    public static class FeatureDecisionNotificationBuilder {
        public final static String FEATURE_KEY = "featureKey";
        public final static String FEATURE_ENABLED = "featureEnabled";
        public final static String SOURCE = "source";
        public final static String SOURCE_INFO = "sourceInfo";

        private String featureKey;
        private Boolean featureEnabled;
        private SourceInfo sourceInfo;
        private FeatureDecision.DecisionSource source;
        private String userId;
        private Map<String, ?> attributes;
        private Map<String, Object> decisionInfo;

        public FeatureDecisionNotificationBuilder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public FeatureDecisionNotificationBuilder withAttributes(Map<String, ?> attributes) {
            this.attributes = attributes;
            return this;
        }

        public FeatureDecisionNotificationBuilder withSourceInfo(SourceInfo sourceInfo) {
            this.sourceInfo = sourceInfo;
            return this;
        }

        public FeatureDecisionNotificationBuilder withSource(FeatureDecision.DecisionSource source) {
            this.source = source;
            return this;
        }

        public FeatureDecisionNotificationBuilder withFeatureKey(String featureKey) {
            this.featureKey = featureKey;
            return this;
        }

        public FeatureDecisionNotificationBuilder withFeatureEnabled(Boolean featureEnabled) {
            this.featureEnabled = featureEnabled;
            return this;
        }

        public DecisionNotification build() {
            if (source == null) {
                throw new OptimizelyRuntimeException("source not set");
            }

            if (featureKey == null) {
                throw new OptimizelyRuntimeException("featureKey not set");
            }

            if (featureEnabled == null) {
                throw new OptimizelyRuntimeException("featureEnabled not set");
            }

            decisionInfo = new HashMap<>();
            decisionInfo.put(FEATURE_KEY, featureKey);
            decisionInfo.put(FEATURE_ENABLED, featureEnabled);
            decisionInfo.put(SOURCE, source.toString());
            decisionInfo.put(SOURCE_INFO, sourceInfo.get());

            return new DecisionNotification(
                NotificationCenter.DecisionNotificationType.FEATURE.toString(),
                userId,
                attributes,
                decisionInfo);
        }
    }

    public static FeatureVariableDecisionNotificationBuilder newFeatureVariableDecisionNotificationBuilder() {
        return new FeatureVariableDecisionNotificationBuilder();
    }

    public static class FeatureVariableDecisionNotificationBuilder {

        public static final String FEATURE_KEY = "featureKey";
        public static final String FEATURE_ENABLED = "featureEnabled";
        public static final String SOURCE = "source";
        public static final String SOURCE_INFO = "sourceInfo";
        public static final String VARIABLE_KEY = "variableKey";
        public static final String VARIABLE_TYPE = "variableType";
        public static final String VARIABLE_VALUE = "variableValue";
        public static final String VARIABLE_VALUES = "variableValues";

        private NotificationCenter.DecisionNotificationType notificationType;
        private String featureKey;
        private Boolean featureEnabled;
        private FeatureDecision featureDecision;
        private String variableKey;
        private String variableType;
        private Object variableValue;
        private Object variableValues;
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

        public FeatureVariableDecisionNotificationBuilder withVariableType(String variableType) {
            this.variableType = variableType;
            return this;
        }

        public FeatureVariableDecisionNotificationBuilder withVariableValue(Object variableValue) {
            this.variableValue = variableValue;
            return this;
        }

        public FeatureVariableDecisionNotificationBuilder withVariableValues(Object variableValues) {
            this.variableValues = variableValues;
            return this;
        }

        public DecisionNotification build() {
            if (featureKey == null) {
                throw new OptimizelyRuntimeException("featureKey not set");
            }

            if (featureEnabled == null) {
                throw new OptimizelyRuntimeException("featureEnabled not set");
            }


            decisionInfo = new HashMap<>();
            decisionInfo.put(FEATURE_KEY, featureKey);
            decisionInfo.put(FEATURE_ENABLED, featureEnabled);

            if (variableValues != null) {
                notificationType = NotificationCenter.DecisionNotificationType.ALL_FEATURE_VARIABLES;
                decisionInfo.put(VARIABLE_VALUES, variableValues);
            } else {
                notificationType = NotificationCenter.DecisionNotificationType.FEATURE_VARIABLE;

                if (variableKey == null) {
                    throw new OptimizelyRuntimeException("variableKey not set");
                }

                if (variableType == null) {
                    throw new OptimizelyRuntimeException("variableType not set");
                }

                decisionInfo.put(VARIABLE_KEY, variableKey);
                decisionInfo.put(VARIABLE_TYPE, variableType.toString());
                decisionInfo.put(VARIABLE_VALUE, variableValue);
            }

            SourceInfo sourceInfo = new RolloutSourceInfo();

            if (featureDecision != null && FeatureDecision.DecisionSource.FEATURE_TEST.equals(featureDecision.decisionSource)) {
                sourceInfo = new FeatureTestSourceInfo(featureDecision.experiment.getKey(), featureDecision.variation.getKey());
                decisionInfo.put(SOURCE, featureDecision.decisionSource.toString());
            } else {
                decisionInfo.put(SOURCE, FeatureDecision.DecisionSource.ROLLOUT.toString());
            }
            decisionInfo.put(SOURCE_INFO, sourceInfo.get());

            return new DecisionNotification(
                notificationType.toString(),
                userId,
                attributes,
                decisionInfo);
        }
    }

    public static FlagDecisionNotificationBuilder newFlagDecisionNotificationBuilder() {
        return new FlagDecisionNotificationBuilder();
    }

    public static class FlagDecisionNotificationBuilder {
        public final static String FLAG_KEY = "flagKey";
        public final static String ENABLED = "enabled";
        public final static String VARIABLES = "variables";
        public final static String VARIATION_KEY = "variationKey";
        public final static String RULE_KEY = "ruleKey";
        public final static String REASONS = "reasons";
        public final static String DECISION_EVENT_DISPATCHED = "decisionEventDispatched";

        private String flagKey;
        private Boolean enabled;
        private Object variables;
        private String userId;
        private Map<String, ?> attributes;
        private String variationKey;
        private String ruleKey;
        private List<String> reasons;
        private Boolean decisionEventDispatched;

        private Map<String, Object> decisionInfo;

        public FlagDecisionNotificationBuilder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public FlagDecisionNotificationBuilder withAttributes(Map<String, ?> attributes) {
            this.attributes = attributes;
            return this;
        }

        public FlagDecisionNotificationBuilder withFlagKey(String flagKey) {
            this.flagKey = flagKey;
            return this;
        }

        public FlagDecisionNotificationBuilder withEnabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public FlagDecisionNotificationBuilder withVariables(Object variables) {
            this.variables = variables;
            return this;
        }

        public FlagDecisionNotificationBuilder withVariationKey(String key) {
            this.variationKey = key;
            return this;
        }

        public FlagDecisionNotificationBuilder withRuleKey(String key) {
            this.ruleKey = key;
            return this;
        }

        public FlagDecisionNotificationBuilder withReasons(List<String> reasons) {
            this.reasons = reasons;
            return this;
        }

        public FlagDecisionNotificationBuilder withDecisionEventDispatched(Boolean dispatched) {
            this.decisionEventDispatched = dispatched;
            return this;
        }

        public DecisionNotification build() {
            if (flagKey == null) {
                throw new OptimizelyRuntimeException("flagKey not set");
            }

            if (enabled == null) {
                throw new OptimizelyRuntimeException("enabled not set");
            }

            decisionInfo = new HashMap<>();
            decisionInfo.put(FLAG_KEY, flagKey);
            decisionInfo.put(ENABLED, enabled);
            decisionInfo.put(VARIABLES, variables);
            decisionInfo.put(VARIATION_KEY, variationKey);
            decisionInfo.put(RULE_KEY, ruleKey);
            decisionInfo.put(REASONS, reasons);
            decisionInfo.put(DECISION_EVENT_DISPATCHED, decisionEventDispatched);

            return new DecisionNotification(
                NotificationCenter.DecisionNotificationType.FLAG.toString(),
                userId,
                attributes,
                decisionInfo);
        }
    }

}

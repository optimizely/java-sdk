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

    public static FeatureDecisionNotificationBuilder newFeatureDecisionNotificationBuilder() {
        return new FeatureDecisionNotificationBuilder();
    }

    public static class FeatureDecisionNotificationBuilder {
        public final static String FEATURE_KEY = "feature_key";
        public final static String FEATURE_ENABLED = "feature_enabled";
        public final static String SOURCE = "source";
        public final static String SOURCE_INFO = "source_info";
        public final static String EXPERIMENT_KEY = "experiment_key";
        public final static String VARIATION_KEY = "variation_key";

        private String featureKey;
        private Boolean featureEnabled;
        private String experimentKey;
        private String variationKey;
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

        public FeatureDecisionNotificationBuilder withExperimentKey(String experimentKey) {
            this.experimentKey = experimentKey;
            return this;
        }

        public FeatureDecisionNotificationBuilder withVariationKey(String variationKey) {
            this.variationKey = variationKey;
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
            decisionInfo = new HashMap<>();
            decisionInfo.put(FEATURE_KEY, featureKey);
            decisionInfo.put(FEATURE_ENABLED, featureEnabled);
            decisionInfo.put(SOURCE, source);

            Map<String, String> sourceInfo = new HashMap<>();
            if (source.equals(FeatureDecision.DecisionSource.FEATURE_TEST)) {
                sourceInfo.put(EXPERIMENT_KEY, experimentKey);
                sourceInfo.put(VARIATION_KEY, variationKey);
            }
            decisionInfo.put(SOURCE_INFO, sourceInfo);

            return new DecisionNotification(
                NotificationCenter.DecisionNotificationType.FEATURE.toString(),
                userId,
                attributes,
                decisionInfo);
        }
    }
}
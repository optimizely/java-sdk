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
import com.optimizely.ab.config.Variation;

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

    public static ExperimentDecisionNotificationBuilder newExperimentDecisionNotificationBuilder() {
        return new ExperimentDecisionNotificationBuilder();
    }

    public static class ExperimentDecisionNotificationBuilder {
        public final static String EXPERIMENT_KEY = "experiment_key";
        public final static String VARIATION_KEY = "variation_key";

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

        public ExperimentDecisionNotificationBuilder withVariation(Variation variation) {
            this.variation = variation;
            return this;
        }

        public DecisionNotification build() {
            decisionInfo = new HashMap<>();
            decisionInfo.put(EXPERIMENT_KEY, experimentKey);
            decisionInfo.put(VARIATION_KEY, variation != null ? variation.getKey() : null);

            return new DecisionNotification(
                NotificationCenter.DecisionNotificationType.EXPERIMENT.toString(),
                userId,
                attributes,
                decisionInfo);
        }
    }
}
/**
 *
 *    Copyright 2016-2020, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.event.internal;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.optimizely.ab.bucketing.FeatureDecision;
import com.optimizely.ab.config.ExperimentCore;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.internal.payload.DecisionMetadata;
import com.optimizely.ab.internal.EventTagUtils;

public class UserEventFactory {
    private static final Logger logger = LoggerFactory.getLogger(UserEventFactory.class);

    public static ImpressionEvent createImpressionEvent(@Nonnull ProjectConfig projectConfig,
                                                        @Nullable ExperimentCore activatedExperiment,
                                                        @Nullable Variation variation,
                                                        @Nonnull String userId,
                                                        @Nonnull Map<String, ?> attributes,
                                                        @Nonnull String flagKey,
                                                        @Nonnull String ruleType,
                                                        @Nonnull boolean enabled,
                                                        @Nullable String cmabUUID) {

        if ((FeatureDecision.DecisionSource.ROLLOUT.toString().equals(ruleType)  || variation == null) && !projectConfig.getSendFlagDecisions())
        {
            return null;
        }

        String variationKey = "";
        String variationID = "";
        String layerID = null;
        String experimentId = null;
        String experimentKey = "";

        if (variation != null) {
            variationKey = variation.getKey();
            variationID = variation.getId();
            layerID = activatedExperiment != null ? activatedExperiment.getLayerId() : "";
            experimentId = activatedExperiment != null ? activatedExperiment.getId() : "";
            experimentKey = activatedExperiment != null ? activatedExperiment.getKey() : "";
        }

        UserContext userContext = new UserContext.Builder()
            .withUserId(userId)
            .withAttributes(attributes)
            .withProjectConfig(projectConfig)
            .build();

        DecisionMetadata.Builder metadataBuilder = new DecisionMetadata.Builder()
            .setFlagKey(flagKey)
            .setRuleKey(experimentKey)
            .setRuleType(ruleType)
            .setVariationKey(variationKey)
            .setEnabled(enabled);
        
        if (cmabUUID != null) {
            metadataBuilder.setCmabUUID(cmabUUID);
        }
        
        DecisionMetadata metadata = metadataBuilder.build();

        return new ImpressionEvent.Builder()
            .withUserContext(userContext)
            .withLayerId(layerID)
            .withExperimentId(experimentId)
            .withExperimentKey(experimentKey)
            .withVariationId(variationID)
            .withVariationKey(variationKey)
            .withMetadata(metadata)
            .build();
    }

    public static ConversionEvent createConversionEvent(@Nonnull ProjectConfig projectConfig,
                                                        @Nonnull String userId,
                                                        @Nonnull String eventId, // Why is this not used?
                                                        @Nonnull String eventName,
                                                        @Nonnull Map<String, ?> attributes,
                                                        @Nonnull Map<String, ?> eventTags) {


        UserContext userContext = new UserContext.Builder()
            .withUserId(userId)
            .withAttributes(attributes)
            .withProjectConfig(projectConfig)
            .build();

        return new ConversionEvent.Builder()
            .withUserContext(userContext)
            .withEventId(eventId)
            .withEventKey(eventName)
            .withRevenue(EventTagUtils.getRevenueValue(eventTags))
            .withValue(EventTagUtils.getNumericValue(eventTags))
            .withTags(eventTags)
            .build();
    }
}

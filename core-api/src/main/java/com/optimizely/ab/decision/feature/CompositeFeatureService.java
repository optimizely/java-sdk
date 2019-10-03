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
package com.optimizely.ab.decision.feature;

import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.decision.entities.FeatureDecision;
import com.optimizely.ab.decision.feature.service.FeatureExperimentService;
import com.optimizely.ab.decision.feature.service.FeatureRolloutService;
import com.optimizely.ab.event.internal.UserContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CompositeFeatureService contains the information needed to be able to make a decision for a given featureFlag
 */
public class CompositeFeatureService implements FeatureDecisionService {
    private final UserProfileService userProfileService;
    private transient ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping;

    /**
     * Initialize composite experiment service for decision service
     *
     * @param userProfileService     UserProfileService implementation for storing user info.
     * @param forcedVariationMapping Forced Variation for user if exists
     */
    public CompositeFeatureService(@Nullable UserProfileService userProfileService,
                                      @Nullable ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping) {
        this.userProfileService = userProfileService;
        this.forcedVariationMapping = forcedVariationMapping;
    }

    @Override
    public FeatureDecision getDecision(@Nonnull FeatureFlag featureFlag, @Nonnull UserContext userContext) {
        FeatureDecision featureDecision;
        // loop through different feature decision services until we get a decision
        for (FeatureDecisionService featureDecisionService : getFeatureServices()) {
            featureDecision = featureDecisionService.getDecision(featureFlag, userContext);
            if (featureDecision != null)
                return featureDecision;
        }

        return new FeatureDecision(null,
            null,
            FeatureDecision.DecisionSource.FEATURE_TEST);

    }

    /**
     * Get Feature Services
     *
     * @return List of {@link FeatureDecisionService}
     */
    private List<FeatureDecisionService> getFeatureServices() {
        return Arrays.asList(
            new FeatureExperimentService(userProfileService, forcedVariationMapping),
            new FeatureRolloutService()
        );
    }
}


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
package com.optimizely.ab.decision.feature.service;

import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.decision.entities.FeatureDecision;
import com.optimizely.ab.decision.experiment.CompositeExperimentService;
import com.optimizely.ab.decision.experiment.ExperimentDecisionService;
import com.optimizely.ab.decision.feature.FeatureDecisionService;
import com.optimizely.ab.event.internal.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

public class FeatureExperimentService implements FeatureDecisionService {
    private static final Logger logger = LoggerFactory.getLogger(FeatureExperimentService.class);
    private final UserProfileService userProfileService;
    private transient ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping;


    /**
     * Initialize FeatureExperimentService
     *
     * @param userProfileService UserProfileService implementation for storing user info.
     * @param forcedVariationMapping Forced Variation for user if exists
     */
    public FeatureExperimentService(UserProfileService userProfileService, ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping) {
        this.userProfileService = userProfileService;
        this.forcedVariationMapping = forcedVariationMapping;
    }

    @Override
    public FeatureDecision getDecision(@Nonnull FeatureFlag featureFlag, @Nonnull UserContext userContext) {
        if (!featureFlag.getExperimentIds().isEmpty()) {
            ExperimentDecisionService experimentDecisionService = new CompositeExperimentService(userProfileService, forcedVariationMapping);

            for (String experimentId : featureFlag.getExperimentIds()) {
                Experiment experiment = userContext.getProjectConfig().getExperimentIdMapping().get(experimentId);
                ExperimentDecision experimentDecision = experimentDecisionService.getDecision(experiment, userContext);
                if (experimentDecision.variation != null) {
                    return new FeatureDecision(experiment,
                        experimentDecision.variation,
                        FeatureDecision.DecisionSource.FEATURE_TEST);
                }
            }
        } else {
            logger.info("The feature flag \"{}\" is not used in any experiments.", featureFlag.getKey());
        }

        return null;
    }
}


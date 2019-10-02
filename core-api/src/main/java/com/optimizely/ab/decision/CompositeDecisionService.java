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
package com.optimizely.ab.decision;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.decision.entities.FeatureDecision;
import com.optimizely.ab.decision.feature.CompositeFeatureService;
import com.optimizely.ab.decision.feature.FeatureDecisionService;
import com.optimizely.ab.event.internal.UserContext;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.decision.experiment.ExperimentDecisionService;
import com.optimizely.ab.decision.experiment.CompositeExperimentService;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CompositeDecisionService determines which variation of an experiment the user will be allocated to.
 */
public class CompositeDecisionService implements DecisionService {

    private ExperimentDecisionService experimentDecisionService;
    private FeatureDecisionService featureDecisionService;

    /**
     * Initialize a decision service for the Optimizely client.
     *
     * @param experimentDecisionService ExperimentDecisionService provided from Optimizely Client
     * @param featureDecisionService FeatureDecisionService provided from Optimizely Client
     */
    private CompositeDecisionService(ExperimentDecisionService experimentDecisionService, FeatureDecisionService featureDecisionService) {
        this.experimentDecisionService = experimentDecisionService;
        this.featureDecisionService = featureDecisionService;
    }

    /**
     * Returns a ExperimentDecision for the given user and experiment
     *
     * @param experiment  The Experiment the user will be bucketed into.
     * @param userContext It have user id, attributes and a reference to the current {@link ProjectConfig}
     * @return {@link ExperimentDecision}
     */
    @Override
    public ExperimentDecision getExperimentDecision(@Nonnull Experiment experiment,
                                                    @Nonnull UserContext userContext) {
        return experimentDecisionService.getDecision(experiment, userContext);
    }

    @Override
    public FeatureDecision getFeatureDecision(@Nonnull FeatureFlag featureFlag,
                                              @Nonnull UserContext userContext) {
        return featureDecisionService.getDecision(featureFlag, userContext);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@link CompositeDecisionService} instance builder.
     */
    public static class Builder {
        private UserProfileService userProfileService;
        private transient ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping;

        /**
         * Set User Profile Service
         *
         * @param userProfileService UserProfileService implementation for storing user info.
         * @return {@link Builder}
         */
        public Builder withUserProfileService(UserProfileService userProfileService) {
            this.userProfileService = userProfileService;
            return this;
        }

        /**
         * Set ForcedVariation map
         *
         * @param forcedVariationMapping Forced Variation for user if exists
         * @return {@link Builder}
         */
        public Builder withForcedVariation(ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping) {
            this.forcedVariationMapping = forcedVariationMapping;
            return this;
        }

        /**
         * Create actual object of {@link CompositeDecisionService}
         *
         * @return {@link CompositeDecisionService}
         */
        public CompositeDecisionService build() {
            return new CompositeDecisionService(
                new CompositeExperimentService(userProfileService, forcedVariationMapping),
                new CompositeFeatureService(userProfileService, forcedVariationMapping));
        }
    }
}

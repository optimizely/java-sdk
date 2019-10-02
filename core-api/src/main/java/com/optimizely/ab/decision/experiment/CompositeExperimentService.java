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
package com.optimizely.ab.decision.experiment;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.decision.audience.FullStackAudienceEvaluator;
import com.optimizely.ab.decision.bucketer.MurmurhashBucketer;
import com.optimizely.ab.decision.entities.Reason;
import com.optimizely.ab.internal.ExperimentUtils;
import com.optimizely.ab.event.internal.UserContext;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.decision.entities.DecisionStatus;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.decision.experiment.service.WhitelistingService;
import com.optimizely.ab.decision.experiment.service.ForcedVariationService;
import com.optimizely.ab.decision.experiment.service.ExperimentBucketerService;
import com.optimizely.ab.decision.experiment.service.UserProfileDecisionService;

import java.util.List;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CompositeExperimentService contains the information needed to be able to make a decision for a given experiment
 */
public class CompositeExperimentService implements ExperimentDecisionService {

    private static final Logger logger = LoggerFactory.getLogger(CompositeExperimentService.class);

    private final UserProfileService userProfileService;
    private transient ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping;

    /**
     * Initialize composite experiment service for decision service
     *
     * @param userProfileService     UserProfileService implementation for storing user info.
     * @param forcedVariationMapping Forced Variation for user if exists
     */
    public CompositeExperimentService(@Nullable UserProfileService userProfileService,
                                      @Nullable ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping) {
        this.userProfileService = userProfileService;
        this.forcedVariationMapping = forcedVariationMapping;
    }

    /**
     * Evaluate which variation user should see.
     *
     * @param experiment  The Experiment the user will be bucketed into.
     * @param userContext It have user id, attributes and a reference to the current {@link ProjectConfig}
     * @return {@link ExperimentDecision}
     */
    @Override
    public ExperimentDecision getDecision(@Nonnull Experiment experiment,
                                          @Nonnull UserContext userContext) {
        ExperimentDecision experimentDecision;
        // check experiment status before proceeding
        if (!ExperimentUtils.isExperimentActive(experiment)) {
            return null;
        }
        // loop through different experiment decision services until we get a decision
        for (ExperimentDecisionService experimentDecisionService : getExperimentServices()) {
            experimentDecision = experimentDecisionService.getDecision(experiment, userContext);
            if (experimentDecision != null)
                return experimentDecision;
        }
        logger.info("User \"{}\" does not meet conditions to be in experiment \"{}\".", userContext.getUserId(), experiment.getKey());
        return new ExperimentDecision(null,
            new DecisionStatus(true, Reason.FailedToMeetExperimentConditions));
    }

    /**
     * Get Experiment Services
     *
     * @return List of {@link ExperimentDecisionService}
     */
    private List<ExperimentDecisionService> getExperimentServices() {
        return Arrays.asList(
            new ForcedVariationService(forcedVariationMapping),
            new WhitelistingService(),
            new UserProfileDecisionService(userProfileService),
            new ExperimentBucketerService.Builder()
                .withBucketer(new MurmurhashBucketer())
                .withAudienceEvaluator(new FullStackAudienceEvaluator())
                .build()
        );
    }
}

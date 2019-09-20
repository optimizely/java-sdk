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
package com.optimizely.ab.decision.experiment.service;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.decision.entities.DecisionStatus;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.decision.entities.Reason;
import com.optimizely.ab.decision.experiment.ExperimentDecisionService;
import com.optimizely.ab.event.internal.UserContext;

import java.util.Map;
import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check for whitelisting for a given user in experiment, then they are forced into their specified variation
 */
public class WhitelistingService implements ExperimentDecisionService {

    private static final Logger logger = LoggerFactory.getLogger(WhitelistingService.class);

    /**
     * Get the ExperimentDecision the user has been whitelisted into
     *
     * @param experiment  The Experiment the user will be bucketed into.
     * @param userContext It have user id, attributes and a reference to the current {@link ProjectConfig}
     * @return {@link Variation} the user is bucketed into if the user has a specified whitelisted variation.
     */
    @Override
    public ExperimentDecision getDecision(@Nonnull Experiment experiment,
                                          @Nonnull UserContext userContext) {
        String userId = userContext.getUserId();
        // if a user has a forced variation mapping, return the respective variation
        Map<String, String> userIdToVariationKeyMap = experiment.getUserIdToVariationKeyMap();
        if (userIdToVariationKeyMap.containsKey(userId)) {
            String forcedVariationKey = userIdToVariationKeyMap.get(userId);
            Variation variation = experiment.getVariationKeyToVariationMap().get(forcedVariationKey);
            if (variation != null) {
                logger.info("User \"{}\" is forced in variation \"{}\".", userId, forcedVariationKey);
                return new ExperimentDecision(variation,
                    new DecisionStatus(true, Reason.ForcedToWhitelistedVariation));
            } else {
                logger.error("Variation \"{}\" is not in the datafile. Not activating user \"{}\".",
                    forcedVariationKey, userId);
                return new ExperimentDecision(variation,
                    new DecisionStatus(true, Reason.NoVariationWhitelisted));
            }
        }
        return null;
    }
}

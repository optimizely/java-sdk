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

import com.optimizely.ab.config.*;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.decision.bucketer.MurmurhashBucketer;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.decision.entities.FeatureDecision;
import com.optimizely.ab.decision.evaluator.DecisionAudienceEvaluator;
import com.optimizely.ab.decision.experiment.service.ExperimentBucketerService;
import com.optimizely.ab.decision.feature.FeatureDecisionService;
import com.optimizely.ab.event.internal.UserContext;
import com.optimizely.ab.internal.ExperimentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;


public class FeatureRolloutService implements FeatureDecisionService {
    private static final Logger logger = LoggerFactory.getLogger(FeatureRolloutService.class);

    @Override
    public FeatureDecision getDecision(@Nonnull FeatureFlag featureFlag, @Nonnull UserContext userContext) {
        FeatureDecision featureDecision = getVariationForFeatureInRollout(featureFlag, userContext);
        if (featureDecision == null || featureDecision.variation == null) {
            logger.info("The user \"{}\" was not bucketed into a rollout for feature flag \"{}\".",
                userContext.getUserId(), featureFlag.getKey());
        } else {
            logger.info("The user \"{}\" was bucketed into a rollout for feature flag \"{}\".",
                userContext.getUserId(), featureFlag.getKey());
        }
        return featureDecision;
    }

    /**
     * Try to bucket the user into a rollout rule.
     * Evaluate the user for rules in priority order by seeing if the user satisfies the audience.
     * Fall back onto the everyone else rule if the user is ever excluded from a rule due to traffic allocation.
     *
     * @param featureFlag The feature flag the user wants to access.
     * @param userContext Contains UserID and FilteredAttributes
     * @return {@link FeatureDecision}
     */
    private FeatureDecision getVariationForFeatureInRollout(@Nonnull FeatureFlag featureFlag,
                                                            @Nonnull UserContext userContext) {
        // use rollout to get variation for feature
        if (featureFlag.getRolloutId().isEmpty()) {
            logger.info("The feature flag \"{}\" is not used in a rollout.", featureFlag.getKey());
            return null;
        }
        Rollout rollout = userContext.getProjectConfig().getRolloutIdMapping().get(featureFlag.getRolloutId());
        if (rollout == null) {
            logger.error("The rollout with id \"{}\" was not found in the datafile for feature flag \"{}\".",
                featureFlag.getRolloutId(), featureFlag.getKey());
            return null;
        }
        ExperimentBucketerService experimentBucketerService = new ExperimentBucketerService.Builder()
            .withBucketer(new MurmurhashBucketer())
            .withAudienceEvaluator(new DecisionAudienceEvaluator())
            .build();

        // for all rules before the everyone else rule
        int rolloutRulesLength = rollout.getExperiments().size();
        for (int i = 0; i < rolloutRulesLength - 1; i++) {
            Experiment rolloutRule = rollout.getExperiments().get(i);
            Audience audience = userContext.getProjectConfig().getAudienceIdMapping().get(rolloutRule.getAudienceIds().get(0));
            if (ExperimentUtils.isUserInExperiment(userContext.getProjectConfig(), rolloutRule, userContext.getAttributes())) {
                ExperimentDecision experimentDecision = experimentBucketerService.getDecision(rolloutRule, userContext);
                if (experimentDecision.variation == null) {
                    break;
                }
                return new FeatureDecision(rolloutRule, experimentDecision.variation,
                    FeatureDecision.DecisionSource.ROLLOUT);
            } else {
                logger.debug("User \"{}\" did not meet the conditions to be in rollout rule for audience \"{}\".",
                    userContext.getUserId(), audience.getName());
            }
        }

        // get last rule which is the fall back rule
        Experiment finalRule = rollout.getExperiments().get(rolloutRulesLength - 1);
        if (ExperimentUtils.isUserInExperiment(userContext.getProjectConfig(), finalRule, userContext.getAttributes())) {
            ExperimentDecision experimentDecision = experimentBucketerService.getDecision(finalRule, userContext);
            if (experimentDecision.variation != null) {
                return new FeatureDecision(finalRule, experimentDecision.variation,
                    FeatureDecision.DecisionSource.ROLLOUT);
            }
        }
        return null;
    }
}

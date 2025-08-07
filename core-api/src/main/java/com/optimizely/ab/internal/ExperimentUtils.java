/**
 *
 *    Copyright 2017-2022, Optimizely and contributors
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
package com.optimizely.ab.internal;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ExperimentCore;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.optimizelydecision.DecisionReasons;
import com.optimizely.ab.optimizelydecision.DecisionResponse;
import com.optimizely.ab.optimizelydecision.DefaultDecisionReasons;

public final class ExperimentUtils {

    private static final Logger logger = LoggerFactory.getLogger(ExperimentUtils.class);

    private ExperimentUtils() {
    }

    /**
     * Helper method to validate all pre-conditions before bucketing a user.
     *
     * @param experiment the experiment we are validating pre-conditions for
     * @return whether the pre-conditions are satisfied
     */
    public static boolean isExperimentActive(@Nonnull Experiment experiment) {
        return experiment.isActive();
    }

    /**
     * Determines whether a user satisfies audience conditions for the experiment.
     *
     * @param projectConfig     the current projectConfig
     * @param experiment        the experiment we are evaluating audiences for
     * @param user              the current OptimizelyUserContext
     * @param loggingEntityType It can be either experiment or rule.
     * @param loggingKey        In case of loggingEntityType is experiment it will be experiment key or else it will be rule number.
     * @return whether the user meets the criteria for the experiment
     */
    @Nonnull
    public static DecisionResponse<Boolean> doesUserMeetAudienceConditions(@Nonnull ProjectConfig projectConfig,
                                                                           @Nonnull ExperimentCore experiment,
                                                                           @Nonnull OptimizelyUserContext user,
                                                                           @Nonnull String loggingEntityType,
                                                                           @Nonnull String loggingKey) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();

        DecisionResponse<Boolean> decisionResponse;
        if (experiment.getAudienceConditions() != null) {
            logger.debug("Evaluating audiences for {} \"{}\": {}.", loggingEntityType, loggingKey, experiment.getAudienceConditions());
            decisionResponse = evaluateAudienceConditions(projectConfig, experiment, user, loggingEntityType, loggingKey);
        } else {
            decisionResponse = evaluateAudience(projectConfig, experiment, user, loggingEntityType, loggingKey);
        }

        Boolean resolveReturn = decisionResponse.getResult();
        reasons.merge(decisionResponse.getReasons());

        return new DecisionResponse(
            resolveReturn != null && resolveReturn,    // make it Nonnull for if-evaluation
            reasons);
    }

    @Nonnull
    public static DecisionResponse<Boolean> evaluateAudience(@Nonnull ProjectConfig projectConfig,
                                                             @Nonnull ExperimentCore experiment,
                                                             @Nonnull OptimizelyUserContext user,
                                                             @Nonnull String loggingEntityType,
                                                             @Nonnull String loggingKey) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();

        List<String> experimentAudienceIds = experiment.getAudienceIds();

        // if there are no audiences, ALL users should be part of the experiment
        if (experimentAudienceIds.isEmpty()) {
            return new DecisionResponse(true, reasons);
        }

        List<Condition> conditions = new ArrayList<>();
        for (String audienceId : experimentAudienceIds) {
            AudienceIdCondition condition = new AudienceIdCondition(audienceId);
            conditions.add(condition);
        }

        OrCondition implicitOr = new OrCondition(conditions);

        logger.debug("Evaluating audiences for {} \"{}\": {}.", loggingEntityType, loggingKey, conditions);

        Boolean result = implicitOr.evaluate(projectConfig, user);
        String message = reasons.addInfo("Audiences for %s \"%s\" collectively evaluated to %s.", loggingEntityType, loggingKey, result);
        logger.info(message);

        return new DecisionResponse(result, reasons);
    }

    @Nonnull
    public static DecisionResponse<Boolean> evaluateAudienceConditions(@Nonnull ProjectConfig projectConfig,
                                                                       @Nonnull ExperimentCore experiment,
                                                                       @Nonnull OptimizelyUserContext user,
                                                                       @Nonnull String loggingEntityType,
                                                                       @Nonnull String loggingKey) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();

        Condition conditions = experiment.getAudienceConditions();
        if (conditions == null) return new DecisionResponse(null, reasons);

        Boolean result = null;
        try {
            result = conditions.evaluate(projectConfig, user);
            String message = reasons.addInfo("Audiences for %s \"%s\" collectively evaluated to %s.", loggingEntityType, loggingKey, result);
            logger.info(message);
        } catch (Exception e) {
            String message = reasons.addInfo("Condition invalid: %s", e.getMessage());
            logger.error(message);
        }

        return new DecisionResponse(result, reasons);
    }

}

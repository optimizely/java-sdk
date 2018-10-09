/**
 *
 *    Copyright 2017-2018, Optimizely and contributors
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

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.AudienceHolderCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public final class ExperimentUtils {

    private static final Logger logger = LoggerFactory.getLogger(ExperimentUtils.class);

    private ExperimentUtils() {}

    /**
     * Helper method to validate all pre-conditions before bucketing a user.
     *
     * @param experiment the experiment we are validating pre-conditions for
     * @return whether the pre-conditions are satisfied
     */
    public static boolean isExperimentActive(@Nonnull Experiment experiment) {

        if (!experiment.isActive()) {
            logger.info("Experiment \"{}\" is not running.", experiment.getKey());
            return false;
        }

        return true;
    }

    /**
     * Determines whether a user satisfies audience conditions for the experiment.
     *
     * @param projectConfig the current projectConfig
     * @param experiment the experiment we are evaluating audiences for
     * @param attributes the attributes of the user
     * @return whether the user meets the criteria for the experiment
     */
    public static boolean isUserInExperiment(@Nonnull ProjectConfig projectConfig,
                                             @Nonnull Experiment experiment,
                                             @Nonnull Map<String, ?> attributes) {
        if (experiment.getAudienceConditions() != null) {
            Boolean resolveReturn = resolveAudienceConditions(projectConfig, experiment, attributes);
            return resolveReturn == null ? false : resolveReturn;
        }
        List<String> experimentAudienceIds = experiment.getAudienceIds();

        // if there are no audiences, ALL users should be part of the experiment
        if (experimentAudienceIds.isEmpty()) {
            return true;
        }

        for (String audienceId : experimentAudienceIds) {
            AudienceHolderCondition conditions = new AudienceHolderCondition(projectConfig.getAudience(audienceId));
            Boolean conditionEval = conditions.evaluate(attributes);

            if (conditionEval != null && conditionEval) {
                return true;
            }
        }

        return false;
    }

    public static boolean resolveAudienceConditions(@Nonnull ProjectConfig projectConfig,
                                            @Nonnull Experiment experiment,
                                            @Nonnull Map<String, ?> attributes) {

        resolveAudienceIdConditions(projectConfig, experiment.getAudienceConditions());

        return experiment.getAudienceConditions().evaluate(attributes);
    }

    public static void resolveAudienceIdConditions(@Nonnull ProjectConfig projectConfig,
                                                    @Nonnull Condition conditions) {

        if (conditions instanceof AndCondition) {
            AndCondition andCondition = (AndCondition) conditions;
            for (Condition condition : andCondition.getConditions()) {
                if (condition instanceof AudienceHolderCondition) {
                    AudienceHolderCondition holder = (AudienceHolderCondition) condition;
                    holder.setAudience(projectConfig.getAudience(holder.getAudienceId()));
                }
                else {
                    resolveAudienceIdConditions(projectConfig, condition);
                }
            }
        }
        else if (conditions instanceof OrCondition) {
            OrCondition orCondition = (OrCondition) conditions;
            for (Condition condition : orCondition.getConditions()) {
                if (condition instanceof AudienceHolderCondition) {
                    AudienceHolderCondition holder = (AudienceHolderCondition) condition;
                    holder.setAudience(projectConfig.getAudience(holder.getAudienceId()));
                }
                else {
                    resolveAudienceIdConditions(projectConfig, condition);
                }
            }
        }
        else if (conditions instanceof NotCondition) {
            NotCondition notCondition = (NotCondition) conditions;

            Condition condition = notCondition.getCondition();
            if (condition instanceof AudienceHolderCondition) {
                AudienceHolderCondition holder = (AudienceHolderCondition) condition;
                holder.setAudience(projectConfig.getAudience(holder.getAudienceId()));
            }
            else {
                resolveAudienceIdConditions(projectConfig, conditions);
            }
        }

    }

}

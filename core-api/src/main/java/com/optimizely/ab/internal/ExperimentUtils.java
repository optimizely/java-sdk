/**
 *
 *    Copyright 2017-2019, Optimizely and contributors
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
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.OrCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * @param experiment    the experiment we are evaluating audiences for
     * @param attributes    the attributes of the user
     * @return whether the user meets the criteria for the experiment
     */
    public static boolean isUserInExperiment(@Nonnull ProjectConfig projectConfig,
                                             @Nonnull Experiment experiment,
                                             @Nonnull Map<String, ?> attributes) {
        if (experiment.getAudienceConditions() != null) {
            Boolean resolveReturn = evaluateAudienceConditions(projectConfig, experiment, attributes);
            return resolveReturn == null ? false : resolveReturn;
        } else {
            Boolean resolveReturn = evaluateAudience(projectConfig, experiment, attributes);
            return Boolean.TRUE.equals(resolveReturn);
        }
    }

    @Nullable
    public static Boolean evaluateAudience(@Nonnull ProjectConfig projectConfig,
                                           @Nonnull Experiment experiment,
                                           @Nonnull Map<String, ?> attributes) {
        List<String> experimentAudienceIds = experiment.getAudienceIds();

        // if there are no audiences, ALL users should be part of the experiment
        if (experimentAudienceIds.isEmpty()) {
            logger.debug("There is no Audience associated with experiment {}", experiment.getKey());
            return true;
        }

        List<Condition> conditions = new ArrayList<>();
        for (String audienceId : experimentAudienceIds) {
            AudienceIdCondition condition = new AudienceIdCondition(audienceId);
            conditions.add(condition);
        }

        OrCondition implicitOr = new OrCondition(conditions);

        logger.debug("Evaluating audiences for experiment \"{}\": \"{}\"", experiment.getKey(), conditions);

        Boolean result = implicitOr.evaluate(projectConfig, attributes);

        logger.info("Audiences for experiment {} collectively evaluated to {}", experiment.getKey(), result);

        return result;
    }

    @Nullable
    public static Boolean evaluateAudienceConditions(@Nonnull ProjectConfig projectConfig,
                                                     @Nonnull Experiment experiment,
                                                     @Nonnull Map<String, ?> attributes) {

        Condition conditions = experiment.getAudienceConditions();
        if (conditions == null) return null;
        logger.debug("Evaluating audiences for experiment \"{}\": \"{}\"", experiment.getKey(), conditions.toString());
        try {
            Boolean result = conditions.evaluate(projectConfig, attributes);
            logger.info("Audiences for experiment {} collectively evaluated to {}", experiment.getKey(), result);
            return result;
        } catch (Exception e) {
            logger.error("Condition invalid", e);
            return null;
        }
    }


}

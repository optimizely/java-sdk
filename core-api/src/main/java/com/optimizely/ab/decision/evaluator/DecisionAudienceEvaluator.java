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
package com.optimizely.ab.decision.evaluator;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.event.internal.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Determines whether a user satisfies audience conditions for the experiment.
 */
public class DecisionAudienceEvaluator implements AudienceEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(DecisionAudienceEvaluator.class);

       /**
     * Validates user satisfies audience conditions for the experiment
     *
     * @param experiment  The Experiment the user will be bucketed into.
     * @param userContext It have user id, attributes and a reference to the current {@link ProjectConfig}
     * @return flag if user satisfies audience conditions for the experiment
     */
    @Override
    public boolean evaluate(@Nonnull Experiment experiment,
                            @Nonnull UserContext userContext) {
        if (experiment.getAudienceConditions() != null) {
            return evaluateAudienceConditions(experiment, userContext);
        } else {
            Boolean isAudienceValid = evaluateAudience(experiment, userContext);
            return Boolean.TRUE.equals(isAudienceValid);
        }
    }

    /**
     * Evaluate audience if experiment has no conditions
     *
     * @param experiment  The Experiment the user will be bucketed into.
     * @param userContext It have user id, attributes and a reference to the current {@link ProjectConfig}
     * @return flag if audience are valid for experiment
     */
    @Nullable
    private static Boolean evaluateAudience(@Nonnull Experiment experiment,
                                            @Nonnull UserContext userContext) {
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

        Boolean result = implicitOr.evaluate(userContext.getProjectConfig(), userContext.getAttributes());
        logger.info("Audiences for experiment {} collectively evaluated to {}", experiment.getKey(), result);

        return result;
    }

    /**
     * Evaluate audience if experiment has conditions
     *
     * @param experiment  The Experiment the user will be bucketed into.
     * @param userContext It have user id, attributes and a reference to the current {@link ProjectConfig}
     * @return flag if audience conditions are valid for experiment
     */
    private static Boolean evaluateAudienceConditions(@Nonnull Experiment experiment,
                                                      @Nonnull UserContext userContext) {
        Condition conditions = experiment.getAudienceConditions();
        if (conditions == null) return false;
        logger.debug("Evaluating audiences for experiment \"{}\": \"{}\"", experiment.getKey(), conditions.toString());
        try {
            Boolean result = conditions.evaluate(userContext.getProjectConfig(), userContext.getAttributes());
            logger.info("Audiences for experiment {} collectively evaluated to {}", experiment.getKey(), result);
            return result;
        } catch (Exception e) {
            logger.error("Condition invalid", e);
            return false;
        }
    }
}

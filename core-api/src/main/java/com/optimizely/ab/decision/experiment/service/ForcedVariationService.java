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
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.decision.DecisionUtils;
import com.optimizely.ab.decision.entities.DecisionStatus;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.decision.entities.Reason;
import com.optimizely.ab.decision.experiment.IExperimentDecisionService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.optimizely.ab.event.internal.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Gets the forced variation for a given user and experiment if present.
 */
public class ForcedVariationService implements IExperimentDecisionService {

    private transient ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping;
    private static final Logger logger = LoggerFactory.getLogger(ForcedVariationService.class);

    public ForcedVariationService(ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping) {
        this.forcedVariationMapping = forcedVariationMapping;
    }

    /**
     * @return {@link Variation} of an {@link Experiment} the user was bucketed into. This value can be null if the
     * forced variation fails.
     */
    @Override
    public ExperimentDecision getDecision(@Nonnull Experiment experiment,
                                          @Nonnull UserContext userContext) {
        String userId = userContext.getUserId();
        // if the user id is invalid, return false.
        if (!DecisionUtils.validateUserId(userId)) {
            return null;
        }

        Map<String, String> experimentToVariation = forcedVariationMapping.get(userId);
        if (experimentToVariation != null) {
            String variationId = experimentToVariation.get(experiment.getId());
            if (variationId != null) {
                Variation variation = experiment.getVariationIdToVariationMap().get(variationId);
                if (variation != null) {
                    logger.debug("Variation \"{}\" is mapped to experiment \"{}\" and user \"{}\" in the forced variation map",
                        variation.getKey(), experiment.getKey(), userId);
                    return new ExperimentDecision(variation,
                        new DecisionStatus(true, Reason.ForcedVariationMapped));
                }
            } else {
                logger.debug("No variation for experiment \"{}\" mapped to user \"{}\" in the forced variation map ", experiment.getKey(), userId);
                return new ExperimentDecision(null,
                    new DecisionStatus(true, Reason.NoVariationForExperiment) );
            }
        }
        return null;
    }
}

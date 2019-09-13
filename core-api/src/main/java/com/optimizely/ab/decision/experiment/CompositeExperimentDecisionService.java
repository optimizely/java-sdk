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

import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.decision.entities.DecisionStatus;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.decision.entities.Reason;
import com.optimizely.ab.event.internal.UserContext;
import com.optimizely.ab.internal.ExperimentUtils;
import com.optimizely.ab.decision.experiment.service.ExperimentBucketerDecisionService;

import java.util.List;
import java.util.Arrays;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExperimentDecisionContext contains the information needed to be able to make a decision for a given experiment
 */
public class CompositeExperimentDecisionService implements IExperimentDecisionService {

    private final Bucketer bucketer;
    private static final Logger logger = LoggerFactory.getLogger(CompositeExperimentDecisionService.class);

    /**
     * Initialize a CompositeExperimentDecisionService for the Optimizely client.
     *
     * @param bucketer to allocate new users to an experiment.
     */
    public CompositeExperimentDecisionService(@Nonnull Bucketer bucketer) {
        this.bucketer = bucketer;
    }

    /**
     * @return List of Decision service for experiment
     */
    private List<IExperimentDecisionService> getExperimentServices() {
        return Arrays.asList(
            new ExperimentBucketerDecisionService(bucketer)
        );
    }

    @Override
    public ExperimentDecision getDecision(@Nonnull Experiment experiment,
                                          @Nonnull UserContext userContext) {
        ExperimentDecision experimentDecision;
        // check experiment status before proceeding
        if (!ExperimentUtils.isExperimentActive(experiment)) {
            return null;
        }
        // loop through the different features decision services until we get a decision
        for (IExperimentDecisionService experimentDecisionService : getExperimentServices()) {
            experimentDecision = experimentDecisionService.getDecision(experiment, userContext);
            if (experimentDecision != null)
                return experimentDecision;
        }
        logger.info("User \"{}\" does not meet conditions to be in experiment \"{}\".", userContext.getUserId(), experiment.getKey());
        return new ExperimentDecision(null, new DecisionStatus(false, Reason.NotBucketedIntoVariation));
    }
}

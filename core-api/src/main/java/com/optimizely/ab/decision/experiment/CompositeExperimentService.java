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
import com.optimizely.ab.decision.entities.DecisionStatus;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.decision.entities.Reason;
import com.optimizely.ab.event.internal.UserContext;
import com.optimizely.ab.internal.ExperimentUtils;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CompositeExperimentService contains the information needed to be able to make a decision for a
 * given experiment
 */
public class CompositeExperimentService implements IExperimentDecisionService {

    private static final Logger logger = LoggerFactory.getLogger(CompositeExperimentService.class);

    /**
     * Evaluate user IDs and attributes to determine which variation user should see.
     *
     * @param experiment  The Experiment the user will be bucketed into.
     * @param userContext It have user id, attributes and a reference to the current {@link ProjectConfig}
     * @return {@link ExperimentDecision}
     */
    @Override
    public ExperimentDecision getDecision(@Nonnull Experiment experiment,
                                          @Nonnull UserContext userContext) {
        // check experiment status before proceeding
        if (!ExperimentUtils.isExperimentActive(experiment)) {
            return null;
        }
        // loop through different experiment decision services until we get a decision
        for (IExperimentDecisionService experimentDecisionService : getExperimentServices()) {
            ExperimentDecision experimentDecision = experimentDecisionService.getDecision(experiment, userContext);
            if (experimentDecision != null)
                return experimentDecision;
        }
        logger.info("User \"{}\" does not meet conditions to be in experiment \"{}\".", userContext.getUserId(), experiment.getKey());
        return new ExperimentDecision(null,
            new DecisionStatus(true, Reason.FailedToMeetExperimentConditions));
    }

    /**
     * Returns Experiment Decision Services for evaluation and decision of Variation that user should see
     *
     * @return List of {@link IExperimentDecisionService}
     */
    private List<IExperimentDecisionService> getExperimentServices() {
        //TODO: All the Experiment Services will be added here
        return Collections.emptyList();
    }
}

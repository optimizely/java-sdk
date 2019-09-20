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
import com.optimizely.ab.event.internal.UserContext;
import com.optimizely.ab.decision.entities.ExperimentDecision;

import javax.annotation.Nonnull;

/**
 * Experiment Decision Service can make a decision about which variation of an experiment the user will be
 * allocated to.
 */
public interface ExperimentDecisionService {
    /**
     * Get a {@link ExperimentDecision} of an {@link Experiment} for a user to be allocated into.
     *
     * @param experiment  The Experiment the user will be bucketed into.
     * @param userContext It have user id, attributes and a reference to the current {@link ProjectConfig}
     * @return {@link ExperimentDecision}
     */
    ExperimentDecision getDecision(@Nonnull Experiment experiment,
                                   @Nonnull UserContext userContext);
}
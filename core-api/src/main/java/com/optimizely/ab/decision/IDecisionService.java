/****************************************************************************
 * Copyright 2016-2019, Optimizely, Inc. and contributors                   *
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
package com.optimizely.ab.decision;

import javax.annotation.Nonnull;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.event.internal.UserContext;

/**
 * DecisionService interface is used to make a decision for a given feature or experiment
 */
public interface IDecisionService {
    /**
     * getExperimentDecision returns a ExperimentDecision for the given user and experiment
     */
    ExperimentDecision getExperimentDecision(@Nonnull Experiment experiment,
                                             @Nonnull UserContext userContext);
}

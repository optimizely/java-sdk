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
package com.optimizely.ab.decision.audience;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.event.internal.UserContext;
import com.optimizely.ab.internal.ExperimentUtils;

import javax.annotation.Nonnull;

/**
 * Determines whether a user satisfies audience conditions for the experiment.
 */
public class AudienceEvaluator implements IAudienceEvaluator {
    /**
     * Validate Audience conditions for the experiment
     */
    @Override
    public boolean evaluate(@Nonnull Experiment experiment,
                            @Nonnull UserContext userContext) {
       return ExperimentUtils.isUserInExperiment(userContext.getProjectConfig(),
                                                 experiment,
                                                 userContext.getAttributes());
    }
}

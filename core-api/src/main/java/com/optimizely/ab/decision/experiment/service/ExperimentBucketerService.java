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
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.decision.DecisionUtils;
import com.optimizely.ab.decision.bucketer.Bucketer;
import com.optimizely.ab.decision.bucketer.IBucketer;
import com.optimizely.ab.decision.entities.DecisionStatus;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.decision.entities.Reason;
import com.optimizely.ab.decision.experiment.IExperimentDecisionService;
import com.optimizely.ab.event.internal.UserContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * ExperimentBucketerService returns the decision by using Bucketer
 */
public class ExperimentBucketerService implements IExperimentDecisionService {

    private final IBucketer bucketer = new Bucketer();
    private static final Logger logger = LoggerFactory.getLogger(ExperimentBucketerService.class);

    /**
     * Returns decision of variation based on Bucketer
     *
     * @param experiment  The Experiment the user will be bucketed into.
     * @param userContext It have user id, attributes and a reference to the current {@link ProjectConfig}
     * @return {@link ExperimentDecision}
     */
    @Override
    public ExperimentDecision getDecision(@Nonnull Experiment experiment,
                                          @Nonnull UserContext userContext) {
        String bucketingId = DecisionUtils.getBucketingId(userContext.getUserId(), userContext.getAttributes());
        Variation variation = bucketer.bucket(experiment, bucketingId, userContext.getProjectConfig());
        if (variation != null)
            return new ExperimentDecision(variation,
                new DecisionStatus(false, Reason.BucketedIntoVariation));

        return new ExperimentDecision(null,
            new DecisionStatus(false, Reason.NotBucketedIntoVariation));
    }
}

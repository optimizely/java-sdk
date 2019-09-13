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

import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.decision.entities.DecisionStatus;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.decision.entities.Reason;
import com.optimizely.ab.decision.experiment.IExperimentDecisionService;
import com.optimizely.ab.event.internal.UserContext;

import com.optimizely.ab.internal.ControlAttribute;
import com.optimizely.ab.internal.ExperimentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * ExperimentBucketerDecisionService makes a decision using the experiment bucketer
 */
public class ExperimentBucketerDecisionService implements IExperimentDecisionService {

    private final Bucketer bucketer;
    private static final Logger logger = LoggerFactory.getLogger(ExperimentBucketerDecisionService.class);

    /**
     * Initialize Bucketer
     */
    public ExperimentBucketerDecisionService(@Nonnull Bucketer bucketer) {
        this.bucketer = bucketer;
    }

    @Override
    public ExperimentDecision getDecision(@Nonnull Experiment experiment,
                                          @Nonnull UserContext userContext) {
        if (ExperimentUtils.isUserInExperiment(userContext.getProjectConfig(), experiment, userContext.getAttributes())) {
            String bucketingId = getBucketingId(userContext.getUserId(), userContext.getAttributes());
            Variation variation = bucketer.bucket(experiment, bucketingId, userContext.getProjectConfig());
            return new ExperimentDecision(variation,
                new DecisionStatus(false, Reason.BucketedIntoVariation));
        }
        return new ExperimentDecision(null,
            new DecisionStatus(false, Reason.NotBucketedIntoVariation));
    }

    /**
     * Get the bucketingId of a user if a bucketingId exists in attributes, or else default to userId.
     *
     * @param userId             The userId of the user.
     * @param filteredAttributes The user's attributes. This should be filtered to just attributes in the Datafile.
     * @return bucketingId if it is a String type in attributes.
     * else return userId
     */
    private String getBucketingId(@Nonnull String userId,
                                  @Nonnull Map<String, ?> filteredAttributes) {
        String bucketingId = userId;
        if (filteredAttributes != null && filteredAttributes.containsKey(ControlAttribute.BUCKETING_ATTRIBUTE.toString())) {
            if (String.class.isInstance(filteredAttributes.get(ControlAttribute.BUCKETING_ATTRIBUTE.toString()))) {
                bucketingId = (String) filteredAttributes.get(ControlAttribute.BUCKETING_ATTRIBUTE.toString());
                logger.debug("BucketingId is valid: \"{}\"", bucketingId);
            } else {
                logger.warn("BucketingID attribute is not a string. Defaulted to userId");
            }
        }
        return bucketingId;
    }
}

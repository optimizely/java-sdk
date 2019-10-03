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

import com.optimizely.ab.bucketing.internal.MurmurHash3;
import com.optimizely.ab.config.*;
import com.optimizely.ab.decision.bucketer.Bucketer;
import com.optimizely.ab.decision.bucketer.MurmurhashBucketer;
import com.optimizely.ab.decision.entities.DecisionStatus;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.decision.entities.Reason;
import com.optimizely.ab.decision.evaluator.AudienceEvaluator;
import com.optimizely.ab.decision.experiment.ExperimentDecisionService;
import com.optimizely.ab.event.internal.UserContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.optimizely.ab.decision.DecisionUtils.getBucketingId;

/**
 * ExperimentBucketerService returns the decision by using FullStackBucketer
 */
public class ExperimentBucketerService implements ExperimentDecisionService {

    private final Bucketer bucketer;
    private AudienceEvaluator audienceEvaluator;
    private static final Logger logger = LoggerFactory.getLogger(ExperimentBucketerService.class);

    private ExperimentBucketerService(@Nonnull Bucketer bucketer,
                                      @Nonnull AudienceEvaluator audienceEvaluator) {
        this.bucketer = bucketer;
        this.audienceEvaluator = audienceEvaluator;
    }

    /**
     * Returns ExperimentDecision based on FullStackBucketer
     *
     * @param experiment  The Experiment the user will be bucketed into.
     * @param userContext It have user id, attributes and a reference to the current {@link ProjectConfig}
     * @return {@link ExperimentDecision}
     */
    @Override
    public ExperimentDecision getDecision(@Nonnull Experiment experiment,
                                 @Nonnull UserContext userContext) {
        if (audienceEvaluator.evaluate(experiment, userContext)) {
            String bucketingId = getBucketingId(userContext.getUserId(), userContext.getAttributes());
            Variation variation = bucket(experiment, bucketingId, userContext.getProjectConfig());
            if (variation != null)
                return new ExperimentDecision(variation,
                    new DecisionStatus(false, Reason.BucketedIntoVariation));
        }
        return new ExperimentDecision(null,
            new DecisionStatus(false, Reason.NotBucketedIntoVariation));
    }

    /**
     * Assign a {@link Variation} of an {@link Experiment} to a user based on hashed value from murmurhash3.
     *
     * @param experiment  The Experiment in which the user is to be bucketed.
     * @param bucketingId A user-assigned value, to create the key for the murmur hash.
     * @return The {@link Variation} user is bucketed into or null.
     */
    @Nullable
    private Variation bucket(@Nonnull Experiment experiment,
                             @Nonnull String bucketingId,
                             @Nonnull ProjectConfig projectConfig) {
        String groupId = experiment.getGroupId();
        // check whether the experiment belongs to a group
        if (!groupId.isEmpty()) {
            Group experimentGroup = projectConfig.getGroupIdMapping().get(groupId);
            // bucket to an experiment only if group entities are to be mutually exclusive
            if (experimentGroup.getPolicy().equals(Group.RANDOM_POLICY)) {
                Experiment bucketedExperiment = bucketToExperiment(experimentGroup, bucketingId, projectConfig);
                if (bucketedExperiment == null) {
                    logger.info("User with bucketingId \"{}\" is not in any experiment of group {}.", bucketingId,
                        experimentGroup.getId());
                    return null;
                }
                // if the experiment a user is bucketed in within a group isn't the same as the experiment provided,
                // don't perform further bucketing within the experiment
                if (!bucketedExperiment.getId().equals(experiment.getId())) {
                    logger.info("User with bucketingId \"{}\" is not in experiment \"{}\" of group {}.", bucketingId,
                        experiment.getKey(),
                        experimentGroup.getId());
                    return null;
                }
                logger.info("User with bucketingId \"{}\" is in experiment \"{}\" of group {}.", bucketingId,
                    experiment.getKey(),
                    experimentGroup.getId());
            }
        }
        return bucketToVariation(experiment, bucketingId);
    }

    /**
     * If experiment belongs to group then first bucket to experiment
     *
     * @param group         group from which experiment belongs to
     * @param bucketingId   bucketing id provided
     * @param projectConfig current projectConfig
     * @return {@link Experiment}
     */
    private Experiment bucketToExperiment(@Nonnull Group group,
                                          @Nonnull String bucketingId,
                                          @Nonnull ProjectConfig projectConfig) {
        // combining bucket id with group id
        String bucketKey = bucketingId + group.getId();
        // generating hashcode for getting bucket value
        int hashCode = MurmurHash3.murmurhash3_x86_32(bucketKey,
            0,
            bucketKey.length(),
            MurmurhashBucketer.MURMUR_HASH_SEED);
        int bucketValue = bucketer.generateBucketValue(hashCode);
        logger.debug("Assigned bucket {} to user with bucketingId \"{}\" during experiment bucketing.",
            bucketValue,
            bucketingId);

        String bucketedExperimentId = bucketToEntity(bucketValue, group.getTrafficAllocation());
        if (bucketedExperimentId != null) {
            return projectConfig.getExperimentIdMapping().get(bucketedExperimentId);
        }
        // user was not bucketed to an experiment in the group
        return null;
    }

    /**
     * Buckets user to variation using bucketer
     *
     * @param experiment  The Experiment the user will be bucketed into.
     * @param bucketingId bucketing id provided
     * @return {@link Variation}
     */
    private Variation bucketToVariation(@Nonnull Experiment experiment,
                                        @Nonnull String bucketingId) {
        // combining bucket id with group id
        String experimentKey = experiment.getKey();
        String combinedBucketId = bucketingId + experiment.getId();
        int hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId,
            0,
            combinedBucketId.length(),
            MurmurhashBucketer.MURMUR_HASH_SEED);
        int bucketValue = bucketer.generateBucketValue(hashCode);
        logger.debug("Assigned bucket {} to user with bucketingId \"{}\" when bucketing to a variation.",
            bucketValue,
            bucketingId);
        // buckets to entity
        String bucketedVariationId = bucketToEntity(bucketValue, experiment.getTrafficAllocation());
        if (bucketedVariationId != null) {
            Variation bucketedVariation = experiment.getVariationIdToVariationMap().get(bucketedVariationId);
            String variationKey = bucketedVariation.getKey();
            logger.info("User with bucketingId \"{}\" is in variation \"{}\" of experiment \"{}\".", bucketingId,
                variationKey,
                experimentKey);
            return bucketedVariation;
        }
        // user was not bucketed to a variation
        logger.info("User with bucketingId \"{}\" is not in any variation of experiment \"{}\".",
            bucketingId,
            experimentKey);
        return null;
    }

    /**
     * @param bucketValue        bucket value within the range
     * @param trafficAllocations traffic allocations for bucketing
     * @return Id for Current Allocation Entity
     */
    private String bucketToEntity(int bucketValue, List<TrafficAllocation> trafficAllocations) {
        int currentEndOfRange;
        for (TrafficAllocation currAllocation : trafficAllocations) {
            currentEndOfRange = currAllocation.getEndOfRange();
            if (bucketValue < currentEndOfRange) {
                // for mutually exclusive bucketing, de-allocated space is represented by an empty string
                if (currAllocation.getEntityId().isEmpty()) {
                    return null;
                }
                return currAllocation.getEntityId();
            }
        }
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Bucketer bucketer;
        private AudienceEvaluator audienceEvaluator;

        /**
         * Set Bucketer for bucketing
         *
         * @param bucketer Bucketer Service
         * @return {@link Builder}
         */
        public Builder withBucketer(Bucketer bucketer) {
            this.bucketer = bucketer;
            return this;
        }

        /**
         * Set Audience Evaluator
         *
         * @param evaluator Audience Evaluator
         * @return {@link Builder}
         */
        public Builder withAudienceEvaluator(AudienceEvaluator evaluator) {
            this.audienceEvaluator = evaluator;
            return this;
        }

        /**
         * Create object of ExperimentBucketerService
         *
         * @return {@link ExperimentBucketerService}
         */
        public ExperimentBucketerService build() {
            return new ExperimentBucketerService(bucketer, audienceEvaluator);
        }
    }
}

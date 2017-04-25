/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.bucketing;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.internal.ProjectValidationUtils;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class DecisionService {

    private final Bucketer bucketer;
    private final ProjectConfig projectConfig;
    private final Logger logger;
    private final UserProfile userProfile;

    public DecisionService(@Nonnull Bucketer bucketer,
                    @Nonnull Logger logger,
                    @Nonnull ProjectConfig projectConfig,
                    @Nonnull UserProfile userProfile) {
        this.bucketer = bucketer;
        this.logger = logger;
        this.projectConfig = projectConfig;
        this.userProfile = userProfile;
    }

    public @Nullable Variation getVariation(@Nonnull Experiment experiment,
                                            @Nonnull String userId,
                                            @Nonnull Map<String, String> filteredAttributes) {

        if (!ProjectValidationUtils.validatePreconditions(projectConfig, userProfile, experiment, userId, filteredAttributes)) {
            return null;
        }

        Variation variation;

        // check for whitelisting
        variation = getWhitelistedVariation(experiment, userId);
        if (variation != null) {
            return variation;
        }

        // check if user exists in user profile
        variation = getStoredVariation(experiment, userId);
        if (variation != null) {
            return variation;
        }

        if (ProjectValidationUtils.isUserInExperiment(projectConfig, experiment, filteredAttributes)) {
            return bucketer.bucket(experiment, userId);
        }
        logger.info("User \"{}\" does not meet conditions to be in experiment \"{}\".", userId, experiment.getKey());

        return null;
    }

    /**
     * Get the variation the user has been whitelisted into.
     * @param experiment {@link Experiment} in which user is to be bucketed.
     * @param userId User Identifier
     * @return null if the user is not whitelisted into any variation
     *      {@link Variation} the user is bucketed into if the user has a specified whitelisted variation.
     */
    public @Nullable Variation getWhitelistedVariation(@Nonnull Experiment experiment, @Nonnull String userId) {
        // if a user has a forced variation mapping, return the respective variation
        Map<String, String> userIdToVariationKeyMap = experiment.getUserIdToVariationKeyMap();
        if (userIdToVariationKeyMap.containsKey(userId)) {
            String forcedVariationKey = userIdToVariationKeyMap.get(userId);
            Variation forcedVariation = experiment.getVariationKeyToVariationMap().get(forcedVariationKey);
            if (forcedVariation != null) {
                logger.info("User \"{}\" is forced in variation \"{}\".", userId, forcedVariationKey);
            } else {
                logger.error("Variation \"{}\" is not in the datafile. Not activating user \"{}\".", forcedVariationKey,
                        userId);
            }

            return forcedVariation;
        }

        return null;
    }

    /**
     * Get the {@link Variation} that has been stored for the user in the {@link UserProfile} implementation.
     * @param experiment {@link Experiment} in which the user was bucketed.
     * @param userId User Identifier
     * @return null if the {@link UserProfile} implementation is null or the user was not previously bucketed.
     *      else return the {@link Variation} the user was previously bucketed into.
     */
    private @Nullable Variation getStoredVariation(@Nonnull Experiment experiment, @Nonnull String userId) {
        // ---------- Check User Profile for Sticky Bucketing ----------
        // If a user profile instance is present then check it for a saved variation
        String experimentId = experiment.getId();
        String experimentKey = experiment.getKey();
        if (userProfile != null) {
            String variationId = userProfile.lookup(userId, experimentId);
            if (variationId != null) {
                Variation savedVariation = projectConfig
                        .getExperimentIdMapping()
                        .get(experimentId)
                        .getVariationIdToVariationMap()
                        .get(variationId);
                logger.info("Returning previously activated variation \"{}\" of experiment \"{}\" "
                                + "for user \"{}\" from user profile.",
                        savedVariation.getKey(), experimentKey, userId);
                // A variation is stored for this combined bucket id
                return savedVariation;
            } else {
                logger.info("No previously activated variation of experiment \"{}\" "
                                + "for user \"{}\" found in user profile.",
                        experimentKey, userId);
            }
        }

        return null;
    }

}
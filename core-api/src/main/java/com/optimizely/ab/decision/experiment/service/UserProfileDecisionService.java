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

import com.optimizely.ab.bucketing.Decision;
import com.optimizely.ab.bucketing.UserProfile;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.bucketing.UserProfileUtils;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.decision.entities.DecisionStatus;
import com.optimizely.ab.decision.entities.ExperimentDecision;
import com.optimizely.ab.decision.entities.Reason;
import com.optimizely.ab.decision.experiment.IExperimentDecisionService;
import com.optimizely.ab.event.internal.UserContext;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User Profile already exists with a valid variation in case User Profiles are enabled in SDK
 */
public class UserProfileDecisionService implements IExperimentDecisionService {

    private final UserProfileService userProfileService;
    private static final Logger logger = LoggerFactory.getLogger(UserProfileDecisionService.class);

    public UserProfileDecisionService(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * fetch the user profile map from the user profile service
     *
     * @return {@link ExperimentDecision} of an {@link Experiment} the user was bucketed into.
     */
    @Override
    public ExperimentDecision getDecision(@Nonnull Experiment experiment,
                                          @Nonnull UserContext userContext) {
        String userId = userContext.getUserId();
        UserProfile userProfile = getUserProfile(userId);
        // check if user exists in user profile
        if (userProfile != null) {
            Variation variation = getStoredVariation(experiment, userProfile, userContext.getProjectConfig());
            // return the stored variation if it exists
            if (variation != null) {
                return new ExperimentDecision(variation, new DecisionStatus(true, Reason.BucketedToVariationInUserProfile));
            }
        }
        return null;
    }

    // returns UserProfileService Map
    private UserProfile getUserProfile(String userId) {
        UserProfile userProfile = null;
        if (userProfileService != null) {
            try {
                Map<String, Object> userProfileMap = userProfileService.lookup(userId);
                if (userProfileMap == null) {
                    logger.info("We were unable to get a user profile map from the UserProfileService.");
                } else if (UserProfileUtils.isValidUserProfileMap(userProfileMap)) {
                    userProfile = UserProfileUtils.convertMapToUserProfile(userProfileMap);
                } else {
                    logger.warn("The UserProfileService returned an invalid map.");
                }
            } catch (Exception exception) {
                logger.error(exception.getMessage());
            }
        }
        return userProfile;
    }

    /**
     * Get the {@link Variation} that has been stored for the user in the {@link UserProfileService} implementation.
     *
     * @param experiment  {@link Experiment} in which the user was bucketed.
     * @param userProfile {@link UserProfile} of the user.
     * @return null if the {@link UserProfileService} implementation is null or the user was not previously bucketed.
     * else return the {@link Variation} the user was previously bucketed into.
     */
    @Nullable
    private Variation getStoredVariation(@Nonnull Experiment experiment,
                                         @Nonnull UserProfile userProfile,
                                         @Nonnull ProjectConfig projectConfig) {
        // ---------- Check User Profile for Sticky Bucketing ----------
        // If a user profile instance is present then check it for a saved variation
        String experimentId = experiment.getId();
        String experimentKey = experiment.getKey();
        Decision decision = userProfile.experimentBucketMap.get(experimentId);
        if (decision != null) {
            String variationId = decision.variationId;
            Variation savedVariation = projectConfig
                .getExperimentIdMapping()
                .get(experimentId)
                .getVariationIdToVariationMap()
                .get(variationId);
            if (savedVariation != null) {
                logger.info("Returning previously activated variation \"{}\" of experiment \"{}\" " +
                        "for user \"{}\" from user profile.",
                    savedVariation.getKey(), experimentKey, userProfile.userId);
                // A variation is stored for this combined bucket id
                return savedVariation;
            } else {
                logger.info("User \"{}\" was previously bucketed into variation with ID \"{}\" for experiment \"{}\", " +
                        "but no matching variation was found for that user. We will re-bucket the user.",
                    userProfile.userId, variationId, experimentKey);
                return null;
            }
        } else {
            logger.info("No previously activated variation of experiment \"{}\" " +
                    "for user \"{}\" found in user profile.",
                experimentKey, userProfile.userId);
            return null;
        }
    }
}

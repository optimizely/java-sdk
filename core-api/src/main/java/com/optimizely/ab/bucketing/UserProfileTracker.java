/****************************************************************************
 * Copyright 2024, Optimizely, Inc. and contributors             *
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
package com.optimizely.ab.bucketing;

import com.optimizely.ab.OptimizelyRuntimeException;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.optimizelydecision.DecisionReasons;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

class UserProfileTracker {
    private UserProfileService userProfileService;
    private Logger logger;
    private UserProfile userProfile;
    private boolean profileUpdated;
    private String userId;

    UserProfileTracker(
        @Nonnull String userId,
        @Nonnull UserProfileService userProfileService,
        @Nonnull Logger logger
    ) {
        this.userId = userId;
        this.userProfileService = userProfileService;
        this.logger = logger;
        this.profileUpdated = false;
        this.userProfile = null;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void loadUserProfile(DecisionReasons reasons, ErrorHandler errorHandler) {
        try {
            Map<String, Object> userProfileMap = userProfileService.lookup(userId);
            if (userProfileMap == null) {
                String message = reasons.addInfo("We were unable to get a user profile map from the UserProfileService.");
                logger.info(message);
            } else if (UserProfileUtils.isValidUserProfileMap(userProfileMap)) {
                userProfile = UserProfileUtils.convertMapToUserProfile(userProfileMap);
            } else {
                String message = reasons.addInfo("The UserProfileService returned an invalid map.");
                logger.warn(message);
            }
        } catch (Exception exception) {
            String message = reasons.addInfo(exception.getMessage());
            logger.error(message);
            errorHandler.handleError(new OptimizelyRuntimeException(exception));
        }

        if (userProfile == null) {
            userProfile = new UserProfile(userId, new HashMap<String, Decision>());
        }
    }

    public void updateUserProfile(@Nonnull Experiment experiment,
                                  @Nonnull Variation variation) {
        String experimentId = experiment.getId();
        String variationId = variation.getId();
        Decision decision;
        if (userProfile.experimentBucketMap.containsKey(experimentId)) {
            decision = userProfile.experimentBucketMap.get(experimentId);
            decision.variationId = variationId;
        } else {
            decision = new Decision(variationId);
        }
        userProfile.experimentBucketMap.put(experimentId, decision);
        profileUpdated = true;
        logger.info("Updated variation \"{}\" of experiment \"{}\" for user \"{}\".",
            variationId, experimentId, userProfile.userId);
    }

    public void saveUserProfile(ErrorHandler errorHandler) {
        // if there were no updates, no need to save
        if (!this.profileUpdated) {
            return;
        }

        try {
            userProfileService.save(userProfile.toMap());
            logger.info("Saved user profile of user \"{}\".",
                userProfile.userId);
        } catch (Exception exception) {
            logger.warn("Failed to save user profile of user \"{}\".",
                userProfile.userId);
            errorHandler.handleError(new OptimizelyRuntimeException(exception));
        }
    }
}
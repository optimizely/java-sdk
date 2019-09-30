/****************************************************************************
 * Copyright 2017-2019, Optimizely, Inc. and contributors                        *
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
import com.optimizely.ab.config.*;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.decision.experiment.ExperimentService;
import com.optimizely.ab.decision.experiment.services.ExperimentBucketerService;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.event.internal.UserContext;
import com.optimizely.ab.internal.ControlAttribute;

import com.optimizely.ab.internal.ExperimentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Optimizely's decision service that determines which variation of an experiment the user will be allocated to.
 *
 * The decision service contains all logic around how a user decision is made. This includes all of the following:
 *   1. Checking experiment status
 *   2. Checking whitelisting
 *   3. Checking sticky bucketing
 *   4. Checking audience targeting
 *   5. Using Murmurhash3 to bucket the user.
 */
public class DecisionService {

    private final ErrorHandler errorHandler;
    private final UserProfileService userProfileService;
    private ExperimentService experimentService;
    private static final Logger logger = LoggerFactory.getLogger(DecisionService.class);

    /**
     * Forced variations supersede any other mappings.  They are transient and are not persistent or part of
     * the actual datafile. This contains all the forced variations
     * set by the user by calling {@link DecisionService#setForcedVariation(Experiment, String, String)} (it is not the same as the
     * whitelisting forcedVariations data structure in the Experiments class).
     */
    private transient ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping = new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();


    /**
     * Initialize a decision service for the Optimizely client.
     *
     * @param experimentService  Bucketer Service to allocate new users to an experiment.
     * @param errorHandler       The error handler of the Optimizely client.
     * @param userProfileService UserProfileService implementation for storing user info.
     */
    public DecisionService(@Nonnull ExperimentService experimentService,
                           @Nonnull ErrorHandler errorHandler,
                           @Nullable UserProfileService userProfileService) {
        this.experimentService = experimentService;
        this.errorHandler = errorHandler;
        this.userProfileService = userProfileService;
    }

    /**
     * Get a {@link Variation} of an {@link Experiment} for a user to be allocated into.
     *
     * @param experiment         The Experiment the user will be bucketed into.
     * @param userId             The userId of the user.
     * @param filteredAttributes The user's attributes. This should be filtered to just attributes in the Datafile.
     * @return The {@link Variation} the user is allocated into.
     */
    @Nullable
    public Variation getVariation(@Nonnull Experiment experiment,
                                  @Nonnull String userId,
                                  @Nonnull Map<String, ?> filteredAttributes,
                                  @Nonnull ProjectConfig projectConfig) {

        if (!ExperimentUtils.isExperimentActive(experiment)) {
            return null;
        }

        // look for forced bucketing first.
        Variation variation = getForcedVariation(experiment, userId);

        // check for whitelisting
        if (variation == null) {
            variation = getWhitelistedVariation(experiment, userId);
        }

        if (variation != null) {
            return variation;
        }

        // fetch the user profile map from the user profile service
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
                errorHandler.handleError(new OptimizelyRuntimeException(exception));
            }
        }

        // check if user exists in user profile
        if (userProfile != null) {
            variation = getStoredVariation(experiment, userProfile, projectConfig);
            // return the stored variation if it exists
            if (variation != null) {
                return variation;
            }
        } else { // if we could not find a user profile, make a new one
            userProfile = new UserProfile(userId, new HashMap<String, Decision>());
        }

        UserContext userContext = new UserContext.Builder()
            .withProjectConfig(projectConfig)
            .withAttributes(filteredAttributes)
            .withUserId(userId)
            .build();

        variation = experimentService.getDecision(experiment, userContext);

        if (variation != null) {
            if (userProfileService != null) {
                saveVariation(experiment, variation, userProfile);
            } else {
                logger.debug("This decision will not be saved since the UserProfileService is null.");
            }
            return variation;
        }
        else {
            logger.info("User \"{}\" does not meet conditions to be in experiment \"{}\".",
                userId,
                experiment.getKey());
            return null;
        }
    }

    /**
     * Get the variation the user is bucketed into for the FeatureFlag
     *
     * @param featureFlag        The feature flag the user wants to access.
     * @param userId             User Identifier
     * @param filteredAttributes A map of filtered attributes.
     * @return {@link FeatureDecision}
     */
    @Nonnull
    public FeatureDecision getVariationForFeature(@Nonnull FeatureFlag featureFlag,
                                                  @Nonnull String userId,
                                                  @Nonnull Map<String, ?> filteredAttributes,
                                                  @Nonnull ProjectConfig projectConfig) {
        if (!featureFlag.getExperimentIds().isEmpty()) {
            for (String experimentId : featureFlag.getExperimentIds()) {
                Experiment experiment = projectConfig.getExperimentIdMapping().get(experimentId);
                Variation variation = getVariation(experiment, userId, filteredAttributes, projectConfig);
                if (variation != null) {
                    return new FeatureDecision(experiment, variation, FeatureDecision.DecisionSource.FEATURE_TEST);
                }
            }
        } else {
            logger.info("The feature flag \"{}\" is not used in any experiments.", featureFlag.getKey());
        }

        FeatureDecision featureDecision = getVariationForFeatureInRollout(featureFlag, userId, filteredAttributes, projectConfig);
        if (featureDecision.variation == null) {
            logger.info("The user \"{}\" was not bucketed into a rollout for feature flag \"{}\".",
                userId, featureFlag.getKey());
        } else {
            logger.info("The user \"{}\" was bucketed into a rollout for feature flag \"{}\".",
                userId, featureFlag.getKey());
        }
        return featureDecision;
    }

    /**
     * Try to bucket the user into a rollout rule.
     * Evaluate the user for rules in priority order by seeing if the user satisfies the audience.
     * Fall back onto the everyone else rule if the user is ever excluded from a rule due to traffic allocation.
     *
     * @param featureFlag        The feature flag the user wants to access.
     * @param userId             User Identifier
     * @param filteredAttributes A map of filtered attributes.
     * @return {@link FeatureDecision}
     */
    @Nonnull
    FeatureDecision getVariationForFeatureInRollout(@Nonnull FeatureFlag featureFlag,
                                                    @Nonnull String userId,
                                                    @Nonnull Map<String, ?> filteredAttributes,
                                                    @Nonnull ProjectConfig projectConfig) {
        // use rollout to get variation for feature
        if (featureFlag.getRolloutId().isEmpty()) {
            logger.info("The feature flag \"{}\" is not used in a rollout.", featureFlag.getKey());
            return new FeatureDecision(null, null, null);
        }
        Rollout rollout = projectConfig.getRolloutIdMapping().get(featureFlag.getRolloutId());
        if (rollout == null) {
            logger.error("The rollout with id \"{}\" was not found in the datafile for feature flag \"{}\".",
                featureFlag.getRolloutId(), featureFlag.getKey());
            return new FeatureDecision(null, null, null);
        }

        // for all rules before the everyone else rule
        int rolloutRulesLength = rollout.getExperiments().size();
        String bucketingId = getBucketingId(userId, filteredAttributes);
        Variation variation;
        for (int i = 0; i < rolloutRulesLength - 1; i++) {
            Experiment rolloutRule = rollout.getExperiments().get(i);
            Audience audience = projectConfig.getAudienceIdMapping().get(rolloutRule.getAudienceIds().get(0));

            UserContext userContext = new UserContext.Builder()
                .withProjectConfig(projectConfig)
                .withAttributes(filteredAttributes)
                .withUserId(bucketingId)
                .build();


            if (ExperimentUtils.isUserInExperiment(projectConfig, rolloutRule, filteredAttributes)) {
                variation = experimentService.getDecision(rolloutRule, userContext);
                if (variation == null) {
                    break;
                }
                return new FeatureDecision(rolloutRule, variation,
                    FeatureDecision.DecisionSource.ROLLOUT);
            } else {
                logger.debug("User \"{}\" did not meet the conditions to be in rollout rule for audience \"{}\".",
                    userId, audience.getName());
            }
        }

        // get last rule which is the fall back rule
        Experiment finalRule = rollout.getExperiments().get(rolloutRulesLength - 1);
        if (ExperimentUtils.isUserInExperiment(projectConfig, finalRule, filteredAttributes)) {
            UserContext userContext = new UserContext.Builder()
                .withProjectConfig(projectConfig)
                .withAttributes(filteredAttributes)
                .withUserId(bucketingId)
                .build();
            variation = experimentService.getDecision(finalRule, userContext);
            if (variation != null) {
                return new FeatureDecision(finalRule, variation,
                    FeatureDecision.DecisionSource.ROLLOUT);
            }
        }
        return new FeatureDecision(null, null, null);
    }

    /**
     * Get the variation the user has been whitelisted into.
     *
     * @param experiment {@link Experiment} in which user is to be bucketed.
     * @param userId     User Identifier
     * @return null if the user is not whitelisted into any variation
     * {@link Variation} the user is bucketed into if the user has a specified whitelisted variation.
     */
    @Nullable
    Variation getWhitelistedVariation(@Nonnull Experiment experiment, @Nonnull String userId) {
        // if a user has a forced variation mapping, return the respective variation
        Map<String, String> userIdToVariationKeyMap = experiment.getUserIdToVariationKeyMap();
        if (userIdToVariationKeyMap.containsKey(userId)) {
            String forcedVariationKey = userIdToVariationKeyMap.get(userId);
            Variation forcedVariation = experiment.getVariationKeyToVariationMap().get(forcedVariationKey);
            if (forcedVariation != null) {
                logger.info("User \"{}\" is forced in variation \"{}\".", userId, forcedVariationKey);
            } else {
                logger.error("Variation \"{}\" is not in the datafile. Not activating user \"{}\".",
                    forcedVariationKey, userId);
            }
            return forcedVariation;
        }
        return null;
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
    Variation getStoredVariation(@Nonnull Experiment experiment,
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

    /**
     * Save a {@link Variation} of an {@link Experiment} for a user in the {@link UserProfileService}.
     *
     * @param experiment  The experiment the user was buck
     * @param variation   The Variation to save.
     * @param userProfile A {@link UserProfile} instance of the user information.
     */
    void saveVariation(@Nonnull Experiment experiment,
                       @Nonnull Variation variation,
                       @Nonnull UserProfile userProfile) {
        // only save if the user has implemented a user profile service
        if (userProfileService != null) {
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

            try {
                userProfileService.save(userProfile.toMap());
                logger.info("Saved variation \"{}\" of experiment \"{}\" for user \"{}\".",
                    variationId, experimentId, userProfile.userId);
            } catch (Exception exception) {
                logger.warn("Failed to save variation \"{}\" of experiment \"{}\" for user \"{}\".",
                    variationId, experimentId, userProfile.userId);
                errorHandler.handleError(new OptimizelyRuntimeException(exception));
            }
        }
    }

    /**
     * Get the bucketingId of a user if a bucketingId exists in attributes, or else default to userId.
     *
     * @param userId             The userId of the user.
     * @param filteredAttributes The user's attributes. This should be filtered to just attributes in the Datafile.
     * @return bucketingId if it is a String type in attributes.
     * else return userId
     */
    String getBucketingId(@Nonnull String userId,
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

    public ConcurrentHashMap<String, ConcurrentHashMap<String, String>> getForcedVariationMapping() {
        return forcedVariationMapping;
    }

    /**
     * Force a user into a variation for a given experiment.
     * The forced variation value does not persist across application launches.
     * If the experiment key is not in the project file, this call fails and returns false.
     *
     * @param experiment    The experiment to override.
     * @param userId        The user ID to be used for bucketing.
     * @param variationKey  The variation key to force the user into.  If the variation key is null
     *                      then the forcedVariation for that experiment is removed.
     * @return boolean A boolean value that indicates if the set completed successfully.
     */
    public boolean setForcedVariation(@Nonnull Experiment experiment,
                                      @Nonnull String userId,
                                      @Nullable String variationKey) {



        Variation variation = null;

        // keep in mind that you can pass in a variationKey that is null if you want to
        // remove the variation.
        if (variationKey != null) {
            variation = experiment.getVariationKeyToVariationMap().get(variationKey);
            // if the variation is not part of the experiment, return false.
            if (variation == null) {
                logger.error("Variation {} does not exist for experiment {}", variationKey, experiment.getKey());
                return false;
            }
        }

        // if the user id is invalid, return false.
        if (!validateUserId(userId)) {
            return false;
        }

        ConcurrentHashMap<String, String> experimentToVariation;
        if (!forcedVariationMapping.containsKey(userId)) {
            forcedVariationMapping.putIfAbsent(userId, new ConcurrentHashMap<String, String>());
        }
        experimentToVariation = forcedVariationMapping.get(userId);

        boolean retVal = true;
        // if it is null remove the variation if it exists.
        if (variationKey == null) {
            String removedVariationId = experimentToVariation.remove(experiment.getId());
            if (removedVariationId != null) {
                Variation removedVariation = experiment.getVariationIdToVariationMap().get(removedVariationId);
                if (removedVariation != null) {
                    logger.debug("Variation mapped to experiment \"{}\" has been removed for user \"{}\"", experiment.getKey(), userId);
                } else {
                    logger.debug("Removed forced variation that did not exist in experiment");
                }
            } else {
                logger.debug("No variation for experiment {}", experiment.getKey());
                retVal = false;
            }
        } else {
            String previous = experimentToVariation.put(experiment.getId(), variation.getId());
            logger.debug("Set variation \"{}\" for experiment \"{}\" and user \"{}\" in the forced variation map.",
                variation.getKey(), experiment.getKey(), userId);
            if (previous != null) {
                Variation previousVariation = experiment.getVariationIdToVariationMap().get(previous);
                if (previousVariation != null) {
                    logger.debug("forced variation {} replaced forced variation {} in forced variation map.",
                        variation.getKey(), previousVariation.getKey());
                }
            }
        }

        return retVal;
    }

    /**
     * Gets the forced variation for a given user and experiment.
     *
     * @param experiment    The experiment forced.
     * @param userId        The user ID to be used for bucketing.
     * @return The variation the user was bucketed into. This value can be null if the
     * forced variation fails.
     */
    @Nullable
    public Variation getForcedVariation(@Nonnull Experiment experiment,
                                        @Nonnull String userId) {

        // if the user id is invalid, return false.
        if (!validateUserId(userId)) {
            return null;
        }

        Map<String, String> experimentToVariation = getForcedVariationMapping().get(userId);
        if (experimentToVariation != null) {
            String variationId = experimentToVariation.get(experiment.getId());
            if (variationId != null) {
                Variation variation = experiment.getVariationIdToVariationMap().get(variationId);
                if (variation != null) {
                    logger.debug("Variation \"{}\" is mapped to experiment \"{}\" and user \"{}\" in the forced variation map",
                        variation.getKey(), experiment.getKey(), userId);
                    return variation;
                }
            } else {
                logger.debug("No variation for experiment \"{}\" mapped to user \"{}\" in the forced variation map ", experiment.getKey(), userId);
            }
        }
        return null;
    }

    /**
     * Helper function to check that the provided userId is valid
     *
     * @param userId the userId being validated
     * @return whether the user ID is valid
     */
    private boolean validateUserId(String userId) {
        if (userId == null) {
            logger.error("User ID is invalid");
            return false;
        }

        return true;
    }
}
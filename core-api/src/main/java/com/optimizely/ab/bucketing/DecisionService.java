/****************************************************************************
 * Copyright 2017-2020, Optimizely, Inc. and contributors                        *
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
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.internal.ControlAttribute;
import com.optimizely.ab.internal.ExperimentUtils;
import com.optimizely.ab.optimizelydecision.DecisionReasons;
import com.optimizely.ab.optimizelydecision.DefaultDecisionReasons;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.optimizely.ab.internal.LoggingConstants.LoggingEntityType.EXPERIMENT;
import static com.optimizely.ab.internal.LoggingConstants.LoggingEntityType.RULE;

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

    private final Bucketer bucketer;
    private final ErrorHandler errorHandler;
    private final UserProfileService userProfileService;
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
     * @param bucketer           Base bucketer to allocate new users to an experiment.
     * @param errorHandler       The error handler of the Optimizely client.
     * @param userProfileService UserProfileService implementation for storing user info.
     */
    public DecisionService(@Nonnull Bucketer bucketer,
                           @Nonnull ErrorHandler errorHandler,
                           @Nullable UserProfileService userProfileService) {
        this.bucketer = bucketer;
        this.errorHandler = errorHandler;
        this.userProfileService = userProfileService;
    }

    /**
     * Get a {@link Variation} of an {@link Experiment} for a user to be allocated into.
     *
     * @param experiment         The Experiment the user will be bucketed into.
     * @param userId             The userId of the user.
     * @param filteredAttributes The user's attributes. This should be filtered to just attributes in the Datafile.
     * @param projectConfig      The current projectConfig
     * @param options            An array of decision options
     * @param reasons            Decision log messages
     * @return The {@link Variation} the user is allocated into.
     */
    @Nullable
    public Variation getVariation(@Nonnull Experiment experiment,
                                  @Nonnull String userId,
                                  @Nonnull Map<String, ?> filteredAttributes,
                                  @Nonnull ProjectConfig projectConfig,
                                  @Nonnull List<OptimizelyDecideOption> options,
                                  @Nonnull DecisionReasons reasons) {
        if (!ExperimentUtils.isExperimentActive(experiment, reasons)) {
            return null;
        }

        // look for forced bucketing first.
        Variation variation = getForcedVariation(experiment, userId, reasons);

        // check for whitelisting
        if (variation == null) {
            variation = getWhitelistedVariation(experiment, userId, reasons);
        }

        if (variation != null) {
            return variation;
        }

        // fetch the user profile map from the user profile service
        boolean ignoreUPS = options.contains(OptimizelyDecideOption.IGNORE_USER_PROFILE_SERVICE);
        UserProfile userProfile = null;

        if (userProfileService != null && !ignoreUPS) {
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

            // check if user exists in user profile
            if (userProfile != null) {
                variation = getStoredVariation(experiment, userProfile, projectConfig, reasons);
                // return the stored variation if it exists
                if (variation != null) {
                    return variation;
                }
            } else { // if we could not find a user profile, make a new one
                userProfile = new UserProfile(userId, new HashMap<String, Decision>());
            }
        }

        if (ExperimentUtils.doesUserMeetAudienceConditions(projectConfig, experiment, filteredAttributes, EXPERIMENT, experiment.getKey(), reasons)) {
            String bucketingId = getBucketingId(userId, filteredAttributes, reasons);
            variation = bucketer.bucket(experiment, bucketingId, projectConfig, reasons);

            if (variation != null) {
                if (userProfileService != null && !ignoreUPS) {
                    saveVariation(experiment, variation, userProfile, reasons);
                } else {
                    logger.debug("This decision will not be saved since the UserProfileService is null.");
                }
            }

            return variation;
        }

        String message = reasons.addInfo("User \"%s\" does not meet conditions to be in experiment \"%s\".", userId, experiment.getKey());
        logger.info(message);
        return null;
    }

    @Nullable
    public Variation getVariation(@Nonnull Experiment experiment,
                                  @Nonnull String userId,
                                  @Nonnull Map<String, ?> filteredAttributes,
                                  @Nonnull ProjectConfig projectConfig) {
        return getVariation(experiment, userId, filteredAttributes, projectConfig, Collections.emptyList(), DefaultDecisionReasons.newInstance());
    }

    /**
     * Get the variation the user is bucketed into for the FeatureFlag
     *
     * @param featureFlag        The feature flag the user wants to access.
     * @param userId             User Identifier
     * @param filteredAttributes A map of filtered attributes.
     * @param projectConfig      The current projectConfig
     * @param options            An array of decision options
     * @param reasons            Decision log messages
     * @return {@link FeatureDecision}
     */
    @Nonnull
    public FeatureDecision getVariationForFeature(@Nonnull FeatureFlag featureFlag,
                                                  @Nonnull String userId,
                                                  @Nonnull Map<String, ?> filteredAttributes,
                                                  @Nonnull ProjectConfig projectConfig,
                                                  @Nonnull List<OptimizelyDecideOption> options,
                                                  @Nonnull DecisionReasons reasons) {
        if (!featureFlag.getExperimentIds().isEmpty()) {
            for (String experimentId : featureFlag.getExperimentIds()) {
                Experiment experiment = projectConfig.getExperimentIdMapping().get(experimentId);
                Variation variation = getVariation(experiment, userId, filteredAttributes, projectConfig, options, reasons);
                if (variation != null) {
                    return new FeatureDecision(experiment, variation, FeatureDecision.DecisionSource.FEATURE_TEST);
                }
            }
        } else {
            String message = reasons.addInfo("The feature flag \"%s\" is not used in any experiments.", featureFlag.getKey());
            logger.info(message);
        }

        FeatureDecision featureDecision = getVariationForFeatureInRollout(featureFlag, userId, filteredAttributes, projectConfig, reasons);
        if (featureDecision.variation == null) {
            String message = reasons.addInfo("The user \"%s\" was not bucketed into a rollout for feature flag \"%s\".",
                userId, featureFlag.getKey());
            logger.info(message);
        } else {
            String message = reasons.addInfo("The user \"%s\" was bucketed into a rollout for feature flag \"%s\".",
                userId, featureFlag.getKey());
            logger.info(message);
        }
        return featureDecision;
    }

    @Nonnull
    public FeatureDecision getVariationForFeature(@Nonnull FeatureFlag featureFlag,
                                                  @Nonnull String userId,
                                                  @Nonnull Map<String, ?> filteredAttributes,
                                                  @Nonnull ProjectConfig projectConfig) {

        return getVariationForFeature(featureFlag, userId, filteredAttributes, projectConfig,Collections.emptyList(), DefaultDecisionReasons.newInstance());
    }

    /**
     * Try to bucket the user into a rollout rule.
     * Evaluate the user for rules in priority order by seeing if the user satisfies the audience.
     * Fall back onto the everyone else rule if the user is ever excluded from a rule due to traffic allocation.
     *
     * @param featureFlag        The feature flag the user wants to access.
     * @param userId             User Identifier
     * @param filteredAttributes A map of filtered attributes.
     * @param projectConfig      The current projectConfig
     * @param reasons            Decision log messages
     * @return {@link FeatureDecision}
     */
    @Nonnull
    FeatureDecision getVariationForFeatureInRollout(@Nonnull FeatureFlag featureFlag,
                                                    @Nonnull String userId,
                                                    @Nonnull Map<String, ?> filteredAttributes,
                                                    @Nonnull ProjectConfig projectConfig,
                                                    @Nonnull DecisionReasons reasons) {
        // use rollout to get variation for feature
        if (featureFlag.getRolloutId().isEmpty()) {
            String message = reasons.addInfo("The feature flag \"%s\" is not used in a rollout.", featureFlag.getKey());
            logger.info(message);
            return new FeatureDecision(null, null, null);
        }
        Rollout rollout = projectConfig.getRolloutIdMapping().get(featureFlag.getRolloutId());
        if (rollout == null) {
            String message = reasons.addInfo("The rollout with id \"%s\" was not found in the datafile for feature flag \"%s\".",
                featureFlag.getRolloutId(), featureFlag.getKey());
            logger.error(message);
            return new FeatureDecision(null, null, null);
        }

        // for all rules before the everyone else rule
        int rolloutRulesLength = rollout.getExperiments().size();
        String bucketingId = getBucketingId(userId, filteredAttributes, reasons);
        Variation variation;
        for (int i = 0; i < rolloutRulesLength - 1; i++) {
            Experiment rolloutRule = rollout.getExperiments().get(i);
            if (ExperimentUtils.doesUserMeetAudienceConditions(projectConfig, rolloutRule, filteredAttributes, RULE, Integer.toString(i + 1), reasons)) {
                variation = bucketer.bucket(rolloutRule, bucketingId, projectConfig, reasons);
                if (variation == null) {
                    break;
                }
                return new FeatureDecision(rolloutRule, variation,
                    FeatureDecision.DecisionSource.ROLLOUT);
            } else {
                String message = reasons.addInfo("User \"%s\" does not meet conditions for targeting rule \"%d\".", userId, i + 1);
                logger.debug(message);
            }
        }

        // get last rule which is the fall back rule
        Experiment finalRule = rollout.getExperiments().get(rolloutRulesLength - 1);
        if (ExperimentUtils.doesUserMeetAudienceConditions(projectConfig, finalRule, filteredAttributes, RULE, "Everyone Else", reasons)) {
            variation = bucketer.bucket(finalRule, bucketingId, projectConfig, reasons);
            if (variation != null) {
                String message = reasons.addInfo("User \"%s\" meets conditions for targeting rule \"Everyone Else\".", userId);
                logger.debug(message);
                return new FeatureDecision(finalRule, variation,
                    FeatureDecision.DecisionSource.ROLLOUT);
            }
        }
        return new FeatureDecision(null, null, null);
    }

    @Nonnull
    FeatureDecision getVariationForFeatureInRollout(@Nonnull FeatureFlag featureFlag,
                                                    @Nonnull String userId,
                                                    @Nonnull Map<String, ?> filteredAttributes,
                                                    @Nonnull ProjectConfig projectConfig) {
        return getVariationForFeatureInRollout(featureFlag, userId, filteredAttributes, projectConfig, DefaultDecisionReasons.newInstance());
    }

    /**
     * Get the variation the user has been whitelisted into.
     *
     * @param experiment {@link Experiment} in which user is to be bucketed.
     * @param userId     User Identifier
     * @param reasons            Decision log messages
     * @return null if the user is not whitelisted into any variation
     * {@link Variation} the user is bucketed into if the user has a specified whitelisted variation.
     */
    @Nullable
    Variation getWhitelistedVariation(@Nonnull Experiment experiment,
                                      @Nonnull String userId,
                                      @Nonnull DecisionReasons reasons) {
        // if a user has a forced variation mapping, return the respective variation
        Map<String, String> userIdToVariationKeyMap = experiment.getUserIdToVariationKeyMap();
        if (userIdToVariationKeyMap.containsKey(userId)) {
            String forcedVariationKey = userIdToVariationKeyMap.get(userId);
            Variation forcedVariation = experiment.getVariationKeyToVariationMap().get(forcedVariationKey);
            if (forcedVariation != null) {
                String message = reasons.addInfo("User \"%s\" is forced in variation \"%s\".", userId, forcedVariationKey);
                logger.info(message);
            } else {
                String message = reasons.addInfo("Variation \"%s\" is not in the datafile. Not activating user \"%s\".",
                    forcedVariationKey, userId);
                logger.error(message);
            }
            return forcedVariation;
        }
        return null;
    }

    @Nullable
    Variation getWhitelistedVariation(@Nonnull Experiment experiment,
                                      @Nonnull String userId) {

        return getWhitelistedVariation(experiment, userId, DefaultDecisionReasons.newInstance());
    }

    /**
     * Get the {@link Variation} that has been stored for the user in the {@link UserProfileService} implementation.
     *
     * @param experiment  {@link Experiment} in which the user was bucketed.
     * @param userProfile {@link UserProfile} of the user.
     * @param projectConfig      The current projectConfig
     * @param reasons            Decision log messages
     * @return null if the {@link UserProfileService} implementation is null or the user was not previously bucketed.
     * else return the {@link Variation} the user was previously bucketed into.
     */
    @Nullable
    Variation getStoredVariation(@Nonnull Experiment experiment,
                                 @Nonnull UserProfile userProfile,
                                 @Nonnull ProjectConfig projectConfig,
                                 @Nonnull DecisionReasons reasons) {

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
                String message = reasons.addInfo("Returning previously activated variation \"%s\" of experiment \"%s\" for user \"%s\" from user profile.",
                    savedVariation.getKey(), experimentKey, userProfile.userId);
                logger.info(message);
                // A variation is stored for this combined bucket id
                return savedVariation;
            } else {
                String message = reasons.addInfo("User \"%s\" was previously bucketed into variation with ID \"%s\" for experiment \"%s\", but no matching variation was found for that user. We will re-bucket the user.",
                    userProfile.userId, variationId, experimentKey);
                logger.info(message);
                return null;
            }
        } else {
            String message = reasons.addInfo("No previously activated variation of experiment \"%s\" for user \"%s\" found in user profile.",
                experimentKey, userProfile.userId);
            logger.info(message);
            return null;
        }
    }

    @Nullable
    Variation getStoredVariation(@Nonnull Experiment experiment,
                                 @Nonnull UserProfile userProfile,
                                 @Nonnull ProjectConfig projectConfig) {
        return getStoredVariation(experiment, userProfile, projectConfig, DefaultDecisionReasons.newInstance());
    }

    /**
     * Save a {@link Variation} of an {@link Experiment} for a user in the {@link UserProfileService}.
     *
     * @param experiment  The experiment the user was buck
     * @param variation   The Variation to save.
     * @param userProfile A {@link UserProfile} instance of the user information.
     * @param reasons     Decision log messages
     */
    void saveVariation(@Nonnull Experiment experiment,
                       @Nonnull Variation variation,
                       @Nonnull UserProfile userProfile,
                       @Nonnull DecisionReasons reasons) {

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

    void saveVariation(@Nonnull Experiment experiment,
                       @Nonnull Variation variation,
                       @Nonnull UserProfile userProfile) {
        saveVariation(experiment, variation, userProfile, DefaultDecisionReasons.newInstance());
    }

    /**
     * Get the bucketingId of a user if a bucketingId exists in attributes, or else default to userId.
     *
     * @param userId             The userId of the user.
     * @param filteredAttributes The user's attributes. This should be filtered to just attributes in the Datafile.
     * @param reasons            Decision log messages
     * @return bucketingId if it is a String type in attributes.
     * else return userId
     */
    String getBucketingId(@Nonnull String userId,
                          @Nonnull Map<String, ?> filteredAttributes,
                          @Nonnull DecisionReasons reasons) {
        String bucketingId = userId;
        if (filteredAttributes != null && filteredAttributes.containsKey(ControlAttribute.BUCKETING_ATTRIBUTE.toString())) {
            if (String.class.isInstance(filteredAttributes.get(ControlAttribute.BUCKETING_ATTRIBUTE.toString()))) {
                bucketingId = (String) filteredAttributes.get(ControlAttribute.BUCKETING_ATTRIBUTE.toString());
                logger.debug("BucketingId is valid: \"{}\"", bucketingId);
            } else {
                String message = reasons.addInfo("BucketingID attribute is not a string. Defaulted to userId");
                logger.warn(message);
            }
        }
        return bucketingId;
    }

    String getBucketingId(@Nonnull String userId,
                          @Nonnull Map<String, ?> filteredAttributes) {
        return getBucketingId(userId, filteredAttributes, DefaultDecisionReasons.newInstance());
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
     * @param reasons       Decision log messages
     * @return The variation the user was bucketed into. This value can be null if the
     * forced variation fails.
     */
    @Nullable
    public Variation getForcedVariation(@Nonnull Experiment experiment,
                                        @Nonnull String userId,
                                        @Nonnull DecisionReasons reasons) {
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
                    String message = reasons.addInfo("Variation \"%s\" is mapped to experiment \"%s\" and user \"%s\" in the forced variation map",
                        variation.getKey(), experiment.getKey(), userId);
                    logger.debug(message);
                    return variation;
                }
            } else {
                logger.debug("No variation for experiment \"{}\" mapped to user \"{}\" in the forced variation map ", experiment.getKey(), userId);
            }
        }
        return null;
    }

    @Nullable
    public Variation getForcedVariation(@Nonnull Experiment experiment,
                                        @Nonnull String userId) {
        return getForcedVariation(experiment, userId, DefaultDecisionReasons.newInstance());
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

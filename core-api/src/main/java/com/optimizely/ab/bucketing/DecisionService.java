/****************************************************************************
 * Copyright 2017-2022, 2024, Optimizely, Inc. and contributors             *
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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.optimizely.ab.cmab.service.CmabDecision;
import com.optimizely.ab.cmab.service.CmabService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.optimizely.ab.OptimizelyDecisionContext;
import com.optimizely.ab.OptimizelyForcedDecision;
import com.optimizely.ab.OptimizelyRuntimeException;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.Holdout;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Rollout;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.internal.ControlAttribute;
import com.optimizely.ab.internal.ExperimentUtils;
import static com.optimizely.ab.internal.LoggingConstants.LoggingEntityType.EXPERIMENT;
import static com.optimizely.ab.internal.LoggingConstants.LoggingEntityType.RULE;
import com.optimizely.ab.optimizelydecision.DecisionReasons;
import com.optimizely.ab.optimizelydecision.DecisionResponse;
import com.optimizely.ab.optimizelydecision.DefaultDecisionReasons;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;

/**
 * Optimizely's decision service that determines which variation of an experiment the user will be allocated to.
 *
 * The decision service contains all logic around how a user decision is made. This includes all of the following:
 *   1. Checking experiment status
 *   2. Checking whitelisting
 *   3. Checking sticky bucketing
 *   4. Checking audience targeting
 *   5. Using Murmurhash3 to bucket the user.
 *   6. Handling CMAB (Contextual Multi-Armed Bandit) experiments for dynamic variation selection
 */
public class DecisionService {

    private final Bucketer bucketer;
    private final ErrorHandler errorHandler;
    private final UserProfileService userProfileService;
    private final CmabService cmabService;
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
     * @param cmabService        Cmab Service for decision making.
     */
    public DecisionService(@Nonnull Bucketer bucketer,
                           @Nonnull ErrorHandler errorHandler,
                           @Nullable UserProfileService userProfileService,
                           @Nonnull CmabService cmabService) {
        this.bucketer = bucketer;
        this.errorHandler = errorHandler;
        this.userProfileService = userProfileService;
        this.cmabService = cmabService;
    }

    /**
     * Get a {@link Variation} of an {@link Experiment} for a user to be allocated into.
     *
     * @param experiment           The Experiment the user will be bucketed into.
     * @param user                 The current OptimizelyUserContext
     * @param projectConfig        The current projectConfig
     * @param options              An array of decision options
     * @param userProfileTracker   tracker for reading and updating user profile of the user
     * @param reasons              Decision reasons
     * @param decisionPath         An enum of paths for decision-making logic
     * @return A {@link DecisionResponse} including the {@link Variation} that user is bucketed into (or null) and the decision reasons
     */
    @Nonnull
    public DecisionResponse<Variation> getVariation(@Nonnull Experiment experiment,
                                                    @Nonnull OptimizelyUserContext user,
                                                    @Nonnull ProjectConfig projectConfig,
                                                    @Nonnull List<OptimizelyDecideOption> options,
                                                    @Nullable UserProfileTracker userProfileTracker,
                                                    @Nullable DecisionReasons reasons,
                                                    @Nonnull DecisionPath decisionPath) {
        if (reasons == null) {
            reasons = DefaultDecisionReasons.newInstance();
        }

        if (!ExperimentUtils.isExperimentActive(experiment)) {
            String message = reasons.addInfo("Experiment \"%s\" is not running.", experiment.getKey());
            logger.info(message);
            return new DecisionResponse(null, reasons);
        }

        // look for forced bucketing first.
        DecisionResponse<Variation> decisionVariation = getForcedVariation(experiment, user.getUserId());
        reasons.merge(decisionVariation.getReasons());
        Variation variation = decisionVariation.getResult();

        // check for whitelisting
        if (variation == null) {
            decisionVariation = getWhitelistedVariation(experiment, user.getUserId());
            reasons.merge(decisionVariation.getReasons());
            variation = decisionVariation.getResult();
        }

        if (variation != null) {
            return new DecisionResponse(variation, reasons);
        }

        if (userProfileTracker != null) {
            decisionVariation = getStoredVariation(experiment, userProfileTracker.getUserProfile(), projectConfig);
            reasons.merge(decisionVariation.getReasons());
            variation = decisionVariation.getResult();
            // return the stored variation if it exists
            if (variation != null) {
                return new DecisionResponse(variation, reasons);
            }
        }

        DecisionResponse<Boolean> decisionMeetAudience = ExperimentUtils.doesUserMeetAudienceConditions(projectConfig, experiment, user, EXPERIMENT, experiment.getKey());
        reasons.merge(decisionMeetAudience.getReasons());
        if (decisionMeetAudience.getResult()) {
            String bucketingId = getBucketingId(user.getUserId(), user.getAttributes());
            String cmabUUID = null;
            if (decisionPath == DecisionPath.WITH_CMAB && isCmabExperiment(experiment)) {
                DecisionResponse<CmabDecision> cmabDecision = getDecisionForCmabExperiment(projectConfig, experiment, user, bucketingId, options);
                reasons.merge(cmabDecision.getReasons());

                if (cmabDecision.isError()) {
                    return new DecisionResponse<>(null, reasons, true, null);
                }

                CmabDecision cmabResult = cmabDecision.getResult();
                if (cmabResult != null) {
                    String variationId = cmabResult.getVariationId();
                    cmabUUID = cmabResult.getCmabUUID();
                    variation = experiment.getVariationIdToVariationMap().get(variationId);
                }
            } else {
                // Standard bucketing for non-CMAB experiments
                decisionVariation = bucketer.bucket(experiment, bucketingId, projectConfig);
                reasons.merge(decisionVariation.getReasons());
                variation = decisionVariation.getResult();
            }

            if (variation != null) {
                if (userProfileTracker != null) {
                    userProfileTracker.updateUserProfile(experiment, variation);
                } else {
                    logger.debug("This decision will not be saved since the UserProfileService is null.");
                }
            }

            return new DecisionResponse<>(variation, reasons, false, cmabUUID);
        }

        String message = reasons.addInfo("User \"%s\" does not meet conditions to be in experiment \"%s\".", user.getUserId(), experiment.getKey());
        logger.info(message);
        return new DecisionResponse(null, reasons);
    }

    /**
     * Get a {@link Variation} of an {@link Experiment} for a user to be allocated into.
     *
     * @param experiment         The Experiment the user will be bucketed into.
     * @param user               The current OptimizelyUserContext
     * @param projectConfig      The current projectConfig
     * @param options            An array of decision options
     * @param decisionPath       An enum of paths for decision-making logic
     * @return A {@link DecisionResponse} including the {@link Variation} that user is bucketed into (or null) and the decision reasons
     */
    @Nonnull
    public DecisionResponse<Variation> getVariation(@Nonnull Experiment experiment,
                                                    @Nonnull OptimizelyUserContext user,
                                                    @Nonnull ProjectConfig projectConfig,
                                                    @Nonnull List<OptimizelyDecideOption> options,
                                                    @Nonnull DecisionPath decisionPath) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();

        // fetch the user profile map from the user profile service
        boolean ignoreUPS = options.contains(OptimizelyDecideOption.IGNORE_USER_PROFILE_SERVICE);
        UserProfileTracker userProfileTracker = null;

        if (userProfileService != null && !ignoreUPS) {
            userProfileTracker = new UserProfileTracker(user.getUserId(), userProfileService, logger);
            userProfileTracker.loadUserProfile(reasons, errorHandler);
        }

        DecisionResponse<Variation> response = getVariation(experiment, user, projectConfig, options, userProfileTracker, reasons, decisionPath);

        if(userProfileService != null && !ignoreUPS) {
            userProfileTracker.saveUserProfile(errorHandler);
        }
        return response;
    }

    @Nonnull
    public DecisionResponse<Variation>  getVariation(@Nonnull Experiment experiment,
                                                     @Nonnull OptimizelyUserContext user,
                                                     @Nonnull ProjectConfig projectConfig) {
        return getVariation(experiment, user, projectConfig, Collections.emptyList(), DecisionPath.WITH_CMAB);
    }

    /**
     * Get the variation the user is bucketed into for the FeatureFlag
     *
     * @param featureFlag        The feature flag the user wants to access.
     * @param user               The current OptimizelyuserContext
     * @param projectConfig      The current projectConfig
     * @param options            An array of decision options
     * @return A {@link DecisionResponse} including a {@link FeatureDecision} and the decision reasons
     */
    @Nonnull
    public DecisionResponse<FeatureDecision> getVariationForFeature(@Nonnull FeatureFlag featureFlag,
                                                                    @Nonnull OptimizelyUserContext user,
                                                                    @Nonnull ProjectConfig projectConfig,
                                                                    @Nonnull List<OptimizelyDecideOption> options) {
        return getVariationsForFeatureList(Arrays.asList(featureFlag), user, projectConfig, options).get(0);
    }

    /**
     * Get the variations the user is bucketed into for the list of feature flags
     *
     * @param featureFlags        The feature flag list the user wants to access.
     * @param user                The current OptimizelyuserContext
     * @param projectConfig       The current projectConfig
     * @param options             An array of decision options
     * @return A {@link DecisionResponse} including a {@link FeatureDecision} and the decision reasons
     */
    @Nonnull
    public  List<DecisionResponse<FeatureDecision>> getVariationsForFeatureList(@Nonnull List<FeatureFlag> featureFlags,
                                                                    @Nonnull OptimizelyUserContext user,
                                                                    @Nonnull ProjectConfig projectConfig,
                                                                    @Nonnull List<OptimizelyDecideOption> options) {
        return getVariationsForFeatureList(featureFlags, user, projectConfig, options, DecisionPath.WITH_CMAB);
    }

    /**
     * Get the variations the user is bucketed into for the list of feature flags
     *
     * @param featureFlags        The feature flag list the user wants to access.
     * @param user                The current OptimizelyuserContext
     * @param projectConfig       The current projectConfig
     * @param options             An array of decision options
     * @param decisionPath        An enum of paths for decision-making logic
     * @return A {@link DecisionResponse} including a {@link FeatureDecision} and the decision reasons
     */
    @Nonnull
    public  List<DecisionResponse<FeatureDecision>> getVariationsForFeatureList(@Nonnull List<FeatureFlag> featureFlags,
                                                                    @Nonnull OptimizelyUserContext user,
                                                                    @Nonnull ProjectConfig projectConfig,
                                                                    @Nonnull List<OptimizelyDecideOption> options,
                                                                    @Nonnull DecisionPath decisionPath) {
        DecisionReasons upsReasons = DefaultDecisionReasons.newInstance();

        boolean ignoreUPS = options.contains(OptimizelyDecideOption.IGNORE_USER_PROFILE_SERVICE);
        UserProfileTracker userProfileTracker = null;

        if (userProfileService != null && !ignoreUPS) {
            userProfileTracker = new UserProfileTracker(user.getUserId(), userProfileService, logger);
            userProfileTracker.loadUserProfile(upsReasons, errorHandler);
        }

        List<DecisionResponse<FeatureDecision>> decisions = new ArrayList<>();

        flagLoop: for (FeatureFlag featureFlag: featureFlags) {
            DecisionReasons reasons = DefaultDecisionReasons.newInstance();
            reasons.merge(upsReasons);

            List<Holdout> holdouts = projectConfig.getHoldoutForFlag(featureFlag.getId());
            if (!holdouts.isEmpty()) {
                for (Holdout holdout : holdouts) {
                    DecisionResponse<Variation> holdoutDecision = getVariationForHoldout(holdout, user, projectConfig);
                    reasons.merge(holdoutDecision.getReasons());
                    if (holdoutDecision.getResult() != null) {
                        decisions.add(new DecisionResponse<>(new FeatureDecision(holdout, holdoutDecision.getResult(), FeatureDecision.DecisionSource.HOLDOUT), reasons));
                        continue flagLoop;
                    }
                }
            }

            DecisionResponse<FeatureDecision> decisionVariationResponse = getVariationFromExperiment(projectConfig, featureFlag, user, options, userProfileTracker, decisionPath);
            reasons.merge(decisionVariationResponse.getReasons());

            FeatureDecision decision = decisionVariationResponse.getResult();
            boolean error = decisionVariationResponse.isError();

            if (decision != null) {
                decisions.add(new DecisionResponse(decision, reasons, error, decision.cmabUUID));
                continue;
            }

            DecisionResponse<FeatureDecision> decisionFeatureResponse = getVariationForFeatureInRollout(featureFlag, user, projectConfig);
            reasons.merge(decisionFeatureResponse.getReasons());
            decision = decisionFeatureResponse.getResult();

            String message;
            if (decision.variation == null) {
                message = reasons.addInfo("The user \"%s\" was not bucketed into a rollout for feature flag \"%s\".",
                    user.getUserId(), featureFlag.getKey());
            } else {
                message = reasons.addInfo("The user \"%s\" was bucketed into a rollout for feature flag \"%s\".",
                    user.getUserId(), featureFlag.getKey());
            }
            logger.info(message);

            decisions.add(new DecisionResponse(decision, reasons));
        }

        if (userProfileService != null && !ignoreUPS) {
            userProfileTracker.saveUserProfile(errorHandler);
        }

        return decisions;
    }

    @Nonnull
    public DecisionResponse<FeatureDecision> getVariationForFeature(@Nonnull FeatureFlag featureFlag,
                                                                    @Nonnull OptimizelyUserContext user,
                                                                    @Nonnull ProjectConfig projectConfig) {
        return getVariationForFeature(featureFlag, user, projectConfig, Collections.emptyList());
    }

    /**
     *
     * @param projectConfig     The ProjectConfig.
     * @param featureFlag       The feature flag the user wants to access.
     * @param user              The current OptimizelyUserContext.
     * @param options           An array of decision options
     * @return A {@link DecisionResponse} including a {@link FeatureDecision} and the decision reasons
     */
    @Nonnull
    DecisionResponse<FeatureDecision> getVariationFromExperiment(@Nonnull ProjectConfig projectConfig,
                                                                 @Nonnull FeatureFlag featureFlag,
                                                                 @Nonnull OptimizelyUserContext user,
                                                                 @Nonnull List<OptimizelyDecideOption> options,
                                                                 @Nullable UserProfileTracker userProfileTracker,
                                                                 @Nonnull DecisionPath decisionPath) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();
        if (!featureFlag.getExperimentIds().isEmpty()) {
            for (String experimentId : featureFlag.getExperimentIds()) {
                Experiment experiment = projectConfig.getExperimentIdMapping().get(experimentId);

                DecisionResponse<Variation> decisionVariation =
                    getVariationFromExperimentRule(projectConfig, featureFlag.getKey(), experiment, user, options, userProfileTracker, decisionPath);
                reasons.merge(decisionVariation.getReasons());
                Variation variation = decisionVariation.getResult();
                String cmabUUID = decisionVariation.getCmabUUID();
                boolean error = decisionVariation.isError();
                if (error) {
                    return new DecisionResponse(
                        new FeatureDecision(experiment, variation, FeatureDecision.DecisionSource.FEATURE_TEST, cmabUUID),
                        reasons,
                        decisionVariation.isError(),
                        cmabUUID);
                }
                if (variation != null) {
                    return new DecisionResponse(
                        new FeatureDecision(experiment, variation, FeatureDecision.DecisionSource.FEATURE_TEST, cmabUUID),
                        reasons,
                        decisionVariation.isError(),
                        cmabUUID);
                }
            }
        } else {
            String message = reasons.addInfo("The feature flag \"%s\" is not used in any experiments.", featureFlag.getKey());
            logger.info(message);
        }

        return new DecisionResponse(null, reasons);

    }

    /**
     * Try to bucket the user into a rollout rule.
     * Evaluate the user for rules in priority order by seeing if the user satisfies the audience.
     * Fall back onto the everyone else rule if the user is ever excluded from a rule due to traffic allocation.
     *
     * @param featureFlag        The feature flag the user wants to access.
     * @param user               The current OptimizelyUserContext
     * @param projectConfig      The current projectConfig
     * @return A {@link DecisionResponse} including a {@link FeatureDecision} and the decision reasons
     */
    @Nonnull
    DecisionResponse<FeatureDecision> getVariationForFeatureInRollout(@Nonnull FeatureFlag featureFlag,
                                                                      @Nonnull OptimizelyUserContext user,
                                                                      @Nonnull ProjectConfig projectConfig) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();

        // use rollout to get variation for feature
        if (featureFlag.getRolloutId().isEmpty()) {
            String message = reasons.addInfo("The feature flag \"%s\" is not used in a rollout.", featureFlag.getKey());
            logger.info(message);
            return new DecisionResponse(new FeatureDecision(null, null, null), reasons);
        }
        Rollout rollout = projectConfig.getRolloutIdMapping().get(featureFlag.getRolloutId());
        if (rollout == null) {
            String message = reasons.addInfo("The rollout with id \"%s\" was not found in the datafile for feature flag \"%s\".",
                featureFlag.getRolloutId(), featureFlag.getKey());
            logger.error(message);
            return new DecisionResponse(new FeatureDecision(null, null, null), reasons);
        }

        // for all rules before the everyone else rule
        int rolloutRulesLength = rollout.getExperiments().size();
        if (rolloutRulesLength == 0) {
            return new DecisionResponse(new FeatureDecision(null, null, null), reasons);
        }


        int index = 0;
        while (index < rolloutRulesLength) {

            DecisionResponse<AbstractMap.SimpleEntry> decisionVariationResponse = getVariationFromDeliveryRule(
                projectConfig,
                featureFlag.getKey(),
                rollout.getExperiments(),
                index,
                user
            );
            reasons.merge(decisionVariationResponse.getReasons());

            AbstractMap.SimpleEntry<Variation, Boolean> response = decisionVariationResponse.getResult();
            Variation variation = response.getKey();
            Boolean skipToEveryoneElse = response.getValue();
            if (variation != null) {
                Experiment rule = rollout.getExperiments().get(index);
                FeatureDecision featureDecision = new FeatureDecision(rule, variation, FeatureDecision.DecisionSource.ROLLOUT);
                return new DecisionResponse(featureDecision, reasons);
            }

            // The last rule is special for "Everyone Else"
            index = skipToEveryoneElse ? (rolloutRulesLength - 1) : (index + 1);
        }

        return new DecisionResponse(new FeatureDecision(null, null, null), reasons);
    }

    /**
     * Get the variation the user has been whitelisted into.
     *
     * @param experiment {@link Experiment} in which user is to be bucketed.
     * @param userId     User Identifier
     * @return A {@link DecisionResponse} including the {@link Variation} that user is bucketed into (or null)
     * and the decision reasons. The variation can be null if the user is not whitelisted into any variation.
     */
    @Nonnull
    DecisionResponse<Variation> getWhitelistedVariation(@Nonnull Experiment experiment,
                                                        @Nonnull String userId) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();

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
            return new DecisionResponse(forcedVariation, reasons);
        }
        return new DecisionResponse(null, reasons);
    }

    /**
     * Determines the variation for a holdout rule.
     *
     * @param holdout The holdout rule to evaluate.
     * @param user The user context.
     * @param projectConfig The current project configuration.
     * @return A {@link DecisionResponse} with the variation (if any) and reasons.
     */
    @Nonnull
    DecisionResponse<Variation> getVariationForHoldout(@Nonnull Holdout holdout,
                                                       @Nonnull OptimizelyUserContext user,
                                                       @Nonnull ProjectConfig projectConfig) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();

        if (!holdout.isActive()) {
            String message = reasons.addInfo("Holdout (%s) is not running.", holdout.getKey());
            logger.info(message);
            return new DecisionResponse<>(null, reasons);
        }

        DecisionResponse<Boolean> decisionMeetAudience = ExperimentUtils.doesUserMeetAudienceConditions(projectConfig, holdout, user, EXPERIMENT, holdout.getKey());
        reasons.merge(decisionMeetAudience.getReasons());

        if (decisionMeetAudience.getResult()) {
            // User meets audience conditions for holdout
            String audienceMatchMessage = reasons.addInfo("User (%s) meets audience conditions for holdout (%s).", user.getUserId(), holdout.getKey());
            logger.info(audienceMatchMessage);

            String bucketingId = getBucketingId(user.getUserId(), user.getAttributes());
            DecisionResponse<Variation> decisionVariation = bucketer.bucket(holdout, bucketingId, projectConfig);
            reasons.merge(decisionVariation.getReasons());
            Variation variation = decisionVariation.getResult();

            if (variation != null) {
                String message = reasons.addInfo("User (%s) is in variation (%s) of holdout (%s).", user.getUserId(), variation.getKey(), holdout.getKey());
                logger.info(message);
            } else {
                String message = reasons.addInfo("User (%s) is in no holdout variation.", user.getUserId());
                logger.info(message);
            }
            return new DecisionResponse<>(variation, reasons);
        }

        String message = reasons.addInfo("User (%s) does not meet conditions for holdout (%s).", user.getUserId(), holdout.getKey());
        logger.info(message);
        return new DecisionResponse<>(null, reasons);
    }


    // TODO: Logically, it makes sense to move this method to UserProfileTracker. But some tests are also calling this
    // method, requiring us to refactor those tests as well. We'll look to refactor this later.
    /**
     * Get the {@link Variation} that has been stored for the user in the {@link UserProfileService} implementation.
     *
     * @param experiment  {@link Experiment} in which the user was bucketed.
     * @param userProfile {@link UserProfile} of the user.
     * @param projectConfig      The current projectConfig
     * @return A {@link DecisionResponse} including the {@link Variation} that user was previously bucketed into (or null)
     * and the decision reasons. The variation can be null if the {@link UserProfileService} implementation is null or the user was not previously bucketed.
     */
    @Nonnull
    DecisionResponse<Variation> getStoredVariation(@Nonnull Experiment experiment,
                                                   @Nonnull UserProfile userProfile,
                                                   @Nonnull ProjectConfig projectConfig) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();

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
                return new DecisionResponse(savedVariation, reasons);
            } else {
                String message = reasons.addInfo("User \"%s\" was previously bucketed into variation with ID \"%s\" for experiment \"%s\", but no matching variation was found for that user. We will re-bucket the user.",
                    userProfile.userId, variationId, experimentKey);
                logger.info(message);
                return new DecisionResponse(null, reasons);
            }
        } else {
            String message = reasons.addInfo("No previously activated variation of experiment \"%s\" for user \"%s\" found in user profile.",
                experimentKey, userProfile.userId);
            logger.info(message);
            return new DecisionResponse(null, reasons);
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

    /**
     * Find a validated forced decision
     *
     * @param optimizelyDecisionContext The OptimizelyDecisionContext containing flagKey and ruleKey
     * @param projectConfig             The Project config
     * @param user                      The OptimizelyUserContext
     * @return Returns a DecisionResponse structure of type Variation, otherwise null result with reasons
     */
    public DecisionResponse<Variation> validatedForcedDecision(@Nonnull OptimizelyDecisionContext optimizelyDecisionContext, @Nonnull ProjectConfig projectConfig, @Nonnull OptimizelyUserContext user) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();
        String userId = user.getUserId();
        OptimizelyForcedDecision optimizelyForcedDecision = user.findForcedDecision(optimizelyDecisionContext);
        String variationKey = optimizelyForcedDecision != null ? optimizelyForcedDecision.getVariationKey() : null;
        if (projectConfig != null && variationKey != null) {
            Variation variation = projectConfig.getFlagVariationByKey(optimizelyDecisionContext.getFlagKey(), variationKey);
            String ruleKey = optimizelyDecisionContext.getRuleKey();
            String flagKey = optimizelyDecisionContext.getFlagKey();
            String info;
            String target = ruleKey != OptimizelyDecisionContext.OPTI_NULL_RULE_KEY ? String.format("flag (%s), rule (%s)", flagKey, ruleKey) : String.format("flag (%s)", flagKey);
            if (variation != null) {
                info = String.format("Variation (%s) is mapped to %s and user (%s) in the forced decision map.", variationKey, target, userId);
                logger.debug(info);
                reasons.addInfo(info);
                return new DecisionResponse(variation, reasons);
            } else {
                info = String.format("Invalid variation is mapped to %s and user (%s) in the forced decision map.", target, userId);
                logger.debug(info);
                reasons.addInfo(info);
            }
        }
        return new DecisionResponse<>(null, reasons);
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
            logger.error("User ID is invalid");
            return false;
        }

        ConcurrentHashMap<String, String> experimentToVariation;
        if (!forcedVariationMapping.containsKey(userId)) {
            forcedVariationMapping.putIfAbsent(userId, new ConcurrentHashMap());
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
     * @return A {@link DecisionResponse} including the {@link Variation} that user is bucketed into (or null)
     * and the decision reasons. The variation can be null if the forced variation fails.
     */
    @Nonnull
    public DecisionResponse<Variation> getForcedVariation(@Nonnull Experiment experiment,
                                                          @Nonnull String userId) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();

        if (!validateUserId(userId)) {
            String message = reasons.addInfo("User ID is invalid");
            logger.error(message);
            return new DecisionResponse(null, reasons);
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
                    return new DecisionResponse(variation, reasons);
                }
            } else {
                logger.debug("No variation for experiment \"{}\" mapped to user \"{}\" in the forced variation map ", experiment.getKey(), userId);
            }
        }
        return new DecisionResponse(null, reasons);
    }


    private DecisionResponse<Variation> getVariationFromExperimentRule(@Nonnull ProjectConfig projectConfig,
                                                                      @Nonnull String flagKey,
                                                                      @Nonnull Experiment rule,
                                                                      @Nonnull OptimizelyUserContext user,
                                                                      @Nonnull List<OptimizelyDecideOption> options,
                                                                      @Nullable UserProfileTracker userProfileTracker,
                                                                      @Nonnull DecisionPath decisionPath) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();

        String ruleKey = rule != null ? rule.getKey() : null;
        // Check Forced-Decision
        OptimizelyDecisionContext optimizelyDecisionContext = new OptimizelyDecisionContext(flagKey, ruleKey);
        DecisionResponse<Variation> forcedDecisionResponse = validatedForcedDecision(optimizelyDecisionContext, projectConfig, user);

        reasons.merge(forcedDecisionResponse.getReasons());

        Variation variation = forcedDecisionResponse.getResult();
        if (variation != null) {
            return new DecisionResponse(variation, reasons);
        }
        //regular decision
        DecisionResponse<Variation> decisionResponse = getVariation(rule, user, projectConfig, options, userProfileTracker, null, decisionPath);
        reasons.merge(decisionResponse.getReasons());

        variation = decisionResponse.getResult();

        return new DecisionResponse<>(variation, reasons, decisionResponse.isError(), decisionResponse.getCmabUUID());
    }

    /**
     * Helper function to check that the provided userId is valid
     *
     * @param userId the userId being validated
     * @return whether the user ID is valid
     */
    private boolean validateUserId(String userId) {
        return (userId != null);
    }

    /**
     *
     * @param projectConfig     The Project config
     * @param flagKey           The flag key for the feature flag
     * @param rules             The experiments belonging to a rollout
     * @param ruleIndex         The index of the rule
     * @param user              The OptimizelyUserContext
     * @return                  Returns a DecisionResponse Object containing a AbstractMap.SimpleEntry<Variation, Boolean>
     *                          where the Variation is the result and the Boolean is the skipToEveryoneElse.
     */
    DecisionResponse<AbstractMap.SimpleEntry> getVariationFromDeliveryRule(@Nonnull ProjectConfig projectConfig,
                                                       @Nonnull String flagKey,
                                                       @Nonnull List<Experiment> rules,
                                                       @Nonnull int ruleIndex,
                                                       @Nonnull OptimizelyUserContext user) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();

        Boolean skipToEveryoneElse = false;
        AbstractMap.SimpleEntry<Variation, Boolean> variationToSkipToEveryoneElsePair;
        // Check forced-decisions first
        Experiment rule = rules.get(ruleIndex);
        OptimizelyDecisionContext optimizelyDecisionContext = new OptimizelyDecisionContext(flagKey, rule.getKey());
        DecisionResponse<Variation> forcedDecisionResponse = validatedForcedDecision(optimizelyDecisionContext, projectConfig, user);
        reasons.merge(forcedDecisionResponse.getReasons());

        Variation variation = forcedDecisionResponse.getResult();
        if (variation != null) {
            variationToSkipToEveryoneElsePair = new AbstractMap.SimpleEntry<>(variation, false);
            return new DecisionResponse(variationToSkipToEveryoneElsePair, reasons);
        }

        // Handle a regular decision
        String bucketingId = getBucketingId(user.getUserId(), user.getAttributes());
        Boolean everyoneElse = (ruleIndex == rules.size() - 1);
        String loggingKey = everyoneElse ? "Everyone Else" : String.valueOf(ruleIndex + 1);

        Variation bucketedVariation = null;

        DecisionResponse<Boolean> audienceDecisionResponse = ExperimentUtils.doesUserMeetAudienceConditions(
            projectConfig,
            rule,
            user,
            RULE,
            String.valueOf(ruleIndex + 1)
        );

        reasons.merge(audienceDecisionResponse.getReasons());
        String message;
        if (audienceDecisionResponse.getResult()) {
            message = reasons.addInfo("User \"%s\" meets conditions for targeting rule \"%s\".", user.getUserId(), loggingKey);
            reasons.addInfo(message);
            logger.debug(message);

            DecisionResponse<Variation> decisionResponse = bucketer.bucket(rule, bucketingId, projectConfig);
            reasons.merge(decisionResponse.getReasons());
            bucketedVariation = decisionResponse.getResult();

            if (bucketedVariation != null) {
                message = reasons.addInfo("User \"%s\" bucketed for targeting rule \"%s\".", user.getUserId(), loggingKey);
                logger.debug(message);
                reasons.addInfo(message);
            } else if (!everyoneElse) {
                message = reasons.addInfo("User \"%s\" is not bucketed for targeting rule \"%s\".", user.getUserId(), loggingKey);
                logger.debug(message);
                reasons.addInfo(message);
                // Skip the rest of rollout rules to the everyone-else rule if audience matches but not bucketed.
                skipToEveryoneElse = true;
            }
        } else {
            message = reasons.addInfo("User \"%s\" does not meet conditions for targeting rule \"%d\".", user.getUserId(), ruleIndex + 1);
            reasons.addInfo(message);
            logger.debug(message);
        }
        variationToSkipToEveryoneElsePair = new AbstractMap.SimpleEntry<>(bucketedVariation, skipToEveryoneElse);
        return new DecisionResponse(variationToSkipToEveryoneElsePair, reasons);
    }

    /**
     * Retrieves a decision for a contextual multi-armed bandit (CMAB)
     * experiment.
     *
     * @param projectConfig Instance of ProjectConfig.
     * @param experiment The experiment object for which the decision is to be
     * made.
     * @param userContext The user context containing user id and attributes.
     * @param bucketingId The bucketing ID to use for traffic allocation.
     * @param options Optional list of decide options.
     * @return A CmabDecisionResult containing error status, result, and
     * reasons.
     */
    private DecisionResponse<CmabDecision> getDecisionForCmabExperiment(@Nonnull ProjectConfig projectConfig,
                                                                        @Nonnull Experiment experiment,
                                                                        @Nonnull OptimizelyUserContext userContext,
                                                                        @Nonnull String bucketingId,
                                                                        @Nonnull List<OptimizelyDecideOption> options) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();

        // Check if user is in CMAB traffic allocation
        DecisionResponse<Variation> bucketResponse = bucketer.bucket(experiment, bucketingId, projectConfig, true);
        // DecisionResponse<String> bucketResponse = bucketer.bucketForCmab(experiment, bucketingId, projectConfig);
        reasons.merge(bucketResponse.getReasons());

        Variation bucketedVariation = bucketResponse.getResult();
        String bucketedEntityId = bucketedVariation != null ? bucketedVariation.getId() : null;

        if (bucketedEntityId == null) {
            String message = String.format("User \"%s\" not in CMAB experiment \"%s\" due to traffic allocation.",
                    userContext.getUserId(), experiment.getKey());
            logger.info(message);
            reasons.addInfo(message);

            return new DecisionResponse<>(null, reasons);
        }

        // User is in CMAB allocation, proceed to CMAB decision
        try {
            CmabDecision cmabDecision = cmabService.getDecision(projectConfig, userContext, experiment.getId(), options);

            return new DecisionResponse<>(cmabDecision, reasons);
        } catch (Exception e) {
            String errorMessage = String.format("CMAB fetch failed for experiment \"%s\"", experiment.getKey());
            reasons.addInfo(errorMessage);
            logger.error("{} {}", errorMessage, e.getMessage());

            return new DecisionResponse<>(null, reasons, true, null);
        }
    }

    /**
     * Checks whether an experiment is a contextual multi-armed bandit (CMAB)
     * experiment.
     *
     * @param experiment The experiment to check
     * @return true if the experiment is a CMAB experiment, false otherwise
     */
    private boolean isCmabExperiment(@Nonnull Experiment experiment) {
        if (cmabService == null){
            return false;
        }
        return experiment.getCmab() != null;
    }
}

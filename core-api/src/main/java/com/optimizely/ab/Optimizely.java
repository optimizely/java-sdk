/****************************************************************************
 * Copyright 2016-2023, Optimizely, Inc. and contributors                   *
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
package com.optimizely.ab;

import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.bucketing.DecisionService;
import com.optimizely.ab.bucketing.FeatureDecision;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.*;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.event.*;
import com.optimizely.ab.event.internal.*;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.internal.NotificationRegistry;
import com.optimizely.ab.notification.*;
import com.optimizely.ab.odp.*;
import com.optimizely.ab.optimizelyconfig.OptimizelyConfig;
import com.optimizely.ab.optimizelyconfig.OptimizelyConfigManager;
import com.optimizely.ab.optimizelyconfig.OptimizelyConfigService;
import com.optimizely.ab.optimizelydecision.*;
import com.optimizely.ab.optimizelyjson.OptimizelyJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.util.*;

import static com.optimizely.ab.internal.SafetyUtils.tryClose;

/**
 * Top-level container class for Optimizely functionality.
 * Thread-safe, so can be created as a singleton and safely passed around.
 *
 * Example instantiation:
 * <pre>
 *     Optimizely optimizely = Optimizely.builder(projectWatcher, eventHandler).build();
 * </pre>
 *
 * To activate an experiment and perform variation specific processing:
 * <pre>
 *     Variation variation = optimizely.activate(experimentKey, userId, attributes);
 *     if (variation.is("ALGORITHM_A")) {
 *         // execute code for algorithm A
 *     } else if (variation.is("ALGORITHM_B")) {
 *         // execute code for algorithm B
 *     } else {
 *         // execute code for default algorithm
 *     }
 * </pre>
 *
 * <b>NOTE:</b> by default, all exceptions originating from {@code Optimizely} calls are suppressed.
 * For example, attempting to activate an experiment that does not exist in the project config will cause an error
 * to be logged, and for the "control" variation to be returned.
 */
@ThreadSafe
public class Optimizely implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Optimizely.class);

    final DecisionService decisionService;
    @VisibleForTesting
    @Deprecated
    final EventHandler eventHandler;
    @VisibleForTesting
    final EventProcessor eventProcessor;
    @VisibleForTesting
    final ErrorHandler errorHandler;

    public final List<OptimizelyDecideOption> defaultDecideOptions;

    private final ProjectConfigManager projectConfigManager;

    @Nullable
    private final OptimizelyConfigManager optimizelyConfigManager;

    // TODO should be private
    public final NotificationCenter notificationCenter;

    @Nullable
    private final UserProfileService userProfileService;

    @Nullable
    private final ODPManager odpManager;

    private Optimizely(@Nonnull EventHandler eventHandler,
                       @Nonnull EventProcessor eventProcessor,
                       @Nonnull ErrorHandler errorHandler,
                       @Nonnull DecisionService decisionService,
                       @Nullable UserProfileService userProfileService,
                       @Nonnull ProjectConfigManager projectConfigManager,
                       @Nullable OptimizelyConfigManager optimizelyConfigManager,
                       @Nonnull NotificationCenter notificationCenter,
                       @Nonnull List<OptimizelyDecideOption> defaultDecideOptions,
                       @Nullable ODPManager odpManager
    ) {
        this.eventHandler = eventHandler;
        this.eventProcessor = eventProcessor;
        this.errorHandler = errorHandler;
        this.decisionService = decisionService;
        this.userProfileService = userProfileService;
        this.projectConfigManager = projectConfigManager;
        this.optimizelyConfigManager = optimizelyConfigManager;
        this.notificationCenter = notificationCenter;
        this.defaultDecideOptions = defaultDecideOptions;
        this.odpManager = odpManager;

        if (odpManager != null) {
            odpManager.getEventManager().start();
            if (projectConfigManager.getCachedConfig() != null) {
                updateODPSettings();
            }
            if (projectConfigManager.getSDKKey() != null) {
                NotificationRegistry.getInternalNotificationCenter(projectConfigManager.getSDKKey()).
                    addNotificationHandler(UpdateConfigNotification.class,
                        configNotification -> { updateODPSettings(); });
            }

        }
    }

    /**
     * Determine if the instance of the Optimizely client is valid. An instance can be deemed invalid if it was not
     * initialized properly due to an invalid datafile being passed in.
     *
     * @return True if the Optimizely instance is valid.
     * False if the Optimizely instance is not valid.
     */
    public boolean isValid() {
        return getProjectConfig() != null;
    }

    /**
     * Checks if eventHandler {@link EventHandler} and projectConfigManager {@link ProjectConfigManager}
     * are Closeable {@link Closeable} and calls close on them.
     *
     * <b>NOTE:</b> There is a chance that this could be long running if the implementations of close are long running.
     */
    @Override
    public void close() {
        tryClose(eventProcessor);
        tryClose(eventHandler);
        tryClose(projectConfigManager);
        notificationCenter.clearAllNotificationListeners();
        NotificationRegistry.clearNotificationCenterRegistry(projectConfigManager.getSDKKey());
        if (odpManager != null) {
            tryClose(odpManager);
        }
    }

    //======== activate calls ========//

    @Nullable
    public Variation activate(@Nonnull String experimentKey,
                              @Nonnull String userId) throws UnknownExperimentException {
        return activate(experimentKey, userId, Collections.<String, String>emptyMap());
    }

    @Nullable
    public Variation activate(@Nonnull String experimentKey,
                              @Nonnull String userId,
                              @Nonnull Map<String, ?> attributes) throws UnknownExperimentException {

        if (experimentKey == null) {
            logger.error("The experimentKey parameter must be nonnull.");
            return null;
        }

        if (!validateUserId(userId)) {
            logger.info("Not activating user for experiment \"{}\".", experimentKey);
            return null;
        }

        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing activate call.");
            return null;
        }

        Experiment experiment = projectConfig.getExperimentForKey(experimentKey, errorHandler);
        if (experiment == null) {
            // if we're unable to retrieve the associated experiment, return null
            logger.info("Not activating user \"{}\" for experiment \"{}\".", userId, experimentKey);
            return null;
        }

        return activate(projectConfig, experiment, userId, attributes);
    }

    @Nullable
    public Variation activate(@Nonnull Experiment experiment,
                              @Nonnull String userId) {
        return activate(experiment, userId, Collections.<String, String>emptyMap());
    }

    @Nullable
    public Variation activate(@Nonnull Experiment experiment,
                              @Nonnull String userId,
                              @Nonnull Map<String, ?> attributes) {
        return activate(getProjectConfig(), experiment, userId, attributes);
    }

    @Nullable
    private Variation activate(@Nullable ProjectConfig projectConfig,
                               @Nonnull Experiment experiment,
                               @Nonnull String userId,
                               @Nonnull Map<String, ?> attributes) {
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing activate call.");
            return null;
        }

        if (!validateUserId(userId)) {
            logger.info("Not activating user \"{}\" for experiment \"{}\".", userId, experiment.getKey());
            return null;
        }
        Map<String, ?> copiedAttributes = copyAttributes(attributes);
        // bucket the user to the given experiment and dispatch an impression event
        Variation variation = getVariation(projectConfig, experiment, userId, copiedAttributes);
        if (variation == null) {
            logger.info("Not activating user \"{}\" for experiment \"{}\".", userId, experiment.getKey());
            return null;
        }

        sendImpression(projectConfig, experiment, userId, copiedAttributes, variation, "experiment");

        return variation;
    }

    /**
     * Creates and sends impression event.
     *
     * @param projectConfig      the current projectConfig
     * @param experiment         the experiment user bucketed into and dispatch an impression event
     * @param userId             the ID of the user
     * @param filteredAttributes the attributes of the user
     * @param variation          the variation that was returned from activate.
     * @param ruleType           It can either be experiment in case impression event is sent from activate or it's feature-test or rollout
     */
    private void sendImpression(@Nonnull ProjectConfig projectConfig,
                                @Nonnull Experiment experiment,
                                @Nonnull String userId,
                                @Nonnull Map<String, ?> filteredAttributes,
                                @Nonnull Variation variation,
                                @Nonnull String ruleType) {
        sendImpression(projectConfig, experiment, userId, filteredAttributes, variation, "", ruleType, true);
    }

    /**
     * Creates and sends impression event.
     *
     * @param projectConfig      the current projectConfig
     * @param experiment         the experiment user bucketed into and dispatch an impression event
     * @param userId             the ID of the user
     * @param filteredAttributes the attributes of the user
     * @param variation          the variation that was returned from activate.
     * @param flagKey            It can either be empty if ruleType is experiment or it's feature key in case ruleType is feature-test or rollout
     * @param ruleType           It can either be experiment in case impression event is sent from activate or it's feature-test or rollout
     */
    private boolean sendImpression(@Nonnull ProjectConfig projectConfig,
                                   @Nullable Experiment experiment,
                                   @Nonnull String userId,
                                   @Nonnull Map<String, ?> filteredAttributes,
                                   @Nullable Variation variation,
                                   @Nonnull String flagKey,
                                   @Nonnull String ruleType,
                                   @Nonnull boolean enabled) {

        UserEvent userEvent = UserEventFactory.createImpressionEvent(
            projectConfig,
            experiment,
            variation,
            userId,
            filteredAttributes,
            flagKey,
            ruleType,
            enabled);

        if (userEvent == null) {
            return false;
        }
        eventProcessor.process(userEvent);
        if (experiment != null) {
            logger.info("Activating user \"{}\" in experiment \"{}\".", userId, experiment.getKey());
        }
        // Kept For backwards compatibility.
        // This notification is deprecated and the new DecisionNotifications
        // are sent via their respective method calls.
        if (notificationCenter.getNotificationManager(ActivateNotification.class).size() > 0) {
            LogEvent impressionEvent = EventFactory.createLogEvent(userEvent);
            ActivateNotification activateNotification = new ActivateNotification(
                experiment, userId, filteredAttributes, variation, impressionEvent);
            notificationCenter.send(activateNotification);
        }
        return true;
    }

    //======== track calls ========//

    public void track(@Nonnull String eventName,
                      @Nonnull String userId) throws UnknownEventTypeException {
        track(eventName, userId, Collections.<String, String>emptyMap(), Collections.<String, Object>emptyMap());
    }

    public void track(@Nonnull String eventName,
                      @Nonnull String userId,
                      @Nonnull Map<String, ?> attributes) throws UnknownEventTypeException {
        track(eventName, userId, attributes, Collections.<String, String>emptyMap());
    }

    public void track(@Nonnull String eventName,
                      @Nonnull String userId,
                      @Nonnull Map<String, ?> attributes,
                      @Nonnull Map<String, ?> eventTags) throws UnknownEventTypeException {
        if (!validateUserId(userId)) {
            logger.info("Not tracking event \"{}\".", eventName);
            return;
        }

        if (eventName == null || eventName.trim().isEmpty()) {
            logger.error("Event Key is null or empty when non-null and non-empty String was expected.");
            logger.info("Not tracking event for user \"{}\".", userId);
            return;
        }

        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing isFeatureEnabled call.");
            return;
        }

        Map<String, ?> copiedAttributes = copyAttributes(attributes);

        EventType eventType = projectConfig.getEventTypeForName(eventName, errorHandler);
        if (eventType == null) {
            // if no matching event type could be found, do not dispatch an event
            logger.info("Not tracking event \"{}\" for user \"{}\".", eventName, userId);
            return;
        }

        if (eventTags == null) {
            logger.warn("Event tags is null when non-null was expected. Defaulting to an empty event tags map.");
        }

        UserEvent userEvent = UserEventFactory.createConversionEvent(
            projectConfig,
            userId,
            eventType.getId(),
            eventType.getKey(),
            copiedAttributes,
            eventTags);

        eventProcessor.process(userEvent);
        logger.info("Tracking event \"{}\" for user \"{}\".", eventName, userId);

        if (notificationCenter.getNotificationManager(TrackNotification.class).size() > 0) {
            // create the conversion event request parameters, then dispatch
            LogEvent conversionEvent = EventFactory.createLogEvent(userEvent);
            TrackNotification notification = new TrackNotification(eventName, userId,
                copiedAttributes, eventTags, conversionEvent);

            notificationCenter.send(notification);
        }
    }

    //======== FeatureFlag APIs ========//

    /**
     * Determine whether a boolean feature is enabled.
     * Send an impression event if the user is bucketed into an experiment using the feature.
     *
     * @param featureKey The unique key of the feature.
     * @param userId     The ID of the user.
     * @return True if the feature is enabled.
     * False if the feature is disabled.
     * False if the feature is not found.
     */
    @Nonnull
    public Boolean isFeatureEnabled(@Nonnull String featureKey,
                                    @Nonnull String userId) {
        return isFeatureEnabled(featureKey, userId, Collections.emptyMap());
    }

    /**
     * Determine whether a boolean feature is enabled.
     * Send an impression event if the user is bucketed into an experiment using the feature.
     *
     * @param featureKey The unique key of the feature.
     * @param userId     The ID of the user.
     * @param attributes The user's attributes.
     * @return True if the feature is enabled.
     * False if the feature is disabled.
     * False if the feature is not found.
     */
    @Nonnull
    public Boolean isFeatureEnabled(@Nonnull String featureKey,
                                    @Nonnull String userId,
                                    @Nonnull Map<String, ?> attributes) {
        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing isFeatureEnabled call.");
            return false;
        }

        return isFeatureEnabled(projectConfig, featureKey, userId, attributes);
    }

    @Nonnull
    private Boolean isFeatureEnabled(@Nonnull ProjectConfig projectConfig,
                                     @Nonnull String featureKey,
                                     @Nonnull String userId,
                                     @Nonnull Map<String, ?> attributes) {
        if (featureKey == null) {
            logger.warn("The featureKey parameter must be nonnull.");
            return false;
        } else if (userId == null) {
            logger.warn("The userId parameter must be nonnull.");
            return false;
        }

        FeatureFlag featureFlag = projectConfig.getFeatureKeyMapping().get(featureKey);
        if (featureFlag == null) {
            logger.info("No feature flag was found for key \"{}\".", featureKey);
            return false;
        }

        Map<String, ?> copiedAttributes = copyAttributes(attributes);
        FeatureDecision.DecisionSource decisionSource = FeatureDecision.DecisionSource.ROLLOUT;
        FeatureDecision featureDecision = decisionService.getVariationForFeature(featureFlag, createUserContextCopy(userId, copiedAttributes), projectConfig).getResult();
        Boolean featureEnabled = false;
        SourceInfo sourceInfo = new RolloutSourceInfo();
        if (featureDecision.decisionSource != null) {
            decisionSource = featureDecision.decisionSource;
        }

        if (featureDecision.variation != null) {
            // This information is only necessary for feature tests.
            // For rollouts experiments and variations are an implementation detail only.
            if (featureDecision.decisionSource.equals(FeatureDecision.DecisionSource.FEATURE_TEST)) {
                sourceInfo = new FeatureTestSourceInfo(featureDecision.experiment.getKey(), featureDecision.variation.getKey());
            } else {
                logger.info("The user \"{}\" is not included in an experiment for feature \"{}\".",
                    userId, featureKey);
            }
            if (featureDecision.variation.getFeatureEnabled()) {
                featureEnabled = true;
            }
        }
        sendImpression(
            projectConfig,
            featureDecision.experiment,
            userId,
            copiedAttributes,
            featureDecision.variation,
            featureKey,
            decisionSource.toString(),
            featureEnabled);

        DecisionNotification decisionNotification = DecisionNotification.newFeatureDecisionNotificationBuilder()
            .withUserId(userId)
            .withAttributes(copiedAttributes)
            .withFeatureKey(featureKey)
            .withFeatureEnabled(featureEnabled)
            .withSource(decisionSource)
            .withSourceInfo(sourceInfo)
            .build();

        notificationCenter.send(decisionNotification);

        logger.info("Feature \"{}\" is enabled for user \"{}\"? {}", featureKey, userId, featureEnabled);
        return featureEnabled;
    }

    /**
     * Get the Boolean value of the specified variable in the feature.
     *
     * @param featureKey  The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId      The ID of the user.
     * @return The Boolean value of the boolean single variable feature.
     * Null if the feature could not be found.
     */
    @Nullable
    public Boolean getFeatureVariableBoolean(@Nonnull String featureKey,
                                             @Nonnull String variableKey,
                                             @Nonnull String userId) {
        return getFeatureVariableBoolean(featureKey, variableKey, userId, Collections.<String, String>emptyMap());
    }

    /**
     * Get the Boolean value of the specified variable in the feature.
     *
     * @param featureKey  The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId      The ID of the user.
     * @param attributes  The user's attributes.
     * @return The Boolean value of the boolean single variable feature.
     * Null if the feature or variable could not be found.
     */
    @Nullable
    public Boolean getFeatureVariableBoolean(@Nonnull String featureKey,
                                             @Nonnull String variableKey,
                                             @Nonnull String userId,
                                             @Nonnull Map<String, ?> attributes) {

        return getFeatureVariableValueForType(
            featureKey,
            variableKey,
            userId,
            attributes,
            FeatureVariable.BOOLEAN_TYPE
        );
    }

    /**
     * Get the Double value of the specified variable in the feature.
     *
     * @param featureKey  The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId      The ID of the user.
     * @return The Double value of the double single variable feature.
     * Null if the feature or variable could not be found.
     */
    @Nullable
    public Double getFeatureVariableDouble(@Nonnull String featureKey,
                                           @Nonnull String variableKey,
                                           @Nonnull String userId) {
        return getFeatureVariableDouble(featureKey, variableKey, userId, Collections.<String, String>emptyMap());
    }

    /**
     * Get the Double value of the specified variable in the feature.
     *
     * @param featureKey  The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId      The ID of the user.
     * @param attributes  The user's attributes.
     * @return The Double value of the double single variable feature.
     * Null if the feature or variable could not be found.
     */
    @Nullable
    public Double getFeatureVariableDouble(@Nonnull String featureKey,
                                           @Nonnull String variableKey,
                                           @Nonnull String userId,
                                           @Nonnull Map<String, ?> attributes) {

        Double variableValue = null;
        try {
            variableValue = getFeatureVariableValueForType(
                featureKey,
                variableKey,
                userId,
                attributes,
                FeatureVariable.DOUBLE_TYPE
            );
        } catch (Exception exception) {
            logger.error("NumberFormatException while trying to parse \"" + variableValue +
                "\" as Double. " + exception);
        }

        return variableValue;
    }

    /**
     * Get the Integer value of the specified variable in the feature.
     *
     * @param featureKey  The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId      The ID of the user.
     * @return The Integer value of the integer single variable feature.
     * Null if the feature or variable could not be found.
     */
    @Nullable
    public Integer getFeatureVariableInteger(@Nonnull String featureKey,
                                             @Nonnull String variableKey,
                                             @Nonnull String userId) {
        return getFeatureVariableInteger(featureKey, variableKey, userId, Collections.<String, String>emptyMap());
    }

    /**
     * Get the Integer value of the specified variable in the feature.
     *
     * @param featureKey  The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId      The ID of the user.
     * @param attributes  The user's attributes.
     * @return The Integer value of the integer single variable feature.
     * Null if the feature or variable could not be found.
     */
    @Nullable
    public Integer getFeatureVariableInteger(@Nonnull String featureKey,
                                             @Nonnull String variableKey,
                                             @Nonnull String userId,
                                             @Nonnull Map<String, ?> attributes) {

        Integer variableValue = null;

        try {
            variableValue = getFeatureVariableValueForType(
                featureKey,
                variableKey,
                userId,
                attributes,
                FeatureVariable.INTEGER_TYPE
            );

        } catch (Exception exception) {
            logger.error("NumberFormatException while trying to parse value as Integer. " + exception.toString());
        }

        return variableValue;
    }

    /**
     * Get the String value of the specified variable in the feature.
     *
     * @param featureKey  The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId      The ID of the user.
     * @return The String value of the string single variable feature.
     * Null if the feature or variable could not be found.
     */
    @Nullable
    public String getFeatureVariableString(@Nonnull String featureKey,
                                           @Nonnull String variableKey,
                                           @Nonnull String userId) {
        return getFeatureVariableString(featureKey, variableKey, userId, Collections.<String, String>emptyMap());
    }

    /**
     * Get the String value of the specified variable in the feature.
     *
     * @param featureKey  The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId      The ID of the user.
     * @param attributes  The user's attributes.
     * @return The String value of the string single variable feature.
     * Null if the feature or variable could not be found.
     */
    @Nullable
    public String getFeatureVariableString(@Nonnull String featureKey,
                                           @Nonnull String variableKey,
                                           @Nonnull String userId,
                                           @Nonnull Map<String, ?> attributes) {

        return getFeatureVariableValueForType(
            featureKey,
            variableKey,
            userId,
            attributes,
            FeatureVariable.STRING_TYPE);
    }

    /**
     * Get the JSON value of the specified variable in the feature.
     *
     * @param featureKey  The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId      The ID of the user.
     * @return An OptimizelyJSON instance for the JSON variable value.
     * Null if the feature or variable could not be found.
     */
    @Nullable
    public OptimizelyJSON getFeatureVariableJSON(@Nonnull String featureKey,
                                                 @Nonnull String variableKey,
                                                 @Nonnull String userId) {
        return getFeatureVariableJSON(featureKey, variableKey, userId, Collections.<String, String>emptyMap());
    }

    /**
     * Get the JSON value of the specified variable in the feature.
     *
     * @param featureKey  The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId      The ID of the user.
     * @param attributes  The user's attributes.
     * @return An OptimizelyJSON instance for the JSON variable value.
     * Null if the feature or variable could not be found.
     */
    @Nullable
    public OptimizelyJSON getFeatureVariableJSON(@Nonnull String featureKey,
                                                 @Nonnull String variableKey,
                                                 @Nonnull String userId,
                                                 @Nonnull Map<String, ?> attributes) {

        return getFeatureVariableValueForType(
            featureKey,
            variableKey,
            userId,
            attributes,
            FeatureVariable.JSON_TYPE);
    }

    @VisibleForTesting
    <T> T getFeatureVariableValueForType(@Nonnull String featureKey,
                                         @Nonnull String variableKey,
                                         @Nonnull String userId,
                                         @Nonnull Map<String, ?> attributes,
                                         @Nonnull String variableType) {
        if (featureKey == null) {
            logger.warn("The featureKey parameter must be nonnull.");
            return null;
        } else if (variableKey == null) {
            logger.warn("The variableKey parameter must be nonnull.");
            return null;
        } else if (userId == null) {
            logger.warn("The userId parameter must be nonnull.");
            return null;
        }

        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing getFeatureVariableValueForType call. type: {}", variableType);
            return null;
        }

        FeatureFlag featureFlag = projectConfig.getFeatureKeyMapping().get(featureKey);
        if (featureFlag == null) {
            logger.info("No feature flag was found for key \"{}\".", featureKey);
            return null;
        }

        FeatureVariable variable = featureFlag.getVariableKeyToFeatureVariableMap().get(variableKey);
        if (variable == null) {
            logger.info("No feature variable was found for key \"{}\" in feature flag \"{}\".",
                variableKey, featureKey);
            return null;
        } else if (!variable.getType().equals(variableType)) {
            logger.info("The feature variable \"" + variableKey +
                "\" is actually of type \"" + variable.getType().toString() +
                "\" type. You tried to access it as type \"" + variableType.toString() +
                "\". Please use the appropriate feature variable accessor.");
            return null;
        }

        String variableValue = variable.getDefaultValue();
        Map<String, ?> copiedAttributes = copyAttributes(attributes);
        FeatureDecision featureDecision = decisionService.getVariationForFeature(featureFlag, createUserContextCopy(userId, copiedAttributes), projectConfig).getResult();
        Boolean featureEnabled = false;
        if (featureDecision.variation != null) {
            if (featureDecision.variation.getFeatureEnabled()) {
                FeatureVariableUsageInstance featureVariableUsageInstance =
                    featureDecision.variation.getVariableIdToFeatureVariableUsageInstanceMap().get(variable.getId());
                if (featureVariableUsageInstance != null) {
                    variableValue = featureVariableUsageInstance.getValue();
                    logger.info("Got variable value \"{}\" for variable \"{}\" of feature flag \"{}\".", variableValue, variableKey, featureKey);
                } else {
                    variableValue = variable.getDefaultValue();
                    logger.info("Value is not defined for variable \"{}\". Returning default value \"{}\".", variableKey, variableValue);
                }
            } else {
                logger.info("Feature \"{}\" is not enabled for user \"{}\". " +
                        "Returning the default variable value \"{}\".",
                    featureKey, userId, variableValue
                );
            }
            featureEnabled = featureDecision.variation.getFeatureEnabled();
        } else {
            logger.info("User \"{}\" was not bucketed into any variation for feature flag \"{}\". " +
                    "The default value \"{}\" for \"{}\" is being returned.",
                userId, featureKey, variableValue, variableKey
            );
        }

        Object convertedValue = convertStringToType(variableValue, variableType);
        Object notificationValue = convertedValue;
        if (convertedValue instanceof OptimizelyJSON) {
            notificationValue = ((OptimizelyJSON) convertedValue).toMap();
        }

        DecisionNotification decisionNotification = DecisionNotification.newFeatureVariableDecisionNotificationBuilder()
            .withUserId(userId)
            .withAttributes(copiedAttributes)
            .withFeatureKey(featureKey)
            .withFeatureEnabled(featureEnabled)
            .withVariableKey(variableKey)
            .withVariableType(variableType)
            .withVariableValue(notificationValue)
            .withFeatureDecision(featureDecision)
            .build();


        notificationCenter.send(decisionNotification);

        return (T) convertedValue;
    }

    // Helper method which takes type and variable value and convert it to object to use in Listener DecisionInfo object variable value
    Object convertStringToType(String variableValue, String type) {
        if (variableValue != null) {
            switch (type) {
                case FeatureVariable.DOUBLE_TYPE:
                    try {
                        return Double.parseDouble(variableValue);
                    } catch (NumberFormatException exception) {
                        logger.error("NumberFormatException while trying to parse \"" + variableValue +
                            "\" as Double. " + exception);
                    }
                    break;
                case FeatureVariable.STRING_TYPE:
                    return variableValue;
                case FeatureVariable.BOOLEAN_TYPE:
                    return Boolean.parseBoolean(variableValue);
                case FeatureVariable.INTEGER_TYPE:
                    try {
                        return Integer.parseInt(variableValue);
                    } catch (NumberFormatException exception) {
                        logger.error("NumberFormatException while trying to parse \"" + variableValue +
                            "\" as Integer. " + exception.toString());
                    }
                    break;
                case FeatureVariable.JSON_TYPE:
                    return new OptimizelyJSON(variableValue);
                default:
                    return variableValue;
            }
        }

        return null;
    }

    /**
     * Get the values of all variables in the feature.
     *
     * @param featureKey  The unique key of the feature.
     * @param userId      The ID of the user.
     * @return An OptimizelyJSON instance for all variable values.
     * Null if the feature could not be found.
     *
     */
    @Nullable
    public OptimizelyJSON getAllFeatureVariables(@Nonnull String featureKey,
                                                 @Nonnull String userId) {
        return getAllFeatureVariables(featureKey, userId, Collections.<String, String>emptyMap());
    }

    /**
     * Get the values of all variables in the feature.
     *
     * @param featureKey  The unique key of the feature.
     * @param userId      The ID of the user.
     * @param attributes  The user's attributes.
     * @return An OptimizelyJSON instance for all variable values.
     * Null if the feature could not be found.
     *
     */
    @Nullable
    public OptimizelyJSON getAllFeatureVariables(@Nonnull String featureKey,
                                                 @Nonnull String userId,
                                                 @Nonnull Map<String, ?> attributes) {

        if (featureKey == null) {
            logger.warn("The featureKey parameter must be nonnull.");
            return null;
        } else if (userId == null) {
            logger.warn("The userId parameter must be nonnull.");
            return null;
        }

        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing getAllFeatureVariableValues call. type");
            return null;
        }

        FeatureFlag featureFlag = projectConfig.getFeatureKeyMapping().get(featureKey);
        if (featureFlag == null) {
            logger.info("No feature flag was found for key \"{}\".", featureKey);
            return null;
        }

        Map<String, ?> copiedAttributes = copyAttributes(attributes);
        FeatureDecision featureDecision = decisionService.getVariationForFeature(featureFlag, createUserContextCopy(userId, copiedAttributes), projectConfig, Collections.emptyList()).getResult();
        Boolean featureEnabled = false;
        Variation variation = featureDecision.variation;

        if (variation != null) {
            featureEnabled = variation.getFeatureEnabled();
            if (featureEnabled) {
                logger.info("Feature \"{}\" is enabled for user \"{}\".", featureKey, userId);
            } else {
                logger.info("Feature \"{}\" is not enabled for user \"{}\".", featureKey, userId);
            }
        } else {
            logger.info("User \"{}\" was not bucketed into any variation for feature flag \"{}\". " +
                "The default values are being returned.", userId, featureKey);
        }

        Map<String, Object> valuesMap = new HashMap<String, Object>();
        for (FeatureVariable variable : featureFlag.getVariables()) {
            String value = variable.getDefaultValue();
            if (featureEnabled) {
                FeatureVariableUsageInstance instance = variation.getVariableIdToFeatureVariableUsageInstanceMap().get(variable.getId());
                if (instance != null) {
                    value = instance.getValue();
                }
            }

            Object convertedValue = convertStringToType(value, variable.getType());
            if (convertedValue instanceof OptimizelyJSON) {
                convertedValue = ((OptimizelyJSON) convertedValue).toMap();
            }

            valuesMap.put(variable.getKey(), convertedValue);
        }

        DecisionNotification decisionNotification = DecisionNotification.newFeatureVariableDecisionNotificationBuilder()
            .withUserId(userId)
            .withAttributes(copiedAttributes)
            .withFeatureKey(featureKey)
            .withFeatureEnabled(featureEnabled)
            .withVariableValues(valuesMap)
            .withFeatureDecision(featureDecision)
            .build();

        notificationCenter.send(decisionNotification);

        return new OptimizelyJSON(valuesMap);
    }

    /**
     * Get the list of features that are enabled for the user.
     * TODO revisit this method. Calling this as-is can dramatically increase visitor impression counts.
     *
     * @param userId     The ID of the user.
     * @param attributes The user's attributes.
     * @return List of the feature keys that are enabled for the user if the userId is empty it will
     * return Empty List.
     *
     */
    public List<String> getEnabledFeatures(@Nonnull String userId, @Nonnull Map<String, ?> attributes) {
        List<String> enabledFeaturesList = new ArrayList();
        if (!validateUserId(userId)) {
            return enabledFeaturesList;
        }

        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing isFeatureEnabled call.");
            return enabledFeaturesList;
        }

        Map<String, ?> copiedAttributes = copyAttributes(attributes);
        for (FeatureFlag featureFlag : projectConfig.getFeatureFlags()) {
            String featureKey = featureFlag.getKey();
            if (isFeatureEnabled(projectConfig, featureKey, userId, copiedAttributes))
                enabledFeaturesList.add(featureKey);
        }

        return enabledFeaturesList;
    }

    //======== getVariation calls ========//

    @Nullable
    public Variation getVariation(@Nonnull Experiment experiment,
                                  @Nonnull String userId) throws UnknownExperimentException {

        return getVariation(experiment, userId, Collections.emptyMap());
    }

    @Nullable
    public Variation getVariation(@Nonnull Experiment experiment,
                                  @Nonnull String userId,
                                  @Nonnull Map<String, ?> attributes) throws UnknownExperimentException {
        return getVariation(getProjectConfig(), experiment, userId, attributes);
    }

    @Nullable
    private Variation getVariation(@Nonnull ProjectConfig projectConfig,
                                   @Nonnull Experiment experiment,
                                   @Nonnull String userId,
                                   @Nonnull Map<String, ?> attributes) throws UnknownExperimentException {
        Map<String, ?> copiedAttributes = copyAttributes(attributes);
        Variation variation = decisionService.getVariation(experiment, createUserContextCopy(userId, copiedAttributes), projectConfig).getResult();
        String notificationType = NotificationCenter.DecisionNotificationType.AB_TEST.toString();

        if (projectConfig.getExperimentFeatureKeyMapping().get(experiment.getId()) != null) {
            notificationType = NotificationCenter.DecisionNotificationType.FEATURE_TEST.toString();
        }

        DecisionNotification decisionNotification = DecisionNotification.newExperimentDecisionNotificationBuilder()
            .withUserId(userId)
            .withAttributes(copiedAttributes)
            .withExperimentKey(experiment.getKey())
            .withVariation(variation)
            .withType(notificationType)
            .build();

        notificationCenter.send(decisionNotification);

        return variation;
    }

    @Nullable
    public Variation getVariation(@Nonnull String experimentKey,
                                  @Nonnull String userId) throws UnknownExperimentException {

        return getVariation(experimentKey, userId, Collections.<String, String>emptyMap());
    }

    @Nullable
    public Variation getVariation(@Nonnull String experimentKey,
                                  @Nonnull String userId,
                                  @Nonnull Map<String, ?> attributes) {
        if (!validateUserId(userId)) {
            return null;
        }

        if (experimentKey == null || experimentKey.trim().isEmpty()) {
            logger.error("The experimentKey parameter must be nonnull.");
            return null;
        }

        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing isFeatureEnabled call.");
            return null;
        }

        Experiment experiment = projectConfig.getExperimentForKey(experimentKey, errorHandler);
        if (experiment == null) {
            // if we're unable to retrieve the associated experiment, return null
            return null;
        }

        return getVariation(projectConfig, experiment, userId, attributes);
    }

    /**
     * Force a user into a variation for a given experiment.
     * The forced variation value does not persist across application launches.
     * If the experiment key is not in the project file, this call fails and returns false.
     * If the variationKey is not in the experiment, this call fails.
     *
     * @param experimentKey The key for the experiment.
     * @param userId        The user ID to be used for bucketing.
     * @param variationKey  The variation key to force the user into.  If the variation key is null
     *                      then the forcedVariation for that experiment is removed.
     * @return boolean A boolean value that indicates if the set completed successfully.
     */
    public boolean setForcedVariation(@Nonnull String experimentKey,
                                      @Nonnull String userId,
                                      @Nullable String variationKey) {
        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing isFeatureEnabled call.");
            return false;
        }

        // if the experiment is not a valid experiment key, don't set it.
        Experiment experiment = projectConfig.getExperimentKeyMapping().get(experimentKey);
        if (experiment == null) {
            logger.error("Experiment {} does not exist in ProjectConfig for project {}", experimentKey, projectConfig.getProjectId());
            return false;
        }

        // TODO this is problematic if swapping out ProjectConfigs.
        // This state should be represented elsewhere like in a ephemeral UserProfileService.
        return decisionService.setForcedVariation(experiment, userId, variationKey);
    }

    /**
     * Gets the forced variation for a given user and experiment.
     * This method just calls into the {@link DecisionService#getForcedVariation(Experiment, String)}
     * method of the same signature.
     *
     * @param experimentKey The key for the experiment.
     * @param userId        The user ID to be used for bucketing.
     * @return The variation the user was bucketed into. This value can be null if the
     * forced variation fails.
     */
    @Nullable
    public Variation getForcedVariation(@Nonnull String experimentKey,
                                        @Nonnull String userId) {
        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing getForcedVariation call.");
            return null;
        }

        Experiment experiment = projectConfig.getExperimentKeyMapping().get(experimentKey);
        if (experiment == null) {
            logger.debug("No experiment \"{}\" mapped to user \"{}\" in the forced variation map ", experimentKey, userId);
            return null;
        }

        return decisionService.getForcedVariation(experiment, userId).getResult();
    }

    /**
     * @return the current {@link ProjectConfig} instance.
     */
    @Nullable
    public ProjectConfig getProjectConfig() {
        return projectConfigManager.getConfig();
    }

    @Nullable
    public UserProfileService getUserProfileService() {
        return userProfileService;
    }

    //======== Helper methods ========//

    /**
     * Helper function to check that the provided userId is valid
     *
     * @param userId the userId being validated
     * @return whether the user ID is valid
     */
    private boolean validateUserId(String userId) {
        if (userId == null) {
            logger.error("The user ID parameter must be nonnull.");
            return false;
        }

        return true;
    }

    /**
     * Get {@link OptimizelyConfig} containing experiments and features map
     *
     * @return {@link OptimizelyConfig}
     */
    public OptimizelyConfig getOptimizelyConfig() {
        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing getOptimizelyConfig call.");
            return null;
        }
        if (optimizelyConfigManager != null) {
            return optimizelyConfigManager.getOptimizelyConfig();
        }
        // Generate and return a new OptimizelyConfig object as a fallback when consumer implements their own ProjectConfigManager without implementing OptimizelyConfigManager.
        logger.debug("optimizelyConfigManager is null, generating new OptimizelyConfigObject as a fallback");
        return new OptimizelyConfigService(projectConfig).getConfig();
    }

    //============ decide ============//

    /**
     * Create a context of the user for which decision APIs will be called.
     *
     * A user context will be created successfully even when the SDK is not fully configured yet.
     *
     * @param userId The user ID to be used for bucketing.
     * @param attributes: A map of attribute names to current user attribute values.
     * @return An OptimizelyUserContext associated with this OptimizelyClient.
     */
    public OptimizelyUserContext createUserContext(@Nonnull String userId,
                                                   @Nonnull Map<String, ?> attributes) {
        if (userId == null) {
            logger.warn("The userId parameter must be nonnull.");
            return null;
        }

        return new OptimizelyUserContext(this, userId, attributes);
    }

    public OptimizelyUserContext createUserContext(@Nonnull String userId) {
        return new OptimizelyUserContext(this, userId);
    }

    private OptimizelyUserContext createUserContextCopy(@Nonnull String userId, @Nonnull Map<String, ?> attributes) {
        if (userId == null) {
            logger.warn("The userId parameter must be nonnull.");
            return null;
        }
        return new OptimizelyUserContext(this, userId, attributes, Collections.EMPTY_MAP, null, false);
    }

    OptimizelyDecision decide(@Nonnull OptimizelyUserContext user,
                              @Nonnull String key,
                              @Nonnull List<OptimizelyDecideOption> options) {

        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            return OptimizelyDecision.newErrorDecision(key, user, DecisionMessage.SDK_NOT_READY.reason());
        }

        FeatureFlag flag = projectConfig.getFeatureKeyMapping().get(key);
        if (flag == null) {
            return OptimizelyDecision.newErrorDecision(key, user, DecisionMessage.FLAG_KEY_INVALID.reason(key));
        }

        String userId = user.getUserId();
        Map<String, Object> attributes = user.getAttributes();
        Boolean decisionEventDispatched = false;
        List<OptimizelyDecideOption> allOptions = getAllOptions(options);
        DecisionReasons decisionReasons = DefaultDecisionReasons.newInstance(allOptions);

        Map<String, ?> copiedAttributes = new HashMap<>(attributes);
        FeatureDecision flagDecision;

        // Check Forced Decision
        OptimizelyDecisionContext optimizelyDecisionContext = new OptimizelyDecisionContext(flag.getKey(), null);
        DecisionResponse<Variation> forcedDecisionVariation = decisionService.validatedForcedDecision(optimizelyDecisionContext, projectConfig, user);
        decisionReasons.merge(forcedDecisionVariation.getReasons());
        if (forcedDecisionVariation.getResult() != null) {
            flagDecision = new FeatureDecision(null, forcedDecisionVariation.getResult(), FeatureDecision.DecisionSource.FEATURE_TEST);
        } else {
            // Regular decision
            DecisionResponse<FeatureDecision> decisionVariation = decisionService.getVariationForFeature(
                flag,
                user,
                projectConfig,
                allOptions);
            flagDecision = decisionVariation.getResult();
            decisionReasons.merge(decisionVariation.getReasons());
        }

        Boolean flagEnabled = false;
        if (flagDecision.variation != null) {
            if (flagDecision.variation.getFeatureEnabled()) {
                flagEnabled = true;
            }
        }
        logger.info("Feature \"{}\" is enabled for user \"{}\"? {}", key, userId, flagEnabled);

        Map<String, Object> variableMap = new HashMap<>();
        if (!allOptions.contains(OptimizelyDecideOption.EXCLUDE_VARIABLES)) {
            DecisionResponse<Map<String, Object>> decisionVariables = getDecisionVariableMap(
                flag,
                flagDecision.variation,
                flagEnabled);
            variableMap = decisionVariables.getResult();
            decisionReasons.merge(decisionVariables.getReasons());
        }
        OptimizelyJSON optimizelyJSON = new OptimizelyJSON(variableMap);

        FeatureDecision.DecisionSource decisionSource = FeatureDecision.DecisionSource.ROLLOUT;
        if (flagDecision.decisionSource != null) {
            decisionSource = flagDecision.decisionSource;
        }

        List<String> reasonsToReport = decisionReasons.toReport();
        String variationKey = flagDecision.variation != null ? flagDecision.variation.getKey() : null;
        // TODO: add ruleKey values when available later. use a copy of experimentKey until then.
        //       add to event metadata as well (currently set to experimentKey)
        String ruleKey = flagDecision.experiment != null ? flagDecision.experiment.getKey() : null;

        if (!allOptions.contains(OptimizelyDecideOption.DISABLE_DECISION_EVENT)) {
            decisionEventDispatched = sendImpression(
                projectConfig,
                flagDecision.experiment,
                userId,
                copiedAttributes,
                flagDecision.variation,
                key,
                decisionSource.toString(),
                flagEnabled);
        }

        DecisionNotification decisionNotification = DecisionNotification.newFlagDecisionNotificationBuilder()
            .withUserId(userId)
            .withAttributes(copiedAttributes)
            .withFlagKey(key)
            .withEnabled(flagEnabled)
            .withVariables(variableMap)
            .withVariationKey(variationKey)
            .withRuleKey(ruleKey)
            .withReasons(reasonsToReport)
            .withDecisionEventDispatched(decisionEventDispatched)
            .build();
        notificationCenter.send(decisionNotification);

        return new OptimizelyDecision(
            variationKey,
            flagEnabled,
            optimizelyJSON,
            ruleKey,
            key,
            user,
            reasonsToReport);
    }

    Map<String, OptimizelyDecision> decideForKeys(@Nonnull OptimizelyUserContext user,
                                                  @Nonnull List<String> keys,
                                                  @Nonnull List<OptimizelyDecideOption> options) {
        Map<String, OptimizelyDecision> decisionMap = new HashMap<>();

        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing isFeatureEnabled call.");
            return decisionMap;
        }

        if (keys.isEmpty()) return decisionMap;

        List<OptimizelyDecideOption> allOptions = getAllOptions(options);

        for (String key : keys) {
            OptimizelyDecision decision = decide(user, key, options);
            if (!allOptions.contains(OptimizelyDecideOption.ENABLED_FLAGS_ONLY) || decision.getEnabled()) {
                decisionMap.put(key, decision);
            }
        }

        return decisionMap;
    }

    Map<String, OptimizelyDecision> decideAll(@Nonnull OptimizelyUserContext user,
                                              @Nonnull List<OptimizelyDecideOption> options) {
        Map<String, OptimizelyDecision> decisionMap = new HashMap<>();

        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing isFeatureEnabled call.");
            return decisionMap;
        }

        List<FeatureFlag> allFlags = projectConfig.getFeatureFlags();
        List<String> allFlagKeys = new ArrayList<>();
        for (int i = 0; i < allFlags.size(); i++) allFlagKeys.add(allFlags.get(i).getKey());

        return decideForKeys(user, allFlagKeys, options);
    }

    private List<OptimizelyDecideOption> getAllOptions(List<OptimizelyDecideOption> options) {
        List<OptimizelyDecideOption> copiedOptions = new ArrayList(defaultDecideOptions);
        if (options != null) {
            copiedOptions.addAll(options);
        }
        return copiedOptions;
    }

    @Nonnull
    private DecisionResponse<Map<String, Object>> getDecisionVariableMap(@Nonnull FeatureFlag flag,
                                                                         @Nonnull Variation variation,
                                                                         @Nonnull Boolean featureEnabled) {
        DecisionReasons reasons = new DecisionReasons();

        Map<String, Object> valuesMap = new HashMap<String, Object>();
        for (FeatureVariable variable : flag.getVariables()) {
            String value = variable.getDefaultValue();
            if (featureEnabled) {
                FeatureVariableUsageInstance instance = variation.getVariableIdToFeatureVariableUsageInstanceMap().get(variable.getId());
                if (instance != null) {
                    value = instance.getValue();
                }
            }

            Object convertedValue = convertStringToType(value, variable.getType());
            if (convertedValue == null) {
                reasons.addError(DecisionMessage.VARIABLE_VALUE_INVALID.reason(variable.getKey()));
            } else if (convertedValue instanceof OptimizelyJSON) {
                convertedValue = ((OptimizelyJSON) convertedValue).toMap();
            }

            valuesMap.put(variable.getKey(), convertedValue);
        }

        return new DecisionResponse(valuesMap, reasons);
    }

    /**
     * Helper method which makes separate copy of attributesMap variable and returns it
     *
     * @param attributes map to copy
     * @return copy of attributes
     */
    private Map<String, ?> copyAttributes(Map<String, ?> attributes) {
        Map<String, ?> copiedAttributes = null;
        if (attributes != null) {
            copiedAttributes = new HashMap<>(attributes);
        }
        return copiedAttributes;
    }

    //======== Notification APIs ========//

    public NotificationCenter getNotificationCenter() {
        return notificationCenter;
    }

    /**
     * Convenience method for adding DecisionNotification Handlers
     *
     * @param handler DicisionNotification handler
     * @return A handler Id (greater than 0 if succeeded)
     */
    public int addDecisionNotificationHandler(NotificationHandler<DecisionNotification> handler) {
        return addNotificationHandler(DecisionNotification.class, handler);
    }

    /**
     * Convenience method for adding TrackNotification Handlers
     *
     * @param handler TrackNotification handler
     * @return A handler Id (greater than 0 if succeeded)
     */
    public int addTrackNotificationHandler(NotificationHandler<TrackNotification> handler) {
        return addNotificationHandler(TrackNotification.class, handler);
    }

    /**
     * Convenience method for adding UpdateConfigNotification Handlers
     *
     * @param handler UpdateConfigNotification handler
     * @return A handler Id (greater than 0 if succeeded)
     */
    public int addUpdateConfigNotificationHandler(NotificationHandler<UpdateConfigNotification> handler) {
        return addNotificationHandler(UpdateConfigNotification.class, handler);
    }

    /**
     * Convenience method for adding LogEvent Notification Handlers
     *
     * @param handler NotificationHandler handler
     * @return A handler Id (greater than 0 if succeeded)
     */
    public int addLogEventNotificationHandler(NotificationHandler<LogEvent> handler) {
        return addNotificationHandler(LogEvent.class, handler);
    }

    /**
     * Convenience method for adding NotificationHandlers
     *
     * @param clazz The class of NotificationHandler
     * @param handler NotificationHandler handler
     * @param <T> This is the type parameter
     * @return A handler Id (greater than 0 if succeeded)
     */
    public <T> int addNotificationHandler(Class<T> clazz, NotificationHandler<T> handler) {
        return notificationCenter.addNotificationHandler(clazz, handler);
    }

    public List<String> fetchQualifiedSegments(String userId, @Nonnull List<ODPSegmentOption> segmentOptions) {
        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing fetchQualifiedSegments call.");
            return null;
        }
        if (odpManager != null) {
            synchronized (odpManager) {
                return odpManager.getSegmentManager().getQualifiedSegments(userId, segmentOptions);
            }
        }
        logger.error("Audience segments fetch failed (ODP is not enabled).");
        return null;
    }

    public void fetchQualifiedSegments(String userId, ODPSegmentManager.ODPSegmentFetchCallback callback, List<ODPSegmentOption> segmentOptions) {
        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing fetchQualifiedSegments call.");
            callback.onCompleted(null);
            return;
        }
        if (odpManager == null) {
            logger.error("Audience segments fetch failed (ODP is not enabled).");
            callback.onCompleted(null);
        } else {
            odpManager.getSegmentManager().getQualifiedSegments(userId, callback, segmentOptions);
        }
    }

    @Nullable
    public ODPManager getODPManager() {
        return odpManager;
    }


    /**
     * Send an event to the ODP server.
     *
     * @param type the event type (default = "fullstack").
     * @param action the event action name.
     * @param identifiers a dictionary for identifiers. The caller must provide at least one key-value pair unless non-empty common identifiers have been set already with {@link ODPManager.Builder#withUserCommonIdentifiers(Map) }.
     * @param data a dictionary for associated data. The default event data will be added to this data before sending to the ODP server.
     */
    public void sendODPEvent(@Nullable String type, @Nonnull String action, @Nullable Map<String, String> identifiers, @Nullable Map<String, Object> data) {
        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing sendODPEvent call.");
            return;
        }
        if (odpManager != null) {
            if (action == null || action.trim().isEmpty()) {
                logger.error("ODP action is not valid (cannot be empty).");
                return;
            }

            ODPEvent event = new ODPEvent(type, action, identifiers, data);
            odpManager.getEventManager().sendEvent(event);
        } else {
            logger.error("ODP event send failed (ODP is not enabled)");
        }
    }

    public void identifyUser(@Nonnull String userId) {
        ProjectConfig projectConfig = getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing identifyUser call.");
            return;
        }
        ODPManager odpManager = getODPManager();
        if (odpManager != null) {
            odpManager.getEventManager().identifyUser(userId);
        }
    }

    private void updateODPSettings() {
        ProjectConfig projectConfig = projectConfigManager.getCachedConfig();
        if (odpManager != null && projectConfig != null) {
            odpManager.updateSettings(projectConfig.getHostForODP(), projectConfig.getPublicKeyForODP(), projectConfig.getAllSegments());
        }
    }

    //======== Builder ========//

    /**
     * This overloaded factory method is deprecated in favor of pure builder methods.
     * Please use {@link com.optimizely.ab.Optimizely#builder()} along with
     * {@link Builder#withDatafile(java.lang.String)} and
     * {@link Builder#withEventHandler(com.optimizely.ab.event.EventHandler)}
     * respectively.
     *
     * Example:
     * <pre>
     *     Optimizely optimizely = Optimizely.builder()
     *         .withDatafile(datafile)
     *         .withEventHandler(eventHandler)
     *         .build();
     * </pre>
     *
     * @param datafile  A datafile
     * @param eventHandler An EventHandler
     * @return An Optimizely builder
     */
    @Deprecated
    public static Builder builder(@Nonnull String datafile,
                                  @Nonnull EventHandler eventHandler) {

        return new Builder(datafile, eventHandler);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@link Optimizely} instance builder.
     * <p>
     * <b>NOTE</b>, the default value for {@link #eventHandler} is a {@link NoOpErrorHandler} instance, meaning that the
     * created {@link Optimizely} object will <b>NOT</b> throw exceptions unless otherwise specified.
     *
     * @see #builder(String, EventHandler)
     */
    public static class Builder {

        private String datafile;
        private Bucketer bucketer;
        private DecisionService decisionService;
        private ErrorHandler errorHandler;
        private EventHandler eventHandler;
        private EventProcessor eventProcessor;
        private ProjectConfig projectConfig;
        private ProjectConfigManager projectConfigManager;
        private OptimizelyConfigManager optimizelyConfigManager;
        private UserProfileService userProfileService;
        private NotificationCenter notificationCenter;
        private List<OptimizelyDecideOption> defaultDecideOptions;
        private ODPManager odpManager;

        // For backwards compatibility
        private AtomicProjectConfigManager fallbackConfigManager = new AtomicProjectConfigManager();

        @Deprecated
        public Builder(@Nonnull String datafile,
                       @Nonnull EventHandler eventHandler) {
            this.eventHandler = eventHandler;
            this.datafile = datafile;
        }

        public Builder() { }

        public Builder withErrorHandler(ErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        /**
         * The withEventHandler has been moved to the EventProcessor which takes a EventHandler in it's builder
         * method.
         * {@link com.optimizely.ab.event.BatchEventProcessor.Builder#withEventHandler(com.optimizely.ab.event.EventHandler)}  label}
         * Please use that builder method instead.
         *
         * @param eventHandler An EventHandler
         * @return An Optimizely builder
         */
        @Deprecated
        public Builder withEventHandler(EventHandler eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        /**
         * You can instantiate a BatchEventProcessor or a ForwardingEventProcessor or supply your own.
         *
         * @param eventProcessor An EventProcessor
         * @return An Optimizely builder
         */
        public Builder withEventProcessor(EventProcessor eventProcessor) {
            this.eventProcessor = eventProcessor;
            return this;
        }

        public Builder withUserProfileService(UserProfileService userProfileService) {
            this.userProfileService = userProfileService;
            return this;
        }

        /**
         * Override the SDK name and version (for client SDKs like android-sdk wrapping the core java-sdk) to be included in events.
         *
         * @param clientEngine the client engine type.
         * @param clientVersion the client SDK version.
         * @return An Optimizely builder
         */
        public Builder withClientInfo(EventBatch.ClientEngine clientEngine, String clientVersion) {
            ClientEngineInfo.setClientEngine(clientEngine);
            BuildVersionInfo.setClientVersion(clientVersion);
            return this;
        }

        @Deprecated
        public Builder withClientEngine(EventBatch.ClientEngine clientEngine) {
            logger.info("Deprecated. In the future, set ClientEngine via ClientEngineInfo#setClientEngine.");
            ClientEngineInfo.setClientEngine(clientEngine);
            return this;
        }

        @Deprecated
        public Builder withClientVersion(String clientVersion) {
            logger.info("Explicitly setting the ClientVersion is no longer supported.");
            return this;
        }

        public Builder withConfigManager(ProjectConfigManager projectConfigManager) {
            this.projectConfigManager = projectConfigManager;
            return this;
        }

        public Builder withNotificationCenter(NotificationCenter notificationCenter) {
            this.notificationCenter = notificationCenter;
            return this;
        }

        public Builder withDatafile(String datafile) {
            this.datafile = datafile;
            return this;
        }

        public Builder withDefaultDecideOptions(List<OptimizelyDecideOption> defaultDecideOtions) {
            this.defaultDecideOptions = defaultDecideOtions;
            return this;
        }

        public Builder withODPManager(ODPManager odpManager) {
            this.odpManager = odpManager;
            return this;
        }

        // Helper functions for making testing easier
        protected Builder withBucketing(Bucketer bucketer) {
            this.bucketer = bucketer;
            return this;
        }

        protected Builder withConfig(ProjectConfig projectConfig) {
            this.projectConfig = projectConfig;
            return this;
        }

        protected Builder withDecisionService(DecisionService decisionService) {
            this.decisionService = decisionService;
            return this;
        }

        public Optimizely build() {

            if (errorHandler == null) {
                errorHandler = new NoOpErrorHandler();
            }

            if (eventHandler == null) {
                eventHandler = new NoopEventHandler();
            }

            if (bucketer == null) {
                bucketer = new Bucketer();
            }

            if (decisionService == null) {
                decisionService = new DecisionService(bucketer, errorHandler, userProfileService);
            }

            if (projectConfig == null && datafile != null && !datafile.isEmpty()) {
                try {
                    projectConfig = new DatafileProjectConfig.Builder().withDatafile(datafile).build();
                    logger.info("Datafile successfully loaded with revision: {}", projectConfig.getRevision());
                } catch (ConfigParseException ex) {
                    logger.error("Unable to parse the datafile", ex);
                    logger.info("Datafile is invalid");
                    errorHandler.handleError(new OptimizelyRuntimeException(ex));
                }
            }

            if (projectConfig != null) {
                fallbackConfigManager.setConfig(projectConfig);
            }

            if (projectConfigManager == null) {
                projectConfigManager = fallbackConfigManager;
            }

            // PollingProjectConfigManager now also implements OptimizelyConfigManager interface to support OptimizelyConfig API.
            // This check is needed in case a consumer provides their own ProjectConfigManager which does nt implement OptimizelyConfigManager interface
            if (projectConfigManager instanceof OptimizelyConfigManager) {
                optimizelyConfigManager = (OptimizelyConfigManager) projectConfigManager;
            }

            if (notificationCenter == null) {
                notificationCenter = new NotificationCenter();
            }

            // For backwards compatibility
            if (eventProcessor == null) {
                eventProcessor = new ForwardingEventProcessor(eventHandler, notificationCenter);
            }

            if (defaultDecideOptions != null) {
                defaultDecideOptions = Collections.unmodifiableList(defaultDecideOptions);
            } else {
                defaultDecideOptions = Collections.emptyList();
            }

            return new Optimizely(eventHandler, eventProcessor, errorHandler, decisionService, userProfileService, projectConfigManager, optimizelyConfigManager, notificationCenter, defaultDecideOptions, odpManager);
        }
    }
}

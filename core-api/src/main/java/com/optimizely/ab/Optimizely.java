/****************************************************************************
 * Copyright 2016-2018, Optimizely, Inc. and contributors                   *
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
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.LiveVariable;
import com.optimizely.ab.config.LiveVariableUsageInstance;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.config.parser.DefaultConfigParser;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.BuildVersionInfo;
import com.optimizely.ab.event.internal.EventBuilder;
import com.optimizely.ab.event.internal.payload.EventBatch.ClientEngine;
import com.optimizely.ab.faultinjection.ExceptionSpot;
import com.optimizely.ab.faultinjection.FaultInjectionManager;
import com.optimizely.ab.notification.NotificationCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class Optimizely {

    private static final Logger logger = LoggerFactory.getLogger(Optimizely.class);

    @VisibleForTesting DecisionService decisionService;
    @VisibleForTesting EventBuilder eventBuilder;
    @VisibleForTesting ProjectConfig projectConfig;
    @VisibleForTesting EventHandler eventHandler;
    @VisibleForTesting ErrorHandler errorHandler;
    public NotificationCenter notificationCenter = new NotificationCenter();

    @Nullable private UserProfileService userProfileService;

    private static void injectFault(ExceptionSpot spot) {
        FaultInjectionManager.getInstance().injectFault(spot);
    }
    private static void throwInjectedExceptionIfTreatmentDisabled() { FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled(); }

    private Optimizely(@Nonnull ProjectConfig projectConfig,
                       @Nonnull DecisionService decisionService,
                       @Nonnull EventHandler eventHandler,
                       @Nonnull EventBuilder eventBuilder,
                       @Nonnull ErrorHandler errorHandler,
                       @Nullable UserProfileService userProfileService) {

        try {

            injectFault(ExceptionSpot.Optimizely_Constructor_spot1);

            this.projectConfig = projectConfig;
            this.decisionService = decisionService;
            this.eventHandler = eventHandler;

            injectFault(ExceptionSpot.Optimizely_Constructor_spot2);

            this.eventBuilder = eventBuilder;
            this.errorHandler = errorHandler;
            this.userProfileService = userProfileService;

            injectFault(ExceptionSpot.Optimizely_Constructor_spot3);
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            this.projectConfig = null;
            this.decisionService = null;
            this.eventHandler = null;
            this.eventBuilder = null;
            this.errorHandler = null;
            this.userProfileService = null;
        }
    }

    // Do work here that should be done once per Optimizely lifecycle
    @VisibleForTesting
    void initialize() {

    }

    //======== activate calls ========//

    public @Nullable
    Variation activate(@Nonnull String experimentKey,
                       @Nonnull String userId) throws UnknownExperimentException {
        try {
            injectFault(ExceptionSpot.Optimizely_activate1_spot1);

            return activate(experimentKey, userId, Collections.<String, String>emptyMap());
        }
        catch (UnknownExperimentException e) {
            throw e;
        }
        catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    public @Nullable
    Variation activate(@Nonnull String experimentKey,
                       @Nonnull String userId,
                       @Nonnull Map<String, String> attributes) throws UnknownExperimentException {

        try {

            injectFault(ExceptionSpot.Optimizely_activate2_spot1);

            if (experimentKey == null) {
                logger.error("The experimentKey parameter must be nonnull.");
                return null;
            }

            if (!validateUserId(userId)) {
                logger.info("Not activating user for experiment \"{}\".", experimentKey);
                return null;
            }

            injectFault(ExceptionSpot.Optimizely_activate2_spot2);

            ProjectConfig currentConfig = getProjectConfig();

            Experiment experiment = currentConfig.getExperimentForKey(experimentKey, errorHandler);
            if (experiment == null) {
                // if we're unable to retrieve the associated experiment, return null
                logger.info("Not activating user \"{}\" for experiment \"{}\".", userId, experimentKey);
                return null;
            }

            injectFault(ExceptionSpot.Optimizely_activate2_spot3);

            return activate(currentConfig, experiment, userId, attributes);
        }
        catch (UnknownExperimentException e) {
            throw e;
        }
        catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    public @Nullable
    Variation activate(@Nonnull Experiment experiment,
                       @Nonnull String userId) {

        try {

            injectFault(ExceptionSpot.Optimizely_activate3_spot1);

            return activate(experiment, userId, Collections.<String, String>emptyMap());
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    public @Nullable
    Variation activate(@Nonnull Experiment experiment,
                       @Nonnull String userId,
                       @Nonnull Map<String, String> attributes) {

        try {
            injectFault(ExceptionSpot.Optimizely_activate4_spot1);

            ProjectConfig currentConfig = getProjectConfig();

            injectFault(ExceptionSpot.Optimizely_activate4_spot2);

            return activate(currentConfig, experiment, userId, attributes);
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    private @Nullable
    Variation activate(@Nonnull ProjectConfig projectConfig,
                       @Nonnull Experiment experiment,
                       @Nonnull String userId,
                       @Nonnull Map<String, String> attributes) {

        try {
            injectFault(ExceptionSpot.Optimizely_activate5_spot1);

            if (!validateUserId(userId)) {
                logger.info("Not activating user \"{}\" for experiment \"{}\".", userId, experiment.getKey());
                return null;
            }
            // determine whether all the given attributes are present in the project config. If not, filter out the unknown
            // attributes.
            Map<String, String> filteredAttributes = filterAttributes(projectConfig, attributes);

            injectFault(ExceptionSpot.Optimizely_activate5_spot2);

            // bucket the user to the given experiment and dispatch an impression event
            Variation variation = decisionService.getVariation(experiment, userId, filteredAttributes);
            if (variation == null) {
                logger.info("Not activating user \"{}\" for experiment \"{}\".", userId, experiment.getKey());
                return null;
            }

            sendImpression(projectConfig, experiment, userId, filteredAttributes, variation);

            injectFault(ExceptionSpot.Optimizely_activate5_spot3);

            return variation;
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }

    }

    private void sendImpression(@Nonnull ProjectConfig projectConfig,
                                @Nonnull Experiment experiment,
                                @Nonnull String userId,
                                @Nonnull Map<String, String> filteredAttributes,
                                @Nonnull Variation variation) {
        try {
            injectFault(ExceptionSpot.Optimizely_sendImpression_spot1);

            if (experiment.isRunning()) {
                LogEvent impressionEvent = eventBuilder.createImpressionEvent(
                        projectConfig,
                        experiment,
                        variation,
                        userId,
                        filteredAttributes);
                logger.info("Activating user \"{}\" in experiment \"{}\".", userId, experiment.getKey());
                logger.debug(
                        "Dispatching impression event to URL {} with params {} and payload \"{}\".",
                        impressionEvent.getEndpointUrl(), impressionEvent.getRequestParams(), impressionEvent.getBody());
                injectFault(ExceptionSpot.Optimizely_sendImpression_spot2);

                try {
                    eventHandler.dispatchEvent(impressionEvent);
                } catch (Exception e) {
                    logger.error("Unexpected exception in event dispatcher", e);
                }

                notificationCenter.sendNotifications(NotificationCenter.NotificationType.Activate, experiment, userId,
                        filteredAttributes, variation, impressionEvent);
            } else {
                logger.info("Experiment has \"Launched\" status so not dispatching event during activation.");
            }

            injectFault(ExceptionSpot.Optimizely_sendImpression_spot3);
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            // eat it
        }
    }

    //======== track calls ========//

    public void track(@Nonnull String eventName,
                      @Nonnull String userId) throws UnknownEventTypeException {
        try {
            injectFault(ExceptionSpot.Optimizely_track1_spot1);
            track(eventName, userId, Collections.<String, String>emptyMap(), Collections.<String, Object>emptyMap());
        }
        catch (UnknownEventTypeException e) {
            throw e;
        }
        catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            // eat it
        }
    }

    public void track(@Nonnull String eventName,
                      @Nonnull String userId,
                      @Nonnull Map<String, String> attributes) throws UnknownEventTypeException {
        try {
            injectFault(ExceptionSpot.Optimizely_track2_spot1);
            track(eventName, userId, attributes, Collections.<String, String>emptyMap());
        }
        catch (UnknownEventTypeException e) {
            throw e;
        }
        catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            // eat it
        }
    }

    public void track(@Nonnull String eventName,
                      @Nonnull String userId,
                      @Nonnull Map<String, String> attributes,
                      @Nonnull Map<String, ?> eventTags) throws UnknownEventTypeException {
        try {

            injectFault(ExceptionSpot.Optimizely_track3_spot1);

            if (!validateUserId(userId)) {
                logger.info("Not tracking event \"{}\".", eventName);
                return;
            }

            if (eventName == null || eventName.trim().isEmpty()) {
                logger.error("Event Key is null or empty when non-null and non-empty String was expected.");
                logger.info("Not tracking event for user \"{}\".", userId);
                return;
            }

            injectFault(ExceptionSpot.Optimizely_track3_spot2);

            ProjectConfig currentConfig = getProjectConfig();

            EventType eventType = currentConfig.getEventTypeForName(eventName, errorHandler);
            if (eventType == null) {
                // if no matching event type could be found, do not dispatch an event
                logger.info("Not tracking event \"{}\" for user \"{}\".", eventName, userId);
                return;
            }

            injectFault(ExceptionSpot.Optimizely_track3_spot3);

            // determine whether all the given attributes are present in the project config. If not, filter out the unknown
            // attributes.
            Map<String, String> filteredAttributes = filterAttributes(currentConfig, attributes);

            if (eventTags == null) {
                logger.warn("Event tags is null when non-null was expected. Defaulting to an empty event tags map.");
                eventTags = Collections.<String, String>emptyMap();
            }

            List<Experiment> experimentsForEvent = projectConfig.getExperimentsForEventKey(eventName);
            Map<Experiment, Variation> experimentVariationMap = new HashMap<Experiment, Variation>(experimentsForEvent.size());
            for (Experiment experiment : experimentsForEvent) {
                if (experiment.isRunning()) {
                    Variation variation = decisionService.getVariation(experiment, userId, filteredAttributes);
                    if (variation != null) {
                        experimentVariationMap.put(experiment, variation);
                    }
                } else {
                    logger.info(
                            "Not tracking event \"{}\" for experiment \"{}\" because experiment has status \"Launched\".",
                            eventType.getKey(), experiment.getKey());
                }
            }

            injectFault(ExceptionSpot.Optimizely_track3_spot4);

            // create the conversion event request parameters, then dispatch
            LogEvent conversionEvent = eventBuilder.createConversionEvent(
                    projectConfig,
                    experimentVariationMap,
                    userId,
                    eventType.getId(),
                    eventType.getKey(),
                    filteredAttributes,
                    eventTags);

            injectFault(ExceptionSpot.Optimizely_track3_spot5);

            if (conversionEvent == null) {
                logger.info("There are no valid experiments for event \"{}\" to track.", eventName);
                logger.info("Not tracking event \"{}\" for user \"{}\".", eventName, userId);
                return;
            }

            logger.info("Tracking event \"{}\" for user \"{}\".", eventName, userId);
            logger.debug("Dispatching conversion event to URL {} with params {} and payload \"{}\".",
                    conversionEvent.getEndpointUrl(), conversionEvent.getRequestParams(), conversionEvent.getBody());
            try {
                eventHandler.dispatchEvent(conversionEvent);
            } catch (Exception e) {
                logger.error("Unexpected exception in event dispatcher", e);
            }

            notificationCenter.sendNotifications(NotificationCenter.NotificationType.Track, eventName, userId,
                    filteredAttributes, eventTags, conversionEvent);

            injectFault(ExceptionSpot.Optimizely_track3_spot6);
        }
        catch (UnknownEventTypeException e) {
            throw e;
        }
        catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            // eat it
        }
    }

    //======== FeatureFlag APIs ========//

    /**
     * Determine whether a boolean feature is enabled.
     * Send an impression event if the user is bucketed into an experiment using the feature.
     *
     * @param featureKey The unique key of the feature.
     * @param userId The ID of the user.
     * @return True if the feature is enabled.
     *         False if the feature is disabled.
     *         False if the feature is not found.
     */
    public @Nonnull Boolean isFeatureEnabled(@Nonnull String featureKey,
                                              @Nonnull String userId) {

        try {

            injectFault(ExceptionSpot.Optimizely_isFeatureEnabled1_spot1);

            return isFeatureEnabled(featureKey, userId, Collections.<String, String>emptyMap());
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return false;
        }
    }

    /**
     * Determine whether a boolean feature is enabled.
     * Send an impression event if the user is bucketed into an experiment using the feature.
     *
     * @param featureKey The unique key of the feature.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return True if the feature is enabled.
     *         False if the feature is disabled.
     *         False if the feature is not found.
     */
    public @Nonnull Boolean isFeatureEnabled(@Nonnull String featureKey,
                                              @Nonnull String userId,
                                              @Nonnull Map<String, String> attributes) {
        try {

            injectFault(ExceptionSpot.Optimizely_isFeatureEnabled2_spot1);

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

            injectFault(ExceptionSpot.Optimizely_isFeatureEnabled2_spot2);

            Map<String, String> filteredAttributes = filterAttributes(projectConfig, attributes);

            FeatureDecision featureDecision = decisionService.getVariationForFeature(featureFlag, userId, filteredAttributes);
            if (featureDecision.variation == null || !featureDecision.variation.getFeatureEnabled()) {
                logger.info("Feature \"{}\" is not enabled for user \"{}\".", featureKey, userId);
                injectFault(ExceptionSpot.Optimizely_isFeatureEnabled2_spot3);
                return false;
            } else {
                if (featureDecision.decisionSource.equals(FeatureDecision.DecisionSource.EXPERIMENT)) {
                    sendImpression(
                            projectConfig,
                            featureDecision.experiment,
                            userId,
                            filteredAttributes,
                            featureDecision.variation);
                } else {
                    logger.info("The user \"{}\" is not included in an experiment for feature \"{}\".",
                            userId, featureKey);
                }
                logger.info("Feature \"{}\" is enabled for user \"{}\".", featureKey, userId);
                injectFault(ExceptionSpot.Optimizely_isFeatureEnabled2_spot3);
                return true;
            }

        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return false;
        }
    }

    /**
     * Get the Boolean value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @return The Boolean value of the boolean single variable feature.
     *         Null if the feature could not be found.
     */
    public @Nullable Boolean getFeatureVariableBoolean(@Nonnull String featureKey,
                                                       @Nonnull String variableKey,
                                                       @Nonnull String userId) {
        try {
            injectFault(ExceptionSpot.Optimizely_getFeatureVariableBoolean1_spot1);

            return getFeatureVariableBoolean(featureKey, variableKey, userId, Collections.<String, String>emptyMap());
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return false;
        }
    }

    /**
     * Get the Boolean value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return The Boolean value of the boolean single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable Boolean getFeatureVariableBoolean(@Nonnull String featureKey,
                                                       @Nonnull String variableKey,
                                                       @Nonnull String userId,
                                                       @Nonnull Map<String, String> attributes) {

        try {

            injectFault(ExceptionSpot.Optimizely_getFeatureVariableBoolean2_spot1);

            String variableValue = getFeatureVariableValueForType(
                    featureKey,
                    variableKey,
                    userId,
                    attributes,
                    LiveVariable.VariableType.BOOLEAN
            );

            injectFault(ExceptionSpot.Optimizely_getFeatureVariableBoolean2_spot2);

            if (variableValue != null) {
                return Boolean.parseBoolean(variableValue);
            }

            injectFault(ExceptionSpot.Optimizely_getFeatureVariableBoolean2_spot3);

            return null;
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    /**
     * Get the Double value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @return The Double value of the double single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable Double getFeatureVariableDouble(@Nonnull String featureKey,
                                                     @Nonnull String variableKey,
                                                     @Nonnull String userId) {

        try {

            injectFault(ExceptionSpot.Optimizely_getFeatureVariableBoolean1_spot1);

            return getFeatureVariableDouble(featureKey, variableKey, userId, Collections.<String, String>emptyMap());
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    /**
     * Get the Double value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return The Double value of the double single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable Double getFeatureVariableDouble(@Nonnull String featureKey,
                                                     @Nonnull String variableKey,
                                                     @Nonnull String userId,
                                                     @Nonnull Map<String, String> attributes) {

        try {


        injectFault(ExceptionSpot.Optimizely_getFeatureVariableBoolean2_spot1);

        String variableValue = getFeatureVariableValueForType(
                featureKey,
                variableKey,
                userId,
                attributes,
                LiveVariable.VariableType.DOUBLE
        );

        injectFault(ExceptionSpot.Optimizely_getFeatureVariableBoolean2_spot2);

        if (variableValue != null) {
            try {
                return Double.parseDouble(variableValue);
            } catch (NumberFormatException exception) {
                logger.error("NumberFormatException while trying to parse \"" + variableValue +
                        "\" as Double. " + exception);
            }
        }

        injectFault(ExceptionSpot.Optimizely_getFeatureVariableBoolean2_spot3);

        return null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the Integer value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @return The Integer value of the integer single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable Integer getFeatureVariableInteger(@Nonnull String featureKey,
                                                       @Nonnull String variableKey,
                                                       @Nonnull String userId) {

        try {

        injectFault(ExceptionSpot.Optimizely_getFeatureVariableInteger1_spot1);

        return getFeatureVariableInteger(featureKey, variableKey, userId, Collections.<String, String>emptyMap());

        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    /**
     * Get the Integer value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return The Integer value of the integer single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable Integer getFeatureVariableInteger(@Nonnull String featureKey,
                                                       @Nonnull String variableKey,
                                                       @Nonnull String userId,
                                                       @Nonnull Map<String, String> attributes) {
        try {

        injectFault(ExceptionSpot.Optimizely_getFeatureVariableInteger2_spot1);

        String variableValue = getFeatureVariableValueForType(
                featureKey,
                variableKey,
                userId,
                attributes,
                LiveVariable.VariableType.INTEGER
        );

        injectFault(ExceptionSpot.Optimizely_getFeatureVariableInteger2_spot2);

        if (variableValue != null) {
            try {
                return Integer.parseInt(variableValue);
            } catch (NumberFormatException exception) {
                logger.error("NumberFormatException while trying to parse \"" + variableValue +
                        "\" as Integer. " + exception.toString());
            }
        }

        injectFault(ExceptionSpot.Optimizely_getFeatureVariableInteger2_spot3);

        return null;

        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    /**
     * Get the String value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @return The String value of the string single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable String getFeatureVariableString(@Nonnull String featureKey,
                                                     @Nonnull String variableKey,
                                                     @Nonnull String userId) {

        try {

        injectFault(ExceptionSpot.Optimizely_getFeatureVariableString1_spot1);

        return getFeatureVariableString(featureKey, variableKey, userId, Collections.<String, String>emptyMap());

        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    /**
     * Get the String value of the specified variable in the feature.
     * @param featureKey The unique key of the feature.
     * @param variableKey The unique key of the variable.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return The String value of the string single variable feature.
     *         Null if the feature or variable could not be found.
     */
    public @Nullable String getFeatureVariableString(@Nonnull String featureKey,
                                                     @Nonnull String variableKey,
                                                     @Nonnull String userId,
                                                     @Nonnull Map<String, String> attributes) {

        try {

        injectFault(ExceptionSpot.Optimizely_getFeatureVariableString2_spot1);

        return getFeatureVariableValueForType(
                featureKey,
                variableKey,
                userId,
                attributes,
                LiveVariable.VariableType.STRING);

        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    @VisibleForTesting
    String getFeatureVariableValueForType(@Nonnull String featureKey,
                                                  @Nonnull String variableKey,
                                                  @Nonnull String userId,
                                                  @Nonnull Map<String, String> attributes,
                                                  @Nonnull LiveVariable.VariableType variableType) {
        try {

            injectFault(ExceptionSpot.Optimizely_getFeatureVariableValueForType_spot1);

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

            injectFault(ExceptionSpot.Optimizely_getFeatureVariableValueForType_spot2);

            FeatureFlag featureFlag = projectConfig.getFeatureKeyMapping().get(featureKey);
            if (featureFlag == null) {
                logger.info("No feature flag was found for key \"{}\".", featureKey);
                return null;
            }

            injectFault(ExceptionSpot.Optimizely_getFeatureVariableValueForType_spot3);

            LiveVariable variable = featureFlag.getVariableKeyToLiveVariableMap().get(variableKey);
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

            injectFault(ExceptionSpot.Optimizely_getFeatureVariableValueForType_spot4);

            FeatureDecision featureDecision = decisionService.getVariationForFeature(featureFlag, userId, attributes);
            if (featureDecision.variation != null) {
                LiveVariableUsageInstance liveVariableUsageInstance =
                        featureDecision.variation.getVariableIdToLiveVariableUsageInstanceMap().get(variable.getId());
                if (liveVariableUsageInstance != null) {
                    variableValue = liveVariableUsageInstance.getValue();
                } else {
                    variableValue = variable.getDefaultValue();
                }
            } else {
                logger.info("User \"{}\" was not bucketed into any variation for feature flag \"{}\". " +
                                "The default value \"{}\" for \"{}\" is being returned.",
                        userId, featureKey, variableValue, variableKey
                );
            }

            injectFault(ExceptionSpot.Optimizely_getFeatureVariableValueForType_spot5);

            return variableValue;

        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    /**
     * Get the list of features that are enabled for the user.
     * @param userId The ID of the user.
     * @param attributes The user's attributes.
     * @return List of the feature keys that are enabled for the user if the userId is empty it will
     * return Empty List.
     */
    public List<String> getEnabledFeatures(@Nonnull String userId, @Nonnull Map<String, String> attributes) {

        try {

            List<String> enabledFeaturesList = new ArrayList<String>();

            injectFault(ExceptionSpot.Optimizely_getEnabledFeatures_spot1);

            if (!validateUserId(userId)) {
                return enabledFeaturesList;
            }

            injectFault(ExceptionSpot.Optimizely_getEnabledFeatures_spot2);

            for (FeatureFlag featureFlag : projectConfig.getFeatureFlags()) {
                String featureKey = featureFlag.getKey();
                if (isFeatureEnabled(featureKey, userId, attributes))
                    enabledFeaturesList.add(featureKey);
            }

            injectFault(ExceptionSpot.Optimizely_getEnabledFeatures_spot3);

            return enabledFeaturesList;

        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    //======== getVariation calls ========//

    public @Nullable
    Variation getVariation(@Nonnull Experiment experiment,
                           @Nonnull String userId) throws UnknownExperimentException {
        try {

            injectFault(ExceptionSpot.Optimizely_getVariation1_spot1);

            return getVariation(experiment, userId, Collections.<String, String>emptyMap());
        }
        catch (UnknownExperimentException e) {
            throw e;
        }
        catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    public @Nullable
    Variation getVariation(@Nonnull Experiment experiment,
                           @Nonnull String userId,
                           @Nonnull Map<String, String> attributes) throws UnknownExperimentException {

        try {

            Map<String, String> filteredAttributes = filterAttributes(projectConfig, attributes);

            injectFault(ExceptionSpot.Optimizely_getVariation2_spot1);

            return decisionService.getVariation(experiment, userId, filteredAttributes);
        }
        catch (UnknownExperimentException e) {
            throw e;
        }
        catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    public @Nullable
    Variation getVariation(@Nonnull String experimentKey,
                           @Nonnull String userId) throws UnknownExperimentException {

        try {

            injectFault(ExceptionSpot.Optimizely_getVariation3_spot1);

            return getVariation(experimentKey, userId, Collections.<String, String>emptyMap());

        }
        catch (UnknownExperimentException ue) {
            throw ue;
        }
        catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    public @Nullable
    Variation getVariation(@Nonnull String experimentKey,
                           @Nonnull String userId,
                           @Nonnull Map<String, String> attributes) {

        try {

            if (!validateUserId(userId)) {
                return null;
            }

            injectFault(ExceptionSpot.Optimizely_getVariation4_spot1);

            if (experimentKey == null || experimentKey.trim().isEmpty()) {
                logger.error("The experimentKey parameter must be nonnull.");
                return null;
            }

            injectFault(ExceptionSpot.Optimizely_getVariation4_spot2);

            ProjectConfig currentConfig = getProjectConfig();

            injectFault(ExceptionSpot.Optimizely_getVariation4_spot3);

            Experiment experiment = currentConfig.getExperimentForKey(experimentKey, errorHandler);
            if (experiment == null) {
                // if we're unable to retrieve the associated experiment, return null
                return null;
            }

            injectFault(ExceptionSpot.Optimizely_getVariation4_spot4);

            Map<String, String> filteredAttributes = filterAttributes(projectConfig, attributes);

            injectFault(ExceptionSpot.Optimizely_getVariation4_spot5);

            return decisionService.getVariation(experiment, userId, filteredAttributes);
        } catch (UnknownExperimentException ue) {
            throw ue;
        }
        catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    /**
     * Force a user into a variation for a given experiment.
     * The forced variation value does not persist across application launches.
     * If the experiment key is not in the project file, this call fails and returns false.
     * If the variationKey is not in the experiment, this call fails.
     * @param experimentKey The key for the experiment.
     * @param userId The user ID to be used for bucketing.
     * @param variationKey The variation key to force the user into.  If the variation key is null
     *                     then the forcedVariation for that experiment is removed.
     *
     * @return boolean A boolean value that indicates if the set completed successfully.
     */
    public boolean setForcedVariation(@Nonnull String experimentKey,
                                      @Nonnull String userId,
                                      @Nullable String variationKey) {
        try {

            injectFault(ExceptionSpot.Optimizely_setForcedVariation_spot1);

            return projectConfig.setForcedVariation(experimentKey, userId, variationKey);
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return false;
        }
    }

    /**
     * Gets the forced variation for a given user and experiment.
     * This method just calls into the {@link com.optimizely.ab.config.ProjectConfig#getForcedVariation(String, String)}
     * method of the same signature.
     *
     * @param experimentKey The key for the experiment.
     * @param userId The user ID to be used for bucketing.
     *
     * @return The variation the user was bucketed into. This value can be null if the
     * forced variation fails.
     */
    public @Nullable Variation getForcedVariation(@Nonnull String experimentKey,
                                        @Nonnull String userId) {
        try {
            injectFault(ExceptionSpot.Optimizely_getForcedVariation_spot1);

            return projectConfig.getForcedVariation(experimentKey, userId);
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    /**
     * @return the current {@link ProjectConfig} instance.
     */
    public @Nonnull ProjectConfig getProjectConfig() {
        try {


            injectFault(ExceptionSpot.Optimizely_getProjectConfig1_spot1);

            return projectConfig;
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    /**
     * @return a {@link ProjectConfig} instance given a json string
     */
    private static ProjectConfig getProjectConfig(String datafile) throws ConfigParseException {
        try {

            if (datafile == null) {
                throw new ConfigParseException("Unable to parse null datafile.");
            }

            injectFault(ExceptionSpot.Optimizely_getProjectConfig2_spot1);

            if (datafile.length() == 0) {
                throw new ConfigParseException("Unable to parse empty datafile.");
            }

            injectFault(ExceptionSpot.Optimizely_getProjectConfig2_spot2);

            ProjectConfig projectConfig = DefaultConfigParser.getInstance().parseProjectConfig(datafile);

            injectFault(ExceptionSpot.Optimizely_getProjectConfig2_spot3);

            if (projectConfig.getVersion().equals("1")) {
                throw new ConfigParseException("This version of the Java SDK does not support version 1 datafiles. " +
                        "Please use a version 2 or 3 datafile with this SDK.");
            }

            injectFault(ExceptionSpot.Optimizely_getProjectConfig2_spot4);

            return projectConfig;
        }
        catch (ConfigParseException e) {
            throw e;
        }
        catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    @Nullable
    public UserProfileService getUserProfileService() {
        try {

            injectFault(ExceptionSpot.Optimizely_getUserProfileService_spot1);

            return userProfileService;
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    //======== Helper methods ========//

    /**
     * Helper method to verify that the given attributes map contains only keys that are present in the
     * {@link ProjectConfig}.
     *
     * @param projectConfig the current project config
     * @param attributes the attributes map to validate and potentially filter. The reserved key for bucketing id
     * {@link DecisionService#BUCKETING_ATTRIBUTE} is kept.
     * @return the filtered attributes map (containing only attributes that are present in the project config) or an
     * empty map if a null attributes object is passed in
     */
    private Map<String, String> filterAttributes(@Nonnull ProjectConfig projectConfig,
                                                 @Nonnull Map<String, String> attributes) {

        try {
            injectFault(ExceptionSpot.Optimizely_filterAttribute_spot1);

            if (attributes == null) {
                logger.warn("Attributes is null when non-null was expected. Defaulting to an empty attributes map.");
                return Collections.<String, String>emptyMap();
            }

            injectFault(ExceptionSpot.Optimizely_filterAttribute_spot2);

            List<String> unknownAttributes = null;

            Map<String, Attribute> attributeKeyMapping = projectConfig.getAttributeKeyMapping();
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                if (!attributeKeyMapping.containsKey(attribute.getKey()) &&
                        attribute.getKey() != com.optimizely.ab.bucketing.DecisionService.BUCKETING_ATTRIBUTE) {
                    if (unknownAttributes == null) {
                        unknownAttributes = new ArrayList<String>();
                    }
                    unknownAttributes.add(attribute.getKey());
                }
            }

            injectFault(ExceptionSpot.Optimizely_filterAttribute_spot3);

            if (unknownAttributes != null) {
                logger.warn("Attribute(s) {} not in the datafile.", unknownAttributes);
                // make a copy of the passed through attributes, then remove the unknown list
                attributes = new HashMap<String, String>(attributes);
                for (String unknownAttribute : unknownAttributes) {
                    attributes.remove(unknownAttribute);
                }
            }

            injectFault(ExceptionSpot.Optimizely_filterAttribute_spot4);

            return attributes;
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    /**
     * Helper function to check that the provided userId is valid
     *
     * @param userId the userId being validated
     * @return whether the user ID is valid
     */
    private boolean validateUserId(String userId) {

        try {

            injectFault(ExceptionSpot.Optimizely_validateUserId_spot1);

            if (userId == null) {
                logger.error("The user ID parameter must be nonnull.");
                return false;
            }

            injectFault(ExceptionSpot.Optimizely_validateUserId_spot2);

            if (userId.trim().isEmpty()) {
                logger.error("Non-empty user ID required");
                return false;
            }

            injectFault(ExceptionSpot.Optimizely_validateUserId_spot3);

            return true;
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return false;
        }
    }

    //======== Builder ========//

    public static Builder builder(@Nonnull String datafile,
                                  @Nonnull EventHandler eventHandler) {
        try {

            injectFault(ExceptionSpot.Optimizely_builder_spot1);

            return new Builder(datafile, eventHandler);
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
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
        private EventBuilder eventBuilder;
        private ClientEngine clientEngine;
        private String clientVersion;
        private ProjectConfig projectConfig;
        private UserProfileService userProfileService;

        public Builder(@Nonnull String datafile,
                       @Nonnull EventHandler eventHandler) {
            try {
                injectFault(ExceptionSpot.Builder_constructor_spot1);
                this.datafile = datafile;
                this.eventHandler = eventHandler;
            } catch (Exception e) {
                throwInjectedExceptionIfTreatmentDisabled();
                // eat it
            }
        }

        protected Builder withBucketing(Bucketer bucketer) {
            try {
                injectFault(ExceptionSpot.Builder_withBucketing_spot1);
                this.bucketer = bucketer;
                return this;
            } catch (Exception e) {
                throwInjectedExceptionIfTreatmentDisabled();
                return this;
            }
        }

        protected Builder withDecisionService(DecisionService decisionService) {
            try {
                injectFault(ExceptionSpot.Builder_withDecisionService_spot1);
                this.decisionService = decisionService;
                return this;
            } catch (Exception e) {
                throwInjectedExceptionIfTreatmentDisabled();
                return this;
            }
        }

        public Builder withErrorHandler(ErrorHandler errorHandler) {
            try {
                injectFault(ExceptionSpot.Builder_withErrorHandler_spot1);
                this.errorHandler = errorHandler;
                return this;
            } catch (Exception e) {
                throwInjectedExceptionIfTreatmentDisabled();
                return this;
            }
        }

        public Builder withUserProfileService(UserProfileService userProfileService) {
            try {
                injectFault(ExceptionSpot.Builder_withUserProfileService_spot1);
                this.userProfileService = userProfileService;
                return this;
            } catch (Exception e) {
                throwInjectedExceptionIfTreatmentDisabled();
                return this;
            }
        }

        public Builder withClientEngine(ClientEngine clientEngine) {
            try {
                injectFault(ExceptionSpot.Builder_withClientEngine_spot1);
                this.clientEngine = clientEngine;
                return this;
            } catch (Exception e) {
                throwInjectedExceptionIfTreatmentDisabled();
                return this;
            }
        }

        public Builder withClientVersion(String clientVersion) {
            try {
                injectFault(ExceptionSpot.Builder_withClientVersion_spot1);
                this.clientVersion = clientVersion;
                return this;
            } catch (Exception e) {
                throwInjectedExceptionIfTreatmentDisabled();
                return this;
            }
        }

        protected Builder withEventBuilder(EventBuilder eventBuilder) {
            try {
                injectFault(ExceptionSpot.Builder_withEventBuilder_spot1);
                this.eventBuilder = eventBuilder;
                return this;
            } catch (Exception e) {
                throwInjectedExceptionIfTreatmentDisabled();
                return this;
            }
        }

        // Helper function for making testing easier
        protected Builder withConfig(ProjectConfig projectConfig) {
            try {
                injectFault(ExceptionSpot.Builder_withConfig_spot1);
                this.projectConfig = projectConfig;
                return this;
            } catch (Exception e) {
                throwInjectedExceptionIfTreatmentDisabled();
                return this;
            }
        }

        public Optimizely build() throws ConfigParseException {

            try {

                injectFault(ExceptionSpot.Builder_build_spot1);
                if (projectConfig == null) {
                    projectConfig = Optimizely.getProjectConfig(datafile);
                }

                injectFault(ExceptionSpot.Builder_build_spot2);

                if (bucketer == null) {
                    bucketer = new Bucketer(projectConfig);
                }

                injectFault(ExceptionSpot.Builder_build_spot3);

                if (clientEngine == null) {
                    clientEngine = ClientEngine.JAVA_SDK;
                }

                injectFault(ExceptionSpot.Builder_build_spot4);

                if (clientVersion == null) {
                    clientVersion = BuildVersionInfo.VERSION;
                }

                injectFault(ExceptionSpot.Builder_build_spot5);

                if (eventBuilder == null) {
                    eventBuilder = new EventBuilder(clientEngine, clientVersion);
                }

                injectFault(ExceptionSpot.Builder_build_spot6);

                if (errorHandler == null) {
                    errorHandler = new NoOpErrorHandler();
                }

                injectFault(ExceptionSpot.Builder_build_spot7);

                if (decisionService == null) {
                    decisionService = new DecisionService(bucketer, errorHandler, projectConfig, userProfileService);
                }

                injectFault(ExceptionSpot.Builder_build_spot8);

                Optimizely optimizely = new Optimizely(projectConfig, decisionService, eventHandler, eventBuilder, errorHandler, userProfileService);

                injectFault(ExceptionSpot.Builder_build_spot9);

                optimizely.initialize();

                injectFault(ExceptionSpot.Builder_build_spot10);

                return optimizely;

            } catch (ConfigParseException e) {
                throw e;
            } catch (Exception e) {
                throwInjectedExceptionIfTreatmentDisabled();
                return null;
            }

        }
    }
}

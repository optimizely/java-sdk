/**
 *
 *    Copyright 2020, Optimizely and contributors
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
package com.optimizely.ab.optimizelyusercontext;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.UnknownEventTypeException;
import com.optimizely.ab.bucketing.FeatureDecision;
import com.optimizely.ab.config.*;
import com.optimizely.ab.notification.DecisionNotification;
import com.optimizely.ab.notification.FeatureTestSourceInfo;
import com.optimizely.ab.notification.RolloutSourceInfo;
import com.optimizely.ab.notification.SourceInfo;
import com.optimizely.ab.optimizelyjson.OptimizelyJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

public class OptimizelyUserContext {
    @Nonnull
    private final String userId;

    @Nonnull
    private final Map<String, Object> attributes;

    @Nonnull
    private final Optimizely optimizely;

    private static final Logger logger = LoggerFactory.getLogger(OptimizelyUserContext.class);

    public final static String SDK_NOT_READY = "Optimizely SDK not configured properly yet";
    public final static String FLAG_KEY_INVALID = "Flag key \"%s\" is not in datafile.";
    public final static String VARIABLE_VALUE_INVALID = "Variable value for key \"%s\" is invalid or wrong type.";
    public final static String OPTIMIZELY_JSON_ERROR = "Invalid variables for OptimizelyJSON.";


    public OptimizelyUserContext(@Nonnull Optimizely optimizely,
                                 @Nonnull String userId,
                                 @Nonnull Map<String, ?> attributes) {
        this.optimizely = optimizely;
        this.userId = userId;
        this.attributes = new HashMap<>(attributes);
    }

    public OptimizelyUserContext(@Nonnull Optimizely optimizely, @Nonnull String userId) {
        this(optimizely, userId, new HashMap<>());
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Optimizely getOptimizely() {
        return optimizely;
    }

    /**
     * Set an attribute for a given key.
     *
     * @param key An attribute key
     * @param value An attribute value
     */
    public void setAttribute(@Nonnull String key, @Nonnull Object value) {
        attributes.put(key, value);
    }

    /**
     * Returns a decision result ({@link OptimizelyDecision}) for a given flag key and a user context, which contains all data required to deliver the flag.
     * <ul>
     * <li>If the SDK finds an error, it’ll return a decision with <b>null</b> for <b>variationKey</b>. The decision will include an error message in <b>reasons</b>.
     * </ul>
     * @param key A flag key for which a decision will be made.
     * @param options An array of options for decision-making.
     * @return A decision result.
     */
    public OptimizelyDecision decide(@Nonnull String key,
                                     @Nonnull OptimizelyDecideOption[] options) {

        ProjectConfig projectConfig = optimizely.getProjectConfig();
        if (projectConfig == null) {
            return OptimizelyDecision.createErrorDecision(key, this, SDK_NOT_READY);
        }

        FeatureFlag flag = projectConfig.getFeatureKeyMapping().get(key);
        if (flag == null) {
            return OptimizelyDecision.createErrorDecision(key, this, getFlagKeyInvalidMessage(key));
        }

        List<OptimizelyDecideOption> allOptions = getAllOptions(options);
        DecisionReasons decisionReasons = new DecisionReasons();
        Boolean sentEvent = false;
        Boolean flagEnabled = false;

        Map<String, ?> copiedAttributes = copyAttributes();
        FeatureDecision flagDecision = optimizely.decisionService.getVariationForFeature(
            flag,
            userId,
            copiedAttributes,
            projectConfig,
            allOptions,
            decisionReasons);

        FeatureDecision.DecisionSource decisionSource = FeatureDecision.DecisionSource.ROLLOUT;
        SourceInfo sourceInfo = new RolloutSourceInfo();

        if (flagDecision.variation != null) {
            if (flagDecision.decisionSource.equals(FeatureDecision.DecisionSource.FEATURE_TEST)) {
                if (!allOptions.contains(OptimizelyDecideOption.DISABLE_DECISION_EVENT)) {
                    optimizely.sendImpression(
                        projectConfig,
                        flagDecision.experiment,
                        userId,
                        copiedAttributes,
                        flagDecision.variation);
                    sentEvent = true;
                }

                decisionSource = flagDecision.decisionSource;
                sourceInfo = new FeatureTestSourceInfo(flagDecision.experiment.getKey(), flagDecision.variation.getKey());
            } else {
                String message = String.format("The user \"%s\" is not included in an experiment for flag \"%s\".", userId, key);
                logger.info(message);
                decisionReasons.addInfo(message);
            }
            if (flagDecision.variation.getFeatureEnabled()) {
                flagEnabled = true;
            }
        }

        Map<String, Object> variableMap = new HashMap<>();
        if (!allOptions.contains(OptimizelyDecideOption.EXCLUDE_VARIABLES)) {
            variableMap = getDecisionVariableMap(
                flag,
                flagDecision.variation,
                flagEnabled,
                decisionReasons);
        }

        OptimizelyJSON optimizelyJSON = new OptimizelyJSON(variableMap);
        if (optimizelyJSON == null) {
            decisionReasons.addError(OPTIMIZELY_JSON_ERROR);
        }

        List<String> reasonsToReport = decisionReasons.toReport(allOptions);
        String variationKey = flagDecision.variation != null ? flagDecision.variation.getKey() : null;
        // TODO: add ruleKey values when available later.
        String ruleKey = null;

        DecisionNotification decisionNotification = DecisionNotification.newFlagDecisionNotificationBuilder()
            .withUserId(userId)
            .withAttributes(copiedAttributes)
            .withFlagKey(key)
            .withEnabled(flagEnabled)
            .withVariables(variableMap)
            .withVariationKey(variationKey)
            .withRuleKey(ruleKey)
            .withReasons(reasonsToReport)
            .withDecisionEventDispatched(sentEvent)
            .build();
        optimizely.notificationCenter.send(decisionNotification);

        logger.info("Feature \"{}\" is enabled for user \"{}\"? {}", key, userId, flagEnabled);

        return new OptimizelyDecision(
            variationKey,
            flagEnabled,
            optimizelyJSON,
            ruleKey,
            key,
            this,
            reasonsToReport);
    }

    /**
     * Returns a decision result ({@link OptimizelyDecision}) for a given flag key and a user context, which contains all data required to deliver the flag.
     *
     * @param key A flag key for which a decision will be made.
     * @return A decision result.
     */
    public OptimizelyDecision decide(String key) {
        return decide(key, new OptimizelyDecideOption[0]);
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for multiple flag keys and a user context.
     * <ul>
     * <li>If the SDK finds an error for a key, the response will include a decision for the key showing <b>reasons</b> for the error.
     * <li>The SDK will always return key-mapped decisions. When it can not process requests, it’ll return an empty map after logging the errors.
     * <ul>
     * @param keys An array of flag keys for which decisions will be made.
     * @param options An array of options for decision-making.
     * @return All decision results mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideAll(@Nonnull String[] keys,
                                                     @Nonnull OptimizelyDecideOption[] options) {
        Map<String, OptimizelyDecision> decisionMap = new HashMap<>();

        ProjectConfig projectConfig = optimizely.getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing isFeatureEnabled call.");
            return decisionMap;
        }

        if (keys.length == 0) return decisionMap;

        List<OptimizelyDecideOption> allOptions = getAllOptions(options);

        for (String key : keys) {
            OptimizelyDecision decision = decide(key, options);
            if (!allOptions.contains(OptimizelyDecideOption.ENABLED_FLAGS_ONLY) || decision.getEnabled()) {
                decisionMap.put(key, decision);
            }
        }

        return decisionMap;
    }

    /**
     * Returns a key-map of decision results for multiple flag keys and a user context.
     *
     * @param keys An array of flag keys for which decisions will be made.
     * @return All decision results mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideAll(@Nonnull String[] keys) {
        return decideAll(keys, new OptimizelyDecideOption[0]);
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for all active flag keys.
     *
     * @param options An array of options for decision-making.
     * @return All decision results mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideAll(@Nonnull OptimizelyDecideOption[] options) {
        Map<String, OptimizelyDecision> decisionMap = new HashMap<>();

        ProjectConfig projectConfig = optimizely.getProjectConfig();
        if (projectConfig == null) {
            logger.error("Optimizely instance is not valid, failing isFeatureEnabled call.");
            return decisionMap;
        }

        List<FeatureFlag> allFlags = projectConfig.getFeatureFlags();
        String[] allFlagKeys = new String[allFlags.size()];
        for (int i = 0; i < allFlags.size(); i++) allFlagKeys[i] = allFlags.get(i).getKey();

        return decideAll(allFlagKeys, options);
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for all active flag keys.
     *
     * @return A dictionary of all decision results, mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideAll() {
        return decideAll(new OptimizelyDecideOption[0]);
    }

    /**
     * Track an event.
     *
     * @param eventName The event name.
     * @param eventTags A map of event tag names to event tag values.
     * @throws UnknownEventTypeException
     */
    public void trackEvent(@Nonnull String eventName,
                           @Nonnull Map<String, ?> eventTags) throws UnknownEventTypeException {
        optimizely.track(eventName, userId, attributes, eventTags);
    }

    /**
     * Track an event.
     *
     * @param eventName The event name.
     * @throws UnknownEventTypeException
     */
    public void trackEvent(@Nonnull String eventName) throws UnknownEventTypeException {
        trackEvent(eventName, Collections.emptyMap());
    }

    // Utils

    private Map<String, Object> copyAttributes() {
        return new HashMap<>(attributes);
    }

    private List<OptimizelyDecideOption> getAllOptions(OptimizelyDecideOption[] options) {
        List<OptimizelyDecideOption> allOptions = new ArrayList(Arrays.asList(optimizely.defaultDecideOptions));
        allOptions.addAll(Arrays.asList(options));
        return allOptions;
    }

    public static String getFlagKeyInvalidMessage(String flagKey) {
        return String.format(FLAG_KEY_INVALID, flagKey);
    }

    public static String getVariableValueInvalidMessage(String variableKey) {
        return String.format(VARIABLE_VALUE_INVALID, variableKey);
    }

    private Map<String, Object> getDecisionVariableMap(FeatureFlag flag,
                                                       Variation variation,
                                                       Boolean featureEnabled,
                                                       DecisionReasons decisionReasons) {
        Map<String, Object> valuesMap = new HashMap<String, Object>();
        for (FeatureVariable variable : flag.getVariables()) {
            String value = variable.getDefaultValue();
            if (featureEnabled) {
                FeatureVariableUsageInstance instance = variation.getVariableIdToFeatureVariableUsageInstanceMap().get(variable.getId());
                if (instance != null) {
                    value = instance.getValue();
                }
            }

            Object convertedValue = optimizely.convertStringToType(value, variable.getType());
            if (convertedValue == null) {
                decisionReasons.addError(getVariableValueInvalidMessage(variable.getKey()));
            } else if (convertedValue instanceof OptimizelyJSON) {
                convertedValue = ((OptimizelyJSON) convertedValue).toMap();
            }

            valuesMap.put(variable.getKey(), convertedValue);
        }

        return valuesMap;
    }

}

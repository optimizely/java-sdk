/**
 *
 *    Copyright 2020-2021, Optimizely and contributors
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
package com.optimizely.ab;

import com.optimizely.ab.config.Variation;
import com.optimizely.ab.optimizelydecision.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class OptimizelyUserContext {

    // Constant used for null values when making keys for Forced Decisions
    static String OPTI_NULL_STRING = "NULL";

    // OptimizelyForcedDecisionsKey mapped to variationKeys
    Map<String, Map<String, String>> forcedDecisionsMap = new HashMap<>();

    @Nonnull
    private final String userId;

    @Nonnull
    private final Map<String, Object> attributes;

    @Nonnull
    private final Optimizely optimizely;

    private static final Logger logger = LoggerFactory.getLogger(OptimizelyUserContext.class);

    public OptimizelyUserContext(@Nonnull Optimizely optimizely,
                                 @Nonnull String userId,
                                 @Nonnull Map<String, ?> attributes) {
        this.optimizely = optimizely;
        this.userId = userId;
        if (attributes != null) {
            this.attributes = Collections.synchronizedMap(new HashMap<>(attributes));
        } else {
            this.attributes = Collections.synchronizedMap(new HashMap<>());
        }
    }

    public OptimizelyUserContext(@Nonnull Optimizely optimizely, @Nonnull String userId) {
        this(optimizely, userId, Collections.EMPTY_MAP);
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

    public OptimizelyUserContext copy() {
        return new OptimizelyUserContext(optimizely, userId, attributes);
    }

    /**
     * Set an attribute for a given key.
     *
     * @param key An attribute key
     * @param value An attribute value
     */
    public void setAttribute(@Nonnull String key, @Nullable Object value) {
        attributes.put(key, value);
    }

    /**
     * Returns a decision result ({@link OptimizelyDecision}) for a given flag key and a user context, which contains all data required to deliver the flag.
     * <ul>
     * <li>If the SDK finds an error, it’ll return a decision with <b>null</b> for <b>variationKey</b>. The decision will include an error message in <b>reasons</b>.
     * </ul>
     * @param key A flag key for which a decision will be made.
     * @param options A list of options for decision-making.
     * @return A decision result.
     */
    public OptimizelyDecision decide(@Nonnull String key,
                                     @Nonnull List<OptimizelyDecideOption> options) {
        return optimizely.decide(copy(), key, options);
    }

    /**
     * Returns a decision result ({@link OptimizelyDecision}) for a given flag key and a user context, which contains all data required to deliver the flag.
     *
     * @param key A flag key for which a decision will be made.
     * @return A decision result.
     */
    public OptimizelyDecision decide(@Nonnull String key) {
        return decide(key, Collections.emptyList());
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for multiple flag keys and a user context.
     * <ul>
     * <li>If the SDK finds an error for a key, the response will include a decision for the key showing <b>reasons</b> for the error.
     * <li>The SDK will always return key-mapped decisions. When it can not process requests, it’ll return an empty map after logging the errors.
     * </ul>
     * @param keys A list of flag keys for which decisions will be made.
     * @param options A list of options for decision-making.
     * @return All decision results mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideForKeys(@Nonnull List<String> keys,
                                                         @Nonnull List<OptimizelyDecideOption> options) {
        return optimizely.decideForKeys(copy(), keys, options);
    }

    /**
     * Returns a key-map of decision results for multiple flag keys and a user context.
     *
     * @param keys A list of flag keys for which decisions will be made.
     * @return All decision results mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideForKeys(@Nonnull List<String> keys) {
        return decideForKeys(keys, Collections.emptyList());
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for all active flag keys.
     *
     * @param options A list of options for decision-making.
     * @return All decision results mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideAll(@Nonnull List<OptimizelyDecideOption> options) {
        return optimizely.decideAll(copy(), options);
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for all active flag keys.
     *
     * @return A dictionary of all decision results, mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideAll() {
        return decideAll(Collections.emptyList());
    }

    /**
     * Track an event.
     *
     * @param eventName The event name.
     * @param eventTags A map of event tag names to event tag values.
     * @throws UnknownEventTypeException when event type is unknown
     */
    public void trackEvent(@Nonnull String eventName,
                           @Nonnull Map<String, ?> eventTags) throws UnknownEventTypeException {
        optimizely.track(eventName, userId, attributes, eventTags);
    }

    /**
     * Track an event.
     *
     * @param eventName The event name.
     * @throws UnknownEventTypeException when event type is unknown
     */
    public void trackEvent(@Nonnull String eventName) throws UnknownEventTypeException {
        trackEvent(eventName, Collections.emptyMap());
    }

    /**
     * Set a forced decision
     *
     * @param flagKey The flag key for the forced decision
     * @param variationKey The variation key for the forced decision
     * @return Returns a boolean, True if successfully set, otherwise false
     */
    public Boolean setForcedDecision(@Nonnull String flagKey, @Nonnull String variationKey) {
        return setForcedDecision(flagKey, null, variationKey);
    }

    /**
     * Set a forced decision
     *
     * @param flagKey The flag key for the forced decision
     * @param ruleKey The rule key for the forced decision
     * @param variationKey The variation key for the forced decision
     * @return Returns a boolean, Ture if successfully set, otherwise false
     */
    public Boolean setForcedDecision(@Nonnull String flagKey, String ruleKey, @Nonnull String variationKey) {
        if (optimizely.getOptimizelyConfig() == null) {
            logger.error("Optimizely SDK not ready.");
            return false;
        }

        ruleKey = ruleKey != null ? ruleKey : OPTI_NULL_STRING;

        if (forcedDecisionsMap.get(flagKey) != null) {
            Map<String, String> existingForcedDecisions = forcedDecisionsMap.get(flagKey);
            existingForcedDecisions.put(ruleKey, variationKey);
        } else {
            Map<String, String> forcedDecision = new HashMap<>();
            forcedDecision.put(ruleKey, variationKey);
            forcedDecisionsMap.put(flagKey, forcedDecision);
        }


        return true;
    }

    /**
     * Get a forced decision
     *
     * @param flagKey The flag key for the forced decision
     * @return Returns a variationKey for a given forced decision
     */
    public String getForcedDecision(@Nonnull String flagKey) {
        return getForcedDecision(flagKey, null);
    }

    /**
     * Get a forced decision
     *
     * @param flagKey The flag key for the forced decision
     * @param ruleKey The rule key for the forced decision
     * @return Returns a variationKey for a given forced decision
     */
    @Nullable
    public String getForcedDecision(@Nonnull String flagKey, String ruleKey) {
        if (optimizely.getOptimizelyConfig() == null) {
            logger.error("Optimizely SDK not ready.");
            return null;
        }
        return findForcedDecision(flagKey, ruleKey);
    }

    /**
     * Finds a forced decision
     *
     * @param flagKey The flag key for the forced decision
     * @param ruleKey The rule key for the forced decision
     * @return Returns a variationKey relating to the found forced decision, otherwise null
     */
    @Nullable
    public String findForcedDecision(@Nonnull String flagKey, String ruleKey) {
        String variationKey = null;
        if (forcedDecisionsMap.get(flagKey) != null ) {
            variationKey = forcedDecisionsMap.get(flagKey).get(ruleKey != null ? ruleKey : OPTI_NULL_STRING);
        }
        return variationKey;
    }

    /**
     * Remove a forced decision
     *
     * @param flagKey The flag key in the forced decision
     * @return Returns a boolean of true if successful, otherwise false
     */
    public boolean removeForcedDecision(@Nonnull String flagKey) {
        return removeForcedDecision(flagKey, null);
    }

    /**
     * Remove a forced decision
     *
     * @param flagKey The flag key for the forced decision
     * @param ruleKey The rule key for the forced decision
     * @return Returns a boolean, true if successfully removed, otherwise false
     */
    public boolean removeForcedDecision(@Nonnull String flagKey, String ruleKey) {
        if (optimizely.getOptimizelyConfig() == null) {
            logger.error("Optimizely SDK not ready.");
            return false;
        }

        try {
            if (forcedDecisionsMap.get(flagKey) != null) {
                ruleKey = ruleKey != null ? ruleKey : OPTI_NULL_STRING;
                if (forcedDecisionsMap.get(flagKey).remove(ruleKey) != null) {
                    forcedDecisionsMap.remove(flagKey);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Unable to remove forced-decision - " + e);
        }

        return false;
    }

    /**
     * Remove all forced decisions
     *
     * @return Returns a boolean, True if successfully, otherwise false
     */
    public boolean removeAllForcedDecisions() {
        if (optimizely.getProjectConfig() == null) {
            logger.error("Optimizely SDK not ready.");
            return false;
        }
        // Clear both maps for with and without ruleKey
        forcedDecisionsMap.clear();
        return true;
    }

    /**
     * Find a validated forced decision
     *
     * @param flagKey The flag key for the forced decision
     * @return Returns a DecisionResponse structure of type Variation, otherwise null with reasons
     */
    public DecisionResponse<Variation> findValidatedForcedDecision(@Nonnull String flagKey) {
        return findValidatedForcedDecision(flagKey, null);
    }

    /**
     * Find a validated forced decision
     *
     * @param flagKey The flag key for a forced decision
     * @param ruleKey The rule key for a forced decision
     * @return Returns a DecisionResponse structure of type Variation, otherwise null result with reasons
     */
    public DecisionResponse<Variation> findValidatedForcedDecision(@Nonnull String flagKey, String ruleKey) {
        DecisionReasons reasons = DefaultDecisionReasons.newInstance();
        String variationKey = findForcedDecision(flagKey, ruleKey);
        if (variationKey != null) {
            Variation variation = optimizely.getFlagVariationByKey(flagKey, variationKey);
            String strRuleKey = ruleKey != null ? ruleKey : "null";
            String info;
            if (variation != null) {
                info = String.format("Variation %s is mapped to flag: %s and rule: %s and user: %s in the forced decisions map.", variationKey, flagKey, strRuleKey, userId);
                logger.debug(info);
                reasons.addInfo(info);
                return new DecisionResponse(variation, reasons);
            } else {
                info = String.format("Invalid variation is mapped to flag: %s and rule: %s and user: %s in the forced decisions map.", flagKey, strRuleKey, userId);
                logger.debug(info);
                reasons.addInfo(info);
            }
        }
        return new DecisionResponse<>(null, reasons);
    }

    // Utils

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        OptimizelyUserContext userContext = (OptimizelyUserContext) obj;
        return userId.equals(userContext.getUserId()) &&
            attributes.equals(userContext.getAttributes()) &&
            optimizely.equals(userContext.getOptimizely());
    }

    @Override
    public int hashCode() {
        int hash = userId.hashCode();
        hash = 31 * hash + attributes.hashCode();
        hash = 31 * hash + optimizely.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "OptimizelyUserContext {" +
            "userId='" + userId + '\'' +
            ", attributes='" + attributes + '\'' +
            '}';
    }

}

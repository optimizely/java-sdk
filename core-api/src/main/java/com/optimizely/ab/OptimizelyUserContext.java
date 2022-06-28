/**
 *
 *    Copyright 2020-2022, Optimizely and contributors
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
import java.util.concurrent.ConcurrentHashMap;

public class OptimizelyUserContext {
    // OptimizelyForcedDecisionsKey mapped to variationKeys
    Map<String, OptimizelyForcedDecision> forcedDecisionsMap;

    @Nonnull
    private final String userId;

    @Nonnull
    private final Map<String, Object> attributes;

    private List<String> qualifiedSegments;

    @Nonnull
    private final Optimizely optimizely;

    private static final Logger logger = LoggerFactory.getLogger(OptimizelyUserContext.class);

    public OptimizelyUserContext(@Nonnull Optimizely optimizely,
                                 @Nonnull String userId,
                                 @Nonnull Map<String, ?> attributes) {
        this(optimizely, userId, attributes, Collections.EMPTY_MAP);
    }

    public OptimizelyUserContext(@Nonnull Optimizely optimizely,
                                 @Nonnull String userId,
                                 @Nonnull Map<String, ?> attributes,
                                 @Nullable Map<String, OptimizelyForcedDecision> forcedDecisionsMap) {
        this.optimizely = optimizely;
        this.userId = userId;
        if (attributes != null) {
            this.attributes = Collections.synchronizedMap(new HashMap<>(attributes));
        } else {
            this.attributes = Collections.synchronizedMap(new HashMap<>());
        }
        if (forcedDecisionsMap != null) {
          this.forcedDecisionsMap = new ConcurrentHashMap<>(forcedDecisionsMap);
        }
        this.qualifiedSegments = Collections.synchronizedList(new LinkedList<>());
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
        return new OptimizelyUserContext(optimizely, userId, attributes, forcedDecisionsMap);
    }

    /**
     * Returns true if the user is qualified for the given segment name
     * @param segment A String segment key which will be check in qualified segments list that if it exist then user is qualified.
     * @return boolean Is user qualified for a segment.
     */
    public boolean isQualifiedFor(@Nonnull String segment) {
        return qualifiedSegments.contains(segment);
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
     * @param optimizelyDecisionContext The OptimizelyDecisionContext containing flagKey and ruleKey
     * @param optimizelyForcedDecision The OptimizelyForcedDecision containing the variationKey
     * @return Returns a boolean, Ture if successfully set, otherwise false
     */
    public Boolean setForcedDecision(@Nonnull OptimizelyDecisionContext optimizelyDecisionContext,
                                     @Nonnull OptimizelyForcedDecision optimizelyForcedDecision) {
        // Check if the forcedDecisionsMap has been initialized yet or not
        if (forcedDecisionsMap == null ){
            // Thread-safe implementation of HashMap
            forcedDecisionsMap = new ConcurrentHashMap<>();
        }
        forcedDecisionsMap.put(optimizelyDecisionContext.getKey(), optimizelyForcedDecision);
        return true;
    }

    /**
     * Get a forced decision
     *
     * @param optimizelyDecisionContext The OptimizelyDecisionContext containing flagKey and ruleKey
     * @return Returns a variationKey for a given forced decision
     */
    @Nullable
    public OptimizelyForcedDecision getForcedDecision(@Nonnull OptimizelyDecisionContext optimizelyDecisionContext) {
        return findForcedDecision(optimizelyDecisionContext);
    }

    /**
     * Finds a forced decision
     *
     * @param optimizelyDecisionContext The OptimizelyDecisionContext containing flagKey and ruleKey
     * @return Returns a variationKey relating to the found forced decision, otherwise null
     */
    @Nullable
    public OptimizelyForcedDecision findForcedDecision(@Nonnull OptimizelyDecisionContext optimizelyDecisionContext) {
        if (forcedDecisionsMap != null) {
            return forcedDecisionsMap.get(optimizelyDecisionContext.getKey());
        }
        return null;
    }

    /**
     * Remove a forced decision
     *
     * @param optimizelyDecisionContext The OptimizelyDecisionContext containing flagKey and ruleKey
     * @return Returns a boolean, true if successfully removed, otherwise false
     */
    public boolean removeForcedDecision(@Nonnull OptimizelyDecisionContext optimizelyDecisionContext) {
        try {
            if (forcedDecisionsMap != null) {
                if (forcedDecisionsMap.remove(optimizelyDecisionContext.getKey()) != null) {
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
        // Clear both maps for with and without ruleKey
        if (forcedDecisionsMap != null) {
            forcedDecisionsMap.clear();
        }
        return true;
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

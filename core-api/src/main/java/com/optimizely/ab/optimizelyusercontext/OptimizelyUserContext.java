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

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OptimizelyUserContext {
    private final String userId;
    private final Map<String, Object> attributes;
    private final Optimizely optimizely;

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

    public void setAttribute(@Nonnull String key, @Nonnull Object value) {
        attributes.put(key, value);
    }

    public OptimizelyDecision decide(@Nonnull String key,
                                     @Nonnull OptimizelyDecideOption[] options) {
        return OptimizelyDecision.createErrorDecision(key, this, "N/A");
    }

    public OptimizelyDecision decide(String key) {
        return decide(key, new OptimizelyDecideOption[0]);
    }

    public Map<String, OptimizelyDecision> decideAll(@Nonnull String[] keys,
                                                     @Nonnull OptimizelyDecideOption[] options) {
        return new HashMap<>();
    }

    public Map<String, OptimizelyDecision> decideAll(@Nonnull String[] keys) {
        return decideAll(keys, new OptimizelyDecideOption[0]);
    }

    public Map<String, OptimizelyDecision> decideAll(@Nonnull OptimizelyDecideOption[] options) {
        String[] allFlagKeys = {};
        return decideAll(allFlagKeys, options);
    }

    public Map<String, OptimizelyDecision> decideAll() {
        return decideAll(new OptimizelyDecideOption[0]);
    }

    public void trackEvent(@Nonnull String eventName,
                           @Nonnull Map<String, ?> eventTags) throws UnknownEventTypeException {
        optimizely.track(eventName, userId, attributes, eventTags);
    }

    public void trackEvent(@Nonnull String eventName) throws UnknownEventTypeException {
        trackEvent(eventName, Collections.emptyMap());
    }

}

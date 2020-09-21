package com.optimizely.ab;

import com.optimizely.ab.optimizelyjson.OptimizelyJSON;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UserContext {
    private final String userId;
    private final Map<String, Object> userAttributes ;
    private final Optimizely optimizely;

    public UserContext(String userId, Optimizely optimizely) {
        this(userId, optimizely, new HashMap<>());
    }

    public UserContext(String userId, Optimizely optimizely, Map<String, Object> userAttributes) {
        this.userId = userId;
        this.optimizely = optimizely;
        this.userAttributes = userAttributes;
    }

    public void setAttribute(String name, Object value) {
        this.userAttributes.put(name, value);
    }

    public OptimizelyJSON decide(String decideKey) {
       return optimizely.getAllFeatureVariables(decideKey, userId, userAttributes);
    }

    public void track(String eventName) {
        track(eventName, Collections.emptyMap());
    }

    public void track(String eventName, Map<String, String> eventTags) {
        optimizely.track(eventName, userId, userAttributes, eventTags);
    }
}

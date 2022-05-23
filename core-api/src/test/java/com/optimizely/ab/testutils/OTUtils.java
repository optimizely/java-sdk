package com.optimizely.ab.testutils;

import com.optimizely.ab.*;

import java.util.Map;

public class OTUtils {
    public static OptimizelyUserContext user(String userId, Map<String,?> attributes) {
        Optimizely optimizely = new Optimizely.Builder().build();
        return new OptimizelyUserContext(optimizely, userId, attributes);
    }

    public static OptimizelyUserContext user(Map<String,?> attributes) {
        return user("any-user", attributes);
    }
}
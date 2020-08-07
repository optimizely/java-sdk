package com.optimizely.ab.config.audience.match;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MatchRegistry {

    private static final Map<String, Match> registry = new ConcurrentHashMap<>();
    private static final String EXISTS = "exists";
    private static final String EXACT = "exact";
    private static final String GREATER_THAN = "gt";
    private static final String LESS_THAN = "lt";
    private static final String SUBSTRING = "substring";
    private static final Match LEGACY = new DefaultMatchForLegacyAttributes();

    static {
        registry.put(EXISTS, new ExistsMatch());
        registry.put(EXACT, new ExactMatch());
        registry.put(GREATER_THAN, new GTMatch());
        registry.put(LESS_THAN, new LTMatch());
        registry.put(SUBSTRING, new SubstringMatch());
    }

    // TODO rename Match to Matcher
    public static Match getMatch(String name) throws UnknownMatchTypeException {
        if (name == null) {
            return LEGACY;
        }

        Match match = registry.get(name);
        if (match == null) {
            throw new UnknownMatchTypeException();
        }

        return match;
    }

    public static void register(String name, Match match) {
        registry.put(name, match);
    }

}

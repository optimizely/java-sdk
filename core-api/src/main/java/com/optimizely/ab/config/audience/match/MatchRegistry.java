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
package com.optimizely.ab.config.audience.match;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MatchRegistry maps a string match "type" to a match implementation.
 * All supported Match implementations must be registed with this registry.
 * Third-party {@link Match} implementations may also be registered to provide
 * additional functionality.
 */
public class MatchRegistry {

    private static final Map<String, Match> registry = new ConcurrentHashMap<>();
    public static final String EXISTS = "exists";
    public static final String EXACT = "exact";
    public static final String GREATER_THAN = "gt";
    public static final String LESS_THAN = "lt";
    public static final String SUBSTRING = "substring";
    public static final String LEGACY = "legacy";

    static {
        register(EXISTS, new ExistsMatch());
        register(EXACT, new ExactMatch());
        register(GREATER_THAN, new GTMatch());
        register(LESS_THAN, new LTMatch());
        register(SUBSTRING, new SubstringMatch());
        register(LEGACY, new DefaultMatchForLegacyAttributes());
    }

    // TODO rename Match to Matcher
    public static Match getMatch(String name) throws UnknownMatchTypeException {
        Match match = registry.get(name == null ? LEGACY : name);
        if (match == null) {
            throw new UnknownMatchTypeException();
        }

        return match;
    }

    /**
     * register registers a Match implementation with it's name.
     * NOTE: This does not check for existence so default implementations can
     * be overridden.
     * @param name
     * @param match
     */
    public static void register(String name, Match match) {
        registry.put(name, match);
    }

}

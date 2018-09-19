/**
 *
 *    Copyright 2018, Optimizely and contributors
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class MatchType {

    public static final Logger logger = LoggerFactory.getLogger(MatchType.class);

    private String type;
    private Match matcher;

    public static MatchType getMatchType(String type, Object value) {
        if (type == null) type = "legacy_custom_attribute";

        switch (type) {
            case "exists":
                return new MatchType(type, new ExistsMatch(value));
            case "exact":
                if (value instanceof String) {
                    return new MatchType(type, new ExactMatch<String>((String) value));
                }
                else if (value instanceof Number) {
                    return new MatchType(type, new ExactMatch<Number>((Number) value));
                }
                else if (value instanceof Boolean) {
                    return new MatchType(type, new ExactMatch<Boolean>((Boolean) value));
                }
                break;
            case "substring":
                if (value instanceof String) {
                    return new MatchType(type, new SubstringMatch((String) value));
                }
                break;
            case "gt":
                if (value instanceof Number) {
                    return new MatchType(type, new GTMatch((Number) value));
                }
                break;
            case "lt":
                if (value instanceof Number) {
                    return new MatchType(type, new LTMatch((Number) value));
                }
                break;
            case "legacy_custom_attribute":
                if (value instanceof String) {
                    return new MatchType(type, new DefaultMatchForLegacyAttributes<String>((String) value));
                }
                break;
            default:
                return new MatchType(type, new NullMatch());
        }

        return new MatchType(type, new NullMatch());
    }


    private MatchType(String type, Match matcher) {
        this.type = type;
        this.matcher = matcher;
    }

    public @Nonnull
    Match getMatcher() {
        return matcher;
    }

    @Override
    public String toString() {
        return type;
    }
}
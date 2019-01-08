/**
 *
 *    Copyright 2018-2019, Optimizely and contributors
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

    private String matchType;
    private Match matcher;

    public static MatchType getMatchType(String matchType, Object conditionValue) {
        if (matchType == null) matchType = "legacy_custom_attribute";

        switch (matchType) {
            case "exists":
                return new MatchType(matchType, new ExistsMatch(conditionValue));
            case "exact":
                if (conditionValue instanceof String) {
                    return new MatchType(matchType, new ExactMatch<String>((String) conditionValue));
                } else if (isValidNumber(conditionValue)) {
                    return new MatchType(matchType, new ExactNumberMatch((Number) conditionValue));
                } else if (conditionValue instanceof Boolean) {
                    return new MatchType(matchType, new ExactMatch<Boolean>((Boolean) conditionValue));
                }
                break;
            case "substring":
                if (conditionValue instanceof String) {
                    return new MatchType(matchType, new SubstringMatch((String) conditionValue));
                }
                break;
            case "gt":
                if (isValidNumber(conditionValue)) {
                    return new MatchType(matchType, new GTMatch((Number) conditionValue));
                }
                break;
            case "lt":
                if (isValidNumber(conditionValue)) {
                    return new MatchType(matchType, new LTMatch((Number) conditionValue));
                }
                break;
            case "legacy_custom_attribute":
                if (conditionValue instanceof String) {
                    return new MatchType(matchType, new DefaultMatchForLegacyAttributes<String>((String) conditionValue));
                }
                break;
            default:
                return new MatchType(matchType, new NullMatch(MatchTypeError.UNKNOWN_MATCH_TYPE));
        }

        return new MatchType(matchType, new NullMatch(MatchTypeError.INAPPLICABLE_CONDITION_VALUE));
    }

    private static boolean isValidNumber(Object conditionValue) {
        if (conditionValue instanceof Integer) {
            return Math.abs((Integer) conditionValue) <= 1e53;
        } else if (conditionValue instanceof Double) {
            Double value = ((Number) conditionValue).doubleValue();
            return !(value.isNaN() || value.isInfinite());
        }
        return false;
    }

    private MatchType(String type, Match matcher) {
        this.matchType = type;
        this.matcher = matcher;
    }

    @Nonnull
    public Match getMatcher() {
        return matcher;
    }

    @Override
    public String toString() {
        return matchType;
    }
}

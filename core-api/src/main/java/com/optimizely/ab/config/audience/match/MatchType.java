/**
 *
 *    Copyright 2018-2020, Optimizely and contributors
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

import static com.optimizely.ab.internal.AttributesUtil.isValidNumber;

public class MatchType {

    public static final Logger logger = LoggerFactory.getLogger(MatchType.class);

    private String matchType;
    private Match matcher;

    public static MatchType getMatchType(String matchType, Object conditionValue) throws UnexpectedValueTypeException, UnknownMatchTypeException {
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
            case "ge":
                if (isValidNumber(conditionValue)) {
                    return new MatchType(matchType, new GEMatch((Number) conditionValue));
                }
                break;
            case "gt":
                if (isValidNumber(conditionValue)) {
                    return new MatchType(matchType, new GTMatch((Number) conditionValue));
                }
                break;
            case "le":
                if (isValidNumber(conditionValue)) {
                    return new MatchType(matchType, new LEMatch((Number) conditionValue));
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
            case "semver_eq":
                if (conditionValue instanceof String) {
                    return new MatchType(matchType, new SemanticVersionEqualsMatch((String) conditionValue));
                }
                break;
            case "semver_ge":
                if (conditionValue instanceof String) {
                    return new MatchType(matchType, new SemanticVersionGEMatch((String) conditionValue));
                }
                break;
            case "semver_gt":
                if (conditionValue instanceof String) {
                    return new MatchType(matchType, new SemanticVersionGTMatch((String) conditionValue));
                }
                break;
            case "semver_le":
                if (conditionValue instanceof String) {
                    return new MatchType(matchType, new SemanticVersionLEMatch((String) conditionValue));
                }
                break;
            case "semver_lt":
                if (conditionValue instanceof String) {
                    return new MatchType(matchType, new SemanticVersionLTMatch((String) conditionValue));
                }
                break;
            default:
                throw new UnknownMatchTypeException();
        }

        throw new UnexpectedValueTypeException();
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

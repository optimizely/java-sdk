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

import com.optimizely.ab.optimizelyjson.OptimizelyJSON;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class OptimizelyDecision {
    @Nullable
    private final String variationKey;

    private final boolean enabled;

    @Nullable
    private final OptimizelyJSON variables;

    @Nullable
    private final String ruleKey;

    @Nonnull
    private final String flagKey;

    @Nullable
    private final OptimizelyUserContext userContext;

    @Nonnull
    private List<String> reasons;


    public OptimizelyDecision(@Nullable String variationKey,
                              boolean enabled,
                              @Nullable OptimizelyJSON variables,
                              @Nullable String ruleKey,
                              @Nonnull String flagKey,
                              @Nullable OptimizelyUserContext userContext,
                              @Nonnull List<String> reasons) {
        this.variationKey = variationKey;
        this.enabled = enabled;
        this.variables = variables;
        this.ruleKey = ruleKey;
        this.flagKey = flagKey;
        this.userContext = userContext;
        this.reasons = reasons;
    }

    @Nullable
    public String getVariationKey() {
        return variationKey;
    }

    public boolean getEnabled() {
        return enabled;
    }

    @Nonnull
    public OptimizelyJSON getVariables() {
        return variables;
    }

    @Nullable
    public String getRuleKey() {
        return ruleKey;
    }

    @Nonnull
    public String getFlagKey() {
        return flagKey;
    }

    @Nullable
    public OptimizelyUserContext getUserContext() {
        return userContext;
    }

    @Nonnull
    public List<String> getReasons() {
        return reasons;
    }

    public static OptimizelyDecision createErrorDecision(@Nonnull String key,
                                                         @Nonnull OptimizelyUserContext user,
                                                         @Nonnull String error) {
        return new OptimizelyDecision(
            null,
            false,
            null,
            null,
            key,
            user,
            Arrays.asList(error));
    }

    public boolean hasFailed() {
        return variationKey == null;
    }

}

/**
 *
 *    Copyright 2021, Optimizely and contributors
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OptimizelyDecisionContext {
    public static final String OPTI_NULL_RULE_KEY = "$opt-null-rule-key";
    public static final String OPTI_KEY_DIVIDER = "-$opt$-";

    private String flagKey;
    private String ruleKey;

    public OptimizelyDecisionContext(@Nonnull String flagKey, @Nullable String ruleKey) {
        this.flagKey = flagKey;
        this.ruleKey = ruleKey;
    }

    public String getFlagKey() {
        return flagKey;
    }

    public String getRuleKey() {
        return ruleKey != null ? ruleKey : OPTI_NULL_RULE_KEY;
    }

    public String getKey() {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(flagKey);
        keyBuilder.append(OPTI_KEY_DIVIDER);
        keyBuilder.append(getRuleKey());
        return keyBuilder.toString();
    }
}

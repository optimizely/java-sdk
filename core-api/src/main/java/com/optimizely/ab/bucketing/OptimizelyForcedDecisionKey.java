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
package com.optimizely.ab.bucketing;

import javax.annotation.Nonnull;

public class OptimizelyForcedDecisionKey {
    private String flagKey;
    private String ruleKey;

    public OptimizelyForcedDecisionKey(@Nonnull String flagKey, String ruleKey) {
        this.flagKey = flagKey;
        this.ruleKey = ruleKey;
    }

    public String getFlagKey() { return flagKey; }

    public String getRuleKey() { return ruleKey; }

    public String toString() {
        StringBuilder keyString = new StringBuilder();
        keyString.append(getFlagKey().hashCode());
        if (getRuleKey() != null) {
            keyString.append(getRuleKey().hashCode());
        }
        return keyString.toString();
    }
}

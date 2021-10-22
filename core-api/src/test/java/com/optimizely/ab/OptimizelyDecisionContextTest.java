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

import org.junit.Test;
import static junit.framework.TestCase.assertEquals;

public class OptimizelyDecisionContextTest {

    @Test
    public void initializeOptimizelyDecisionContextWithFlagKeyAndRuleKey() {
        String flagKey = "test-flag-key";
        String ruleKey = "1029384756";
        String expectedKey = flagKey + ruleKey;
        OptimizelyDecisionContext optimizelyDecisionContext = new OptimizelyDecisionContext(flagKey, ruleKey);
        assertEquals(flagKey, optimizelyDecisionContext.getFlagKey());
        assertEquals(ruleKey, optimizelyDecisionContext.getRuleKey());
        assertEquals(expectedKey, optimizelyDecisionContext.getKey());
    }

    @Test
    public void initializeOptimizelyDecisionContextWithFlagKey() {
        String flagKey = "test-flag-key";
        String expectedKey = flagKey + OptimizelyDecisionContext.OPTI_NULL_RULE_KEY;
        OptimizelyDecisionContext optimizelyDecisionContext = new OptimizelyDecisionContext(flagKey, null);
        assertEquals(flagKey, optimizelyDecisionContext.getFlagKey());
        assertEquals(OptimizelyDecisionContext.OPTI_NULL_RULE_KEY, optimizelyDecisionContext.getRuleKey());
        assertEquals(expectedKey, optimizelyDecisionContext.getKey());
    }
}

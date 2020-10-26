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
package com.optimizely.ab.optimizelydecision;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.optimizelyjson.OptimizelyJSON;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

public class OptimizelyDecisionTest {

    @Test
    public void testOptimizelyDecision() {
        String variationKey = "var1";
        boolean enabled = true;
        OptimizelyJSON variables = new OptimizelyJSON("{\"k1\":\"v1\"}");
        String ruleKey = null;
        String flagKey = "flag1";
        OptimizelyUserContext userContext = new OptimizelyUserContext(Optimizely.builder().build(), "tester");
        List<String> reasons = new ArrayList<>();

        OptimizelyDecision decision = new OptimizelyDecision(
            variationKey,
            enabled,
            variables,
            ruleKey,
            flagKey,
            userContext,
            reasons
        );

        assertEquals(decision.getVariationKey(), variationKey);
        assertEquals(decision.getEnabled(), enabled);
        assertEquals(decision.getVariables(), variables);
        assertEquals(decision.getRuleKey(), ruleKey);
        assertEquals(decision.getFlagKey(), flagKey);
        assertEquals(decision.getUserContext(), userContext);
        assertEquals(decision.getReasons(), reasons);
    }

    @Test
    public void testCreateErrorDecision() {
        String flagKey = "flag1";
        OptimizelyUserContext userContext = new OptimizelyUserContext(Optimizely.builder().build(), "tester");
        String error = "SDK has an error";

        OptimizelyDecision decision = OptimizelyDecision.createErrorDecision(flagKey, userContext, error);

        assertEquals(decision.getVariationKey(), null);
        assertEquals(decision.getEnabled(), false);
        assertTrue(decision.getVariables().isEmpty());
        assertEquals(decision.getRuleKey(), null);
        assertEquals(decision.getFlagKey(), flagKey);
        assertEquals(decision.getUserContext(), userContext);
        assertEquals(decision.getReasons().size(), 1);
        assertEquals(decision.getReasons().get(0), error);
    }

}

package com.optimizely.ab.optimizelyusercontext;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.optimizelyjson.OptimizelyJSON;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class OptimizelyDecisionTest {

    @Test
    public void testOptimizelyDecision() {
        String variationKey = "var1";
        boolean enabled = true;
        OptimizelyJSON variables = new OptimizelyJSON("{\"k1\":\"v1\"}");
        String ruleKey = null;
        String flagKey = "flag1";
        OptimizelyUserContext userContext = new OptimizelyUserContext(Optimizely.builder().build(), "tester");
        String[] reasons = new String[0];

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
        assertEquals(decision.getVariables(), null);
        assertEquals(decision.getRuleKey(), null);
        assertEquals(decision.getFlagKey(), flagKey);
        assertEquals(decision.getUserContext(), userContext);
        assertEquals(decision.getReasons().length, 1);
        assertEquals(decision.getReasons()[0], error);
    }

}

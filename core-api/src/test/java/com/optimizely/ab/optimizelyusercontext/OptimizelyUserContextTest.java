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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.optimizely.ab.Optimizely;
import com.optimizely.ab.optimizelyjson.OptimizelyJSON;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static com.optimizely.ab.config.ValidProjectConfigV4.ATTRIBUTE_HOUSE_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_GRYFFINDOR_VALUE;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class OptimizelyUserContextTest {

    public Optimizely optimizely;
    public String userId = "tester";

    @Before
    public void setUp() throws Exception {
        String datafile = Resources.toString(Resources.getResource("config/decide-project-config.json"), Charsets.UTF_8);

        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .build();
    }

    @Test
    public void optimizelyUserContext_withAttributes() {
        Map<String, Object> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, userId, attributes);

        assertEquals(user.getOptimizely(), optimizely);
        assertEquals(user.getUserId(), userId);
        assertEquals(user.getAttributes(), attributes);
    }

    @Test
    public void optimizelyUserContext_noAttributes() {
        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, userId);

        assertEquals(user.getOptimizely(), optimizely);
        assertEquals(user.getUserId(), userId);
        assertTrue(user.getAttributes().isEmpty());
    }

    @Test
    public void setAttribute() {
        Map<String, Object> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, userId, attributes);

        user.setAttribute("k1", "v1");
        user.setAttribute("k2", true);
        user.setAttribute("k3", 100);
        user.setAttribute("k4", 3.5);

        assertEquals(user.getOptimizely(), optimizely);
        assertEquals(user.getUserId(), userId);
        Map<String, Object> newAttributes = user.getAttributes();
        assertEquals(newAttributes.get(ATTRIBUTE_HOUSE_KEY), AUDIENCE_GRYFFINDOR_VALUE);
        assertEquals(newAttributes.get("k1"), "v1");
        assertEquals(newAttributes.get("k2"), true);
        assertEquals(newAttributes.get("k3"), 100);
        assertEquals(newAttributes.get("k4"), 3.5);
    }

    @Test
    public void setAttribute_override() {
        Map<String, Object> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, userId, attributes);

        user.setAttribute("k1", "v1");
        user.setAttribute(ATTRIBUTE_HOUSE_KEY, "v2");

        assertEquals(user.getOptimizely(), optimizely);
        assertEquals(user.getUserId(), userId);
        Map<String, Object> newAttributes = user.getAttributes();
        assertEquals(newAttributes.get("k1"), "v1");
        assertEquals(newAttributes.get(ATTRIBUTE_HOUSE_KEY), "v2");
    }

    // decide

    @Test
    public void decide() {
        String flagKey = "feature_2";
        OptimizelyJSON variablesExpected = optimizely.getAllFeatureVariables(flagKey, userId);

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey);

        assertEquals(decision.getVariationKey(), "variation_with_traffic");
        assertTrue(decision.getEnabled());
        assertEquals(decision.getVariables().toMap(), variablesExpected.toMap());
        assertEquals(decision.getFlagKey(), flagKey);
        assertEquals(decision.getUserContext(), user);
        assertTrue(decision.getReasons().isEmpty());
    }

    // decideAll

    @Test
    public void decideAll_oneFeature() {
        String flagKey = "feature_2";
        String[] flagKeys = {flagKey};
        OptimizelyJSON variablesExpected = optimizely.getAllFeatureVariables(flagKey, userId);

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        Map<String, OptimizelyDecision> decisions = user.decideAll(flagKeys);

        assertTrue(decisions.size() == 1);
        OptimizelyDecision decision = decisions.get(flagKey);

        OptimizelyDecision expDecision = new OptimizelyDecision(
            "variation_with_traffic",
            true,
            variablesExpected,
            null,
            flagKey,
            user,
            Collections.emptyList());
        assertEquals(decision, expDecision);
    }

    @Test
    public void decideAll_twoFeatures() {
        String flagKey1 = "feature_1";
        String flagKey2 = "feature_2";

        String[] flagKeys = {flagKey1, flagKey2};
        OptimizelyJSON variablesExpected1 = optimizely.getAllFeatureVariables(flagKey1, userId);
        OptimizelyJSON variablesExpected2 = optimizely.getAllFeatureVariables(flagKey2, userId);

        OptimizelyUserContext user = optimizely.createUserContext(userId, Collections.singletonMap("gender", "f"));
        Map<String, OptimizelyDecision> decisions = user.decideAll(flagKeys);

        assertTrue(decisions.size() == 2);

        assertEquals(
            decisions.get(flagKey1),
            new OptimizelyDecision("a",
                true,
                variablesExpected1,
                null,
                flagKey1,
                user,
                Collections.emptyList()));
        assertEquals(
            decisions.get(flagKey2),
            new OptimizelyDecision("variation_with_traffic",
                true,
                variablesExpected2,
                null,
                flagKey2,
                user,
                Collections.emptyList()));
    }

    @Test
    public void decideAll_allFeatures() {
        String flagKey1 = "feature_1";
        String flagKey2 = "feature_2";
        String flagKey3 = "feature_3";

        OptimizelyJSON variablesExpected1 = optimizely.getAllFeatureVariables(flagKey1, userId);
        OptimizelyJSON variablesExpected2 = optimizely.getAllFeatureVariables(flagKey2, userId);
        OptimizelyJSON variablesExpected3 = new OptimizelyJSON(Collections.emptyMap());

        OptimizelyUserContext user = optimizely.createUserContext(userId, Collections.singletonMap("gender", "f"));
        Map<String, OptimizelyDecision> decisions = user.decideAll();

        assertTrue(decisions.size() == 3);

        assertEquals(
            decisions.get(flagKey1),
            new OptimizelyDecision(
                "a",
                true,
                variablesExpected1,
                null,
                flagKey1,
                user,
                Collections.emptyList()));
        assertEquals(
            decisions.get(flagKey2),
            new OptimizelyDecision(
                "variation_with_traffic",
                true,
                variablesExpected2,
                null,
                flagKey2,
                user,
                Collections.emptyList()));
        assertEquals(
            decisions.get(flagKey3),
            new OptimizelyDecision(
                null,
                false,
                variablesExpected3,
                null,
                flagKey3,
                user,
                Collections.emptyList()));
    }

    @Test
    public void decideAll_allFeatures_enabledOnly() {
        String flagKey1 = "feature_1";
        OptimizelyJSON variablesExpected1 = optimizely.getAllFeatureVariables(flagKey1, userId);

        OptimizelyUserContext user = optimizely.createUserContext(userId, Collections.singletonMap("gender", "f"));
        OptimizelyDecideOption[] decideOptions = {OptimizelyDecideOption.ENABLED_FLAGS_ONLY};
        Map<String, OptimizelyDecision> decisions = user.decideAll(decideOptions);

        assertTrue(decisions.size() == 2);

        assertEquals(
            decisions.get(flagKey1),
            new OptimizelyDecision(
                "a",
                true,
                variablesExpected1,
                null,
                flagKey1,
                user,
                Collections.emptyList()));
    }

    // send events

    @Test
    public void decide_sendEvent() {

    }

    @Test
    public void decide_doNotSendEvent() {

    }

    // options

    @Test
    public void decideOptions_disbleTracking() {
    }

    @Test
    public void decideOptions_useUPSbyDefault() {
    }

    @Test
    public void decideOptions_bypassUPS_doNotUpdateUPS() {
    }

    @Test
    public void decideOptions_bypassUPS_doNotReadUPS() {
    }

    @Test
    public void decideOptions_excludeVariables() {
    }

    @Test
    public void decideOptions_defaultDecideOption() {
    }

    // errors

    @Test
    public void decide_sdkNotReady() {
        String flagKey = "feature_1";

        Optimizely optimizely = new Optimizely.Builder().build();

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey);

        assertNull(decision.getVariationKey());
        assertFalse(decision.getEnabled());
        assertNull(decision.getVariables());
        assertEquals(decision.getFlagKey(), flagKey);
        assertEquals(decision.getUserContext(), user);

        assertEquals(decision.getReasons().size(), 1);
        assertEquals(decision.getReasons().get(1), OptimizelyUserContext.SDK_NOT_READY);
    }

    @Test
    public void decide_invalidFeatureKey() {

    }

    @Test
    public void decideAll_sdkNotReady() {

    }

    @Test
    public void decideAll_errorDecisionIncluded() {

    }

    // reasons (errors)

    @Test
    public void decideReasons_sdkNotReady() {

    }

    @Test
    public void decideReasons_featureKeyInvalid() {

    }

    @Test
    public void decideReasons_variableValueInvalid() {

    }

    // reasons (logs with includeReasons)

    @Test
    public void decideReasons_conditionNoMatchingAudience() {}
    @Test
    public void decideReasons_conditionInvalidFormat() {}
    @Test
    public void decideReasons_evaluateAttributeInvalidCondition() {}
    @Test
    public void decideReasons_evaluateAttributeInvalidType() {}
    @Test
    public void decideReasons_evaluateAttributeValueOutOfRange() {}
    @Test
    public void decideReasons_userAttributeInvalidType() {}
    @Test
    public void decideReasons_userAttributeInvalidMatch() {}
    @Test
    public void decideReasons_userAttributeNilValue() {}
    @Test
    public void decideReasons_userAttributeInvalidName() {}
    @Test
    public void decideReasons_missingAttributeValue() {}

    @Test
    public void decideReasons_experimentNotRunning() {}
    @Test
    public void decideReasons_gotVariationFromUserProfile() {}
    @Test
    public void decideReasons_forcedVariationFound() {}
    @Test
    public void decideReasons_forcedVariationFoundButInvalid() {}
    @Test
    public void decideReasons_userMeetsConditionsForTargetingRule() {}
    @Test
    public void decideReasons_userDoesntMeetConditionsForTargetingRule() {}
    @Test
    public void decideReasons_userBucketedIntoTargetingRule() {}
    @Test
    public void decideReasons_userBucketedIntoEveryoneTargetingRule() {}
    @Test
    public void decideReasons_userNotBucketedIntoTargetingRule() {}
    @Test
    public void decideReasons_userBucketedIntoVariationInExperiment() {}
    @Test
    public void decideReasons_userNotBucketedIntoVariation() {}
    @Test
    public void decideReasons_userBucketedIntoInvalidVariation() {}
    @Test
    public void decideReasons_userBucketedIntoExperimentInGroup() {}
    @Test
    public void decideReasons_userNotBucketedIntoExperimentInGroup() {}
    @Test
    public void decideReasons_userNotBucketedIntoAnyExperimentInGroup() {}
    @Test
    public void decideReasons_userBucketedIntoInvalidExperiment() {}
    @Test
    public void decideReasons_userNotInExperiment() {}
}

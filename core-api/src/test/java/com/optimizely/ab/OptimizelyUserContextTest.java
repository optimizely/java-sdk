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
package com.optimizely.ab;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.optimizely.ab.bucketing.FeatureDecision;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.*;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.event.ForwardingEventProcessor;
import com.optimizely.ab.event.internal.payload.DecisionMetadata;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.optimizelydecision.DecisionMessage;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;
import com.optimizely.ab.optimizelydecision.OptimizelyDecision;
import com.optimizely.ab.optimizelyjson.OptimizelyJSON;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.*;

import static com.optimizely.ab.config.ValidProjectConfigV4.ATTRIBUTE_HOUSE_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_GRYFFINDOR_VALUE;
import static com.optimizely.ab.notification.DecisionNotification.ExperimentDecisionNotificationBuilder.VARIATION_KEY;
import static com.optimizely.ab.notification.DecisionNotification.FlagDecisionNotificationBuilder.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OptimizelyUserContextTest {
    @Rule
    public EventHandlerRule eventHandler = new EventHandlerRule();

    String userId = "tester";
    boolean isListenerCalled = false;

    Optimizely optimizely;
    String datafile;
    ProjectConfig config;
    Map<String, Experiment> experimentIdMapping;
    Map<String, FeatureFlag> featureKeyMapping;
    Map<String, Group> groupIdMapping;

    @Before
    public void setUp() throws Exception {
        datafile = Resources.toString(Resources.getResource("config/decide-project-config.json"), Charsets.UTF_8);

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
    public void setAttribute_noAttribute() {
        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, userId);

        user.setAttribute("k1", "v1");
        user.setAttribute("k2", true);

        assertEquals(user.getOptimizely(), optimizely);
        assertEquals(user.getUserId(), userId);
        Map<String, Object> newAttributes = user.getAttributes();
        assertEquals(newAttributes.get("k1"), "v1");
        assertEquals(newAttributes.get("k2"), true);
    }

    @Test
    public void setAttribute_override() {
        Map<String, Object> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, userId, attributes);

        user.setAttribute("k1", "v1");
        user.setAttribute(ATTRIBUTE_HOUSE_KEY, "v2");

        Map<String, Object> newAttributes = user.getAttributes();
        assertEquals(newAttributes.get("k1"), "v1");
        assertEquals(newAttributes.get(ATTRIBUTE_HOUSE_KEY), "v2");
    }

    @Test
    public void setAttribute_nullValue() {
        Map<String, Object> attributes = Collections.singletonMap("k1", null);
        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, userId, attributes);

        Map<String, Object> newAttributes = user.getAttributes();
        assertEquals(newAttributes.get("k1"), null);

        user.setAttribute("k1", true);
        newAttributes = user.getAttributes();
        assertEquals(newAttributes.get("k1"), true);

        user.setAttribute("k1", null);
        newAttributes = user.getAttributes();
        assertEquals(newAttributes.get("k1"), null);
    }

    // decide

    @Test
    public void decide_featureTest() {
        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        String flagKey = "feature_2";
        String experimentKey = "exp_no_audience";
        String variationKey = "variation_with_traffic";
        String experimentId = "10420810910";
        String variationId = "10418551353";
        OptimizelyJSON variablesExpected = optimizely.getAllFeatureVariables(flagKey, userId);

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey);

        assertEquals(decision.getVariationKey(), variationKey);
        assertTrue(decision.getEnabled());
        assertEquals(decision.getVariables().toMap(), variablesExpected.toMap());
        assertEquals(decision.getRuleKey(), experimentKey);
        assertEquals(decision.getFlagKey(), flagKey);
        assertEquals(decision.getUserContext(), user);
        assertTrue(decision.getReasons().isEmpty());

        DecisionMetadata metadata = new DecisionMetadata.Builder()
            .setFlagKey(flagKey)
            .setRuleKey(experimentKey)
            .setRuleType(FeatureDecision.DecisionSource.FEATURE_TEST.toString())
            .setVariationKey(variationKey)
            .setEnabled(true)
            .build();
        eventHandler.expectImpression(experimentId, variationId, userId, Collections.emptyMap(), metadata);
    }

    @Test
    public void decide_rollout() {
        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        String flagKey = "feature_1";
        String experimentKey = "18322080788";
        String variationKey = "18257766532";
        String experimentId = "18322080788";
        String variationId = "18257766532";
        OptimizelyJSON variablesExpected = optimizely.getAllFeatureVariables(flagKey, userId);

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey);

        assertEquals(decision.getVariationKey(), variationKey);
        assertTrue(decision.getEnabled());
        assertEquals(decision.getVariables().toMap(), variablesExpected.toMap());
        assertEquals(decision.getRuleKey(), experimentKey);
        assertEquals(decision.getFlagKey(), flagKey);
        assertEquals(decision.getUserContext(), user);
        assertTrue(decision.getReasons().isEmpty());

        DecisionMetadata metadata = new DecisionMetadata.Builder()
            .setFlagKey(flagKey)
            .setRuleKey(experimentKey)
            .setRuleType(FeatureDecision.DecisionSource.ROLLOUT.toString())
            .setVariationKey(variationKey)
            .setEnabled(true)
            .build();
        eventHandler.expectImpression(experimentId, variationId, userId, Collections.emptyMap(), metadata);
    }

    @Test
    public void decide_nullVariation() {
        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        String flagKey = "feature_3";
        OptimizelyJSON variablesExpected = new OptimizelyJSON(Collections.emptyMap());

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey);

        assertEquals(decision.getVariationKey(), null);
        assertFalse(decision.getEnabled());
        assertEquals(decision.getVariables().toMap(), variablesExpected.toMap());
        assertEquals(decision.getRuleKey(), null);
        assertEquals(decision.getFlagKey(), flagKey);
        assertEquals(decision.getUserContext(), user);
        assertTrue(decision.getReasons().isEmpty());

        DecisionMetadata metadata = new DecisionMetadata.Builder()
            .setFlagKey(flagKey)
            .setRuleKey("")
            .setRuleType(FeatureDecision.DecisionSource.ROLLOUT.toString())
            .setVariationKey("")
            .setEnabled(false)
            .build();
        eventHandler.expectImpression(null, "", userId, Collections.emptyMap(), metadata);
    }

    // decideAll

    @Test
    public void decideAll_oneFlag() {
        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        String flagKey = "feature_2";
        String experimentKey = "exp_no_audience";
        String variationKey = "variation_with_traffic";
        String experimentId = "10420810910";
        String variationId = "10418551353";

        List<String> flagKeys = Arrays.asList(flagKey);
        OptimizelyJSON variablesExpected = optimizely.getAllFeatureVariables(flagKey, userId);

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        Map<String, OptimizelyDecision> decisions = user.decideForKeys(flagKeys);

        assertTrue(decisions.size() == 1);
        OptimizelyDecision decision = decisions.get(flagKey);

        OptimizelyDecision expDecision = new OptimizelyDecision(
            variationKey,
            true,
            variablesExpected,
            experimentKey,
            flagKey,
            user,
            Collections.emptyList());
        assertEquals(decision, expDecision);

        DecisionMetadata metadata = new DecisionMetadata.Builder()
            .setFlagKey(flagKey)
            .setRuleKey(experimentKey)
            .setRuleType(FeatureDecision.DecisionSource.FEATURE_TEST.toString())
            .setVariationKey(variationKey)
            .setEnabled(true)
            .build();
        eventHandler.expectImpression(experimentId, variationId, userId, Collections.emptyMap(), metadata);
    }

    @Test
    public void decideAll_twoFlags() {
        String flagKey1 = "feature_1";
        String flagKey2 = "feature_2";

        List<String> flagKeys = Arrays.asList(flagKey1, flagKey2);
        OptimizelyJSON variablesExpected1 = optimizely.getAllFeatureVariables(flagKey1, userId);
        OptimizelyJSON variablesExpected2 = optimizely.getAllFeatureVariables(flagKey2, userId);

        OptimizelyUserContext user = optimizely.createUserContext(userId, Collections.singletonMap("gender", "f"));
        Map<String, OptimizelyDecision> decisions = user.decideForKeys(flagKeys);

        assertTrue(decisions.size() == 2);

        assertEquals(
            decisions.get(flagKey1),
            new OptimizelyDecision("a",
                true,
                variablesExpected1,
                "exp_with_audience",
                flagKey1,
                user,
                Collections.emptyList()));
        assertEquals(
            decisions.get(flagKey2),
            new OptimizelyDecision("variation_with_traffic",
                true,
                variablesExpected2,
                "exp_no_audience",
                flagKey2,
                user,
                Collections.emptyList()));
    }

    @Test
    public void decideAll_allFlags() {
        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        String flagKey1 = "feature_1";
        String flagKey2 = "feature_2";
        String flagKey3 = "feature_3";
        Map<String, Object> attributes = Collections.singletonMap("gender", "f");

        OptimizelyJSON variablesExpected1 = optimizely.getAllFeatureVariables(flagKey1, userId);
        OptimizelyJSON variablesExpected2 = optimizely.getAllFeatureVariables(flagKey2, userId);
        OptimizelyJSON variablesExpected3 = new OptimizelyJSON(Collections.emptyMap());

        OptimizelyUserContext user = optimizely.createUserContext(userId, attributes);
        Map<String, OptimizelyDecision> decisions = user.decideAll();

        assertTrue(decisions.size() == 3);

        assertEquals(
            decisions.get(flagKey1),
            new OptimizelyDecision(
                "a",
                true,
                variablesExpected1,
                "exp_with_audience",
                flagKey1,
                user,
                Collections.emptyList()));
        assertEquals(
            decisions.get(flagKey2),
            new OptimizelyDecision(
                "variation_with_traffic",
                true,
                variablesExpected2,
                "exp_no_audience",
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

        eventHandler.expectImpression("10390977673", "10389729780", userId, attributes);
        eventHandler.expectImpression("10420810910", "10418551353", userId, attributes);
        eventHandler.expectImpression(null, "", userId, attributes);
    }

    @Test
    public void decideAll_allFlags_enabledFlagsOnly() {
        String flagKey1 = "feature_1";
        OptimizelyJSON variablesExpected1 = optimizely.getAllFeatureVariables(flagKey1, userId);

        OptimizelyUserContext user = optimizely.createUserContext(userId, Collections.singletonMap("gender", "f"));
        Map<String, OptimizelyDecision> decisions = user.decideAll(Arrays.asList(OptimizelyDecideOption.ENABLED_FLAGS_ONLY));

        assertTrue(decisions.size() == 2);

        assertEquals(
            decisions.get(flagKey1),
            new OptimizelyDecision(
                "a",
                true,
                variablesExpected1,
                "exp_with_audience",
                flagKey1,
                user,
                Collections.emptyList()));
    }

    // trackEvent

    @Test
    public void trackEvent() {
        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        Map<String, Object> attributes = Collections.singletonMap("gender", "f");
        String eventKey = "event1";
        Map<String, Object> eventTags = Collections.singletonMap("name", "carrot");
        OptimizelyUserContext user = optimizely.createUserContext(userId, attributes);
        user.trackEvent(eventKey, eventTags);

        eventHandler.expectConversion(eventKey, userId, attributes, eventTags);
    }

    @Test
    public void trackEvent_noEventTags() {
        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        Map<String, Object> attributes = Collections.singletonMap("gender", "f");
        String eventKey = "event1";
        OptimizelyUserContext user = optimizely.createUserContext(userId, attributes);
        user.trackEvent(eventKey);

        eventHandler.expectConversion(eventKey, userId, attributes);
    }

    @Test
    public void trackEvent_emptyAttributes() {
        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        String eventKey = "event1";
        Map<String, ?> eventTags = Collections.singletonMap("name", "carrot");
        OptimizelyUserContext user = optimizely.createUserContext(userId);
        user.trackEvent(eventKey, eventTags);

        eventHandler.expectConversion(eventKey, userId, Collections.emptyMap(), eventTags);
    }

    // send events

    @Test
    public void decide_sendEvent() {
        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        String flagKey = "feature_2";
        String variationKey = "variation_with_traffic";
        String experimentId = "10420810910";
        String variationId = "10418551353";

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey);

        assertEquals(decision.getVariationKey(), variationKey);

        eventHandler.expectImpression(experimentId, variationId, userId, Collections.emptyMap());
    }

    @Test
    public void decide_doNotSendEvent_withOption() {
        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        String flagKey = "feature_2";

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.DISABLE_DECISION_EVENT));

        assertEquals(decision.getVariationKey(), "variation_with_traffic");
    }

    // notifications

    @Test
    public void decisionNotification() {
        String flagKey = "feature_2";
        String variationKey = "variation_with_traffic";
        boolean enabled = true;
        OptimizelyJSON variables = optimizely.getAllFeatureVariables(flagKey, userId);
        String ruleKey = "exp_no_audience";
        List<String> reasons = Collections.emptyList();

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(FLAG_KEY, flagKey);
        testDecisionInfoMap.put(VARIATION_KEY, variationKey);
        testDecisionInfoMap.put(ENABLED, enabled);
        testDecisionInfoMap.put(VARIABLES, variables.toMap());
        testDecisionInfoMap.put(RULE_KEY, ruleKey);
        testDecisionInfoMap.put(REASONS, reasons);

        Map<String, Object> attributes = Collections.singletonMap("gender", "f");
        OptimizelyUserContext user = optimizely.createUserContext(userId, attributes);

        optimizely.addDecisionNotificationHandler(
            decisionNotification -> {
                Assert.assertEquals(decisionNotification.getType(), NotificationCenter.DecisionNotificationType.FLAG.toString());
                Assert.assertEquals(decisionNotification.getUserId(), userId);
                Assert.assertEquals(decisionNotification.getAttributes(), attributes);
                Assert.assertEquals(decisionNotification.getDecisionInfo(), testDecisionInfoMap);
                isListenerCalled = true;
            });

        isListenerCalled = false;
        testDecisionInfoMap.put(DECISION_EVENT_DISPATCHED, true);
        user.decide(flagKey);
        assertTrue(isListenerCalled);

        isListenerCalled = false;
        testDecisionInfoMap.put(DECISION_EVENT_DISPATCHED, false);
        user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.DISABLE_DECISION_EVENT));
        assertTrue(isListenerCalled);
    }

    // options

    @Test
    public void decideOptions_bypassUPS() throws Exception {
        String flagKey = "feature_2";        // embedding experiment: "exp_no_audience"
        String experimentId = "10420810910";    // "exp_no_audience"
        String variationId1 = "10418551353";
        String variationId2 = "10418510624";
        String variationKey1 = "variation_with_traffic";
        String variationKey2 = "variation_no_traffic";

        UserProfileService ups = mock(UserProfileService.class);
        when(ups.lookup(userId)).thenReturn(createUserProfileMap(experimentId, variationId2));

        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withUserProfileService(ups)
            .build();

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey);
        // should return variationId2 set by UPS
        assertEquals(decision.getVariationKey(), variationKey2);

        decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.IGNORE_USER_PROFILE_SERVICE));
        // should ignore variationId2 set by UPS and return variationId1
        assertEquals(decision.getVariationKey(), variationKey1);
        // also should not save either
        verify(ups, never()).save(anyObject());
    }

    @Test
    public void decideOptions_excludeVariables() {
        String flagKey = "feature_1";
        OptimizelyUserContext user = optimizely.createUserContext(userId);

        OptimizelyDecision decision = user.decide(flagKey);
        assertTrue(decision.getVariables().toMap().size() > 0);

        decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.EXCLUDE_VARIABLES));
        assertTrue(decision.getVariables().toMap().size() == 0);
    }

    @Test
    public void decideOptions_includeReasons() {
        OptimizelyUserContext user = optimizely.createUserContext(userId);

        String flagKey = "invalid_key";
        OptimizelyDecision decision = user.decide(flagKey);
        assertEquals(decision.getReasons().size(), 1);
        TestCase.assertEquals(decision.getReasons().get(0), DecisionMessage.FLAG_KEY_INVALID.reason(flagKey));

        decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS));
        assertEquals(decision.getReasons().size(), 1);
        assertEquals(decision.getReasons().get(0), DecisionMessage.FLAG_KEY_INVALID.reason(flagKey));

        flagKey = "feature_1";
        decision = user.decide(flagKey);
        assertEquals(decision.getReasons().size(), 0);

        decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS));
        assertTrue(decision.getReasons().size() > 0);
    }

    public void decideOptions_disableDispatchEvent() {
        // tested already with decide_doNotSendEvent() above
    }

    public void decideOptions_enabledFlagsOnly() {
        // tested already with decideAll_allFlags_enabledFlagsOnly() above
    }

    @Test
    public void decideOptions_defaultDecideOptions() {
        List<OptimizelyDecideOption> options = Arrays.asList(
            OptimizelyDecideOption.EXCLUDE_VARIABLES
        );

        optimizely = Optimizely.builder()
            .withDatafile(datafile)
            .withDefaultDecideOptions(options)
            .build();

        String flagKey = "feature_1";
        OptimizelyUserContext user = optimizely.createUserContext(userId);

        // should be excluded by DefaultDecideOption
        OptimizelyDecision decision = user.decide(flagKey);
        assertTrue(decision.getVariables().toMap().size() == 0);

        decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS, OptimizelyDecideOption.EXCLUDE_VARIABLES));
        // other options should work as well
        assertTrue(decision.getReasons().size() > 0);
        // redundant setting ignored
        assertTrue(decision.getVariables().toMap().size() == 0);
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
        assertTrue(decision.getVariables().isEmpty());
        assertEquals(decision.getFlagKey(), flagKey);
        assertEquals(decision.getUserContext(), user);

        assertEquals(decision.getReasons().size(), 1);
        assertEquals(decision.getReasons().get(0), DecisionMessage.SDK_NOT_READY.reason());
    }

    @Test
    public void decide_invalidFeatureKey() {
        String flagKey = "invalid_key";

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey);

        assertNull(decision.getVariationKey());
        assertFalse(decision.getEnabled());
        assertTrue(decision.getVariables().isEmpty());
        assertEquals(decision.getReasons().size(), 1);
        assertEquals(decision.getReasons().get(0), DecisionMessage.FLAG_KEY_INVALID.reason(flagKey));
    }

    @Test
    public void decideAll_sdkNotReady() {
        List<String> flagKeys = Arrays.asList("feature_1");

        Optimizely optimizely = new Optimizely.Builder().build();
        OptimizelyUserContext user = optimizely.createUserContext(userId);
        Map<String, OptimizelyDecision> decisions = user.decideForKeys(flagKeys);

        assertEquals(decisions.size(), 0);
    }

    @Test
    public void decideAll_errorDecisionIncluded() {
        String flagKey1 = "feature_2";
        String flagKey2 = "invalid_key";

        List<String> flagKeys = Arrays.asList(flagKey1, flagKey2);
        OptimizelyJSON variablesExpected1 = optimizely.getAllFeatureVariables(flagKey1, userId);

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        Map<String, OptimizelyDecision> decisions = user.decideForKeys(flagKeys);

        assertEquals(decisions.size(), 2);

        assertEquals(
            decisions.get(flagKey1),
            new OptimizelyDecision(
                "variation_with_traffic",
                true,
                variablesExpected1,
                "exp_no_audience",
                flagKey1,
                user,
                Collections.emptyList()));
        assertEquals(
            decisions.get(flagKey2),
            OptimizelyDecision.newErrorDecision(
                flagKey2,
                user,
                DecisionMessage.FLAG_KEY_INVALID.reason(flagKey2)));
    }

    // reasons (errors)

    @Test
    public void decideReasons_sdkNotReady() {
        String flagKey = "feature_1";

        Optimizely optimizely = new Optimizely.Builder().build();
        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey);

        assertEquals(decision.getReasons().size(), 1);
        assertEquals(decision.getReasons().get(0), DecisionMessage.SDK_NOT_READY.reason());
    }

    @Test
    public void decideReasons_featureKeyInvalid() {
        String flagKey = "invalid_key";

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey);

        assertEquals(decision.getReasons().size(), 1);
        assertEquals(decision.getReasons().get(0), DecisionMessage.FLAG_KEY_INVALID.reason(flagKey));
    }

    @Test
    public void decideReasons_variableValueInvalid() {
        String flagKey = "feature_1";

        FeatureFlag flag = getSpyFeatureFlag(flagKey);
        List<FeatureVariable> variables = Arrays.asList(new FeatureVariable("any-id", "any-key", "invalid", null, "integer", null));
        when(flag.getVariables()).thenReturn(variables);
        addSpyFeatureFlag(flag);

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey);

        assertEquals(decision.getReasons().get(0), DecisionMessage.VARIABLE_VALUE_INVALID.reason("any-key"));
    }

    // reasons (logs with includeReasons)

    @Test
    public void decideReasons_conditionNoMatchingAudience() throws ConfigParseException {
        String flagKey = "feature_1";
        String audienceId = "invalid_id";

        Experiment experiment = getSpyExperiment(flagKey);
        when(experiment.getAudienceIds()).thenReturn(Arrays.asList(audienceId));
        addSpyExperiment(experiment);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey);

        assertTrue(decision.getReasons().contains(
            String.format("Audience %s could not be found.", audienceId)
        ));
    }

    @Test
    public void decideReasons_evaluateAttributeInvalidType() {
        String flagKey = "feature_1";
        String audienceId = "13389130056";

        Experiment experiment = getSpyExperiment(flagKey);
        when(experiment.getAudienceIds()).thenReturn(Arrays.asList(audienceId));
        addSpyExperiment(experiment);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey, Collections.singletonMap("country", 25));

        assertTrue(decision.getReasons().contains(
            String.format("Audience condition \"{name='country', type='custom_attribute', match='exact', value='US'}\" evaluated to UNKNOWN because a value of type \"java.lang.Integer\" was passed for user attribute \"country\"")
        ));
    }

    @Test
    public void decideReasons_evaluateAttributeValueOutOfRange() {
        String flagKey = "feature_1";
        String audienceId = "age_18";

        Experiment experiment = getSpyExperiment(flagKey);
        when(experiment.getAudienceIds()).thenReturn(Arrays.asList(audienceId));
        addSpyExperiment(experiment);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey, Collections.singletonMap("age", (float)Math.pow(2, 54)));

        assertTrue(decision.getReasons().contains(
            String.format("Audience condition \"{name='age', type='custom_attribute', match='gt', value=18.0}\" evaluated to UNKNOWN because a value of type \"java.lang.Float\" was passed for user attribute \"age\"")
        ));
    }

    @Test
    public void decideReasons_userAttributeInvalidType() {
        String flagKey = "feature_1";
        String audienceId = "invalid_type";

        Experiment experiment = getSpyExperiment(flagKey);
        when(experiment.getAudienceIds()).thenReturn(Arrays.asList(audienceId));
        addSpyExperiment(experiment);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey, Collections.singletonMap("age", 25));

        assertTrue(decision.getReasons().contains(
            String.format("Audience condition \"{name='age', type='invalid', match='gt', value=18.0}\" uses an unknown condition type. You may need to upgrade to a newer release of the Optimizely SDK.")
        ));
    }

    @Test
    public void decideReasons_userAttributeInvalidMatch() {
        String flagKey = "feature_1";
        String audienceId = "invalid_match";

        Experiment experiment = getSpyExperiment(flagKey);
        when(experiment.getAudienceIds()).thenReturn(Arrays.asList(audienceId));
        addSpyExperiment(experiment);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey, Collections.singletonMap("age", 25));

        assertTrue(decision.getReasons().contains(
            String.format("Audience condition \"{name='age', type='custom_attribute', match='invalid', value=18.0}\" uses an unknown match type. You may need to upgrade to a newer release of the Optimizely SDK.")
        ));
    }

    @Test
    public void decideReasons_userAttributeNilValue() {
        String flagKey = "feature_1";
        String audienceId = "nil_value";

        Experiment experiment = getSpyExperiment(flagKey);
        when(experiment.getAudienceIds()).thenReturn(Arrays.asList(audienceId));
        addSpyExperiment(experiment);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey, Collections.singletonMap("age", 25));

        assertTrue(decision.getReasons().contains(
            String.format("Audience condition \"{name='age', type='custom_attribute', match='gt', value=null}\" evaluated to UNKNOWN because a value of type \"java.lang.Integer\" was passed for user attribute \"age\"")
        ));
    }

    @Test
    public void decideReasons_missingAttributeValue() {
        String flagKey = "feature_1";
        String audienceId = "age_18";

        Experiment experiment = getSpyExperiment(flagKey);
        when(experiment.getAudienceIds()).thenReturn(Arrays.asList(audienceId));
        addSpyExperiment(experiment);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey);

        assertTrue(decision.getReasons().contains(
            String.format("Audience condition \"{name='age', type='custom_attribute', match='gt', value=18.0}\" evaluated to UNKNOWN because no value was passed for user attribute \"age\"")
        ));
    }

    @Test
    public void decideReasons_experimentNotRunning() {
        String flagKey = "feature_1";

        Experiment experiment = getSpyExperiment(flagKey);
        when(experiment.isActive()).thenReturn(false);
        addSpyExperiment(experiment);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey);

        assertTrue(decision.getReasons().contains(
            String.format("Experiment \"exp_with_audience\" is not running.")
        ));
    }

    @Test
    public void decideReasons_gotVariationFromUserProfile() throws Exception {
        String flagKey = "feature_2";        // embedding experiment: "exp_no_audience"
        String experimentId = "10420810910";    // "exp_no_audience"
        String experimentKey = "exp_no_audience";
        String variationId2 = "10418510624";
        String variationKey2 = "variation_no_traffic";

        UserProfileService ups = mock(UserProfileService.class);
        when(ups.lookup(userId)).thenReturn(createUserProfileMap(experimentId, variationId2));

        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withUserProfileService(ups)
            .build();

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS));

        assertTrue(decision.getReasons().contains(
            String.format("Returning previously activated variation \"%s\" of experiment \"%s\" for user \"%s\" from user profile.", variationKey2, experimentKey, userId)
        ));
    }

    @Test
    public void decideReasons_forcedVariationFound() {
        String flagKey = "feature_1";
        String variationKey = "b";

        Experiment experiment = getSpyExperiment(flagKey);
        when(experiment.getUserIdToVariationKeyMap()).thenReturn(Collections.singletonMap(userId, variationKey));
        addSpyExperiment(experiment);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey);

        assertTrue(decision.getReasons().contains(
            String.format("User \"%s\" is forced in variation \"%s\".", userId, variationKey)
        ));
    }

    @Test
    public void decideReasons_forcedVariationFoundButInvalid() {
        String flagKey = "feature_1";
        String variationKey = "invalid-key";

        Experiment experiment = getSpyExperiment(flagKey);
        when(experiment.getUserIdToVariationKeyMap()).thenReturn(Collections.singletonMap(userId, variationKey));
        addSpyExperiment(experiment);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey);

        assertTrue(decision.getReasons().contains(
            String.format("Variation \"%s\" is not in the datafile. Not activating user \"%s\".", variationKey, userId)
        ));
    }

    @Test
    public void decideReasons_userMeetsConditionsForTargetingRule() {
        String flagKey = "feature_1";

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        user.setAttribute("country", "US");
        OptimizelyDecision decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS));

        assertTrue(decision.getReasons().contains(
            String.format("The user \"%s\" was bucketed into a rollout for feature flag \"%s\".", userId, flagKey)
        ));
    }

    @Test
    public void decideReasons_userDoesntMeetConditionsForTargetingRule() {
        String flagKey = "feature_1";

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        user.setAttribute("country", "CA");
        OptimizelyDecision decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS));

        assertTrue(decision.getReasons().contains(
            String.format("User \"%s\" does not meet conditions for targeting rule \"%d\".", userId, 1)
        ));
    }

    @Test
    public void decideReasons_userBucketedIntoTargetingRule() {
        String flagKey = "feature_1";

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        user.setAttribute("country", "US");
        OptimizelyDecision decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS));

        assertTrue(decision.getReasons().contains(
            String.format("The user \"%s\" was bucketed into a rollout for feature flag \"%s\".", userId, flagKey)
        ));
    }

    @Test
    public void decideReasons_userBucketedIntoEveryoneTargetingRule() {
        String flagKey = "feature_1";

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        user.setAttribute("country", "KO");
        OptimizelyDecision decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS));

        assertTrue(decision.getReasons().contains(
            String.format("User \"%s\" meets conditions for targeting rule \"Everyone Else\".", userId)
        ));
    }

    @Test
    public void decideReasons_userNotBucketedIntoTargetingRule() {
        String flagKey = "feature_1";
        String experimentKey = "3332020494";   // experimentKey of rollout[2]

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        user.setAttribute("browser", "safari");
        OptimizelyDecision decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS));

        assertTrue(decision.getReasons().contains(
            String.format("User with bucketingId \"%s\" is not in any variation of experiment \"%s\".", userId, experimentKey)
        ));
    }

    @Test
    public void decideReasons_userBucketedIntoVariationInExperiment() {
        String flagKey = "feature_2";
        String experimentKey = "exp_no_audience";
        String variationKey = "variation_with_traffic";

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS));

        assertTrue(decision.getReasons().contains(
            String.format("User with bucketingId \"%s\" is in variation \"%s\" of experiment \"%s\".", userId, variationKey, experimentKey)
        ));
    }

    @Test
    public void decideReasons_userNotBucketedIntoVariation() {
        String flagKey = "feature_2";

        Experiment experiment = getSpyExperiment(flagKey);
        when(experiment.getTrafficAllocation()).thenReturn(Arrays.asList(new TrafficAllocation("any-id", 0)));
        addSpyExperiment(experiment);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey, Collections.singletonMap("age", 25));

        assertTrue(decision.getReasons().contains(
            String.format("User with bucketingId \"%s\" is not in any variation of experiment \"exp_no_audience\".", userId)
        ));
    }

    @Test
    public void decideReasons_userBucketedIntoExperimentInGroup() {
        String flagKey = "feature_3";
        String experimentId = "10390965532";   // "group_exp_1"

        FeatureFlag flag = getSpyFeatureFlag(flagKey);
        when(flag.getExperimentIds()).thenReturn(Arrays.asList(experimentId));
        addSpyFeatureFlag(flag);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey);

        assertTrue(decision.getReasons().contains(
            String.format("User with bucketingId \"tester\" is in experiment \"group_exp_1\" of group 13142870430.")
        ));
    }

    @Test
    public void decideReasons_userNotBucketedIntoExperimentInGroup() {
        String flagKey = "feature_3";
        String experimentId = "10420843432";   // "group_exp_2"

        FeatureFlag flag = getSpyFeatureFlag(flagKey);
        when(flag.getExperimentIds()).thenReturn(Arrays.asList(experimentId));
        addSpyFeatureFlag(flag);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey);

        assertTrue(decision.getReasons().contains(
            String.format("User with bucketingId \"tester\" is not in experiment \"group_exp_2\" of group 13142870430.")
        ));
    }

    @Test
    public void decideReasons_userNotBucketedIntoAnyExperimentInGroup() {
        String flagKey = "feature_3";
        String experimentId = "10390965532";   // "group_exp_1"
        String groupId = "13142870430";

        FeatureFlag flag = getSpyFeatureFlag(flagKey);
        when(flag.getExperimentIds()).thenReturn(Arrays.asList(experimentId));
        addSpyFeatureFlag(flag);

        Group group = getSpyGroup(groupId);
        when(group.getTrafficAllocation()).thenReturn(Collections.emptyList());
        addSpyGroup(group);
        OptimizelyDecision decision = callDecideWithIncludeReasons(flagKey);

        assertTrue(decision.getReasons().contains(
            String.format("User with bucketingId \"tester\" is not in any experiment of group 13142870430.")
        ));
    }

    @Test
    public void decideReasons_userNotInExperiment() {
        String flagKey = "feature_1";
        String experimentKey = "exp_with_audience";

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS));

        assertTrue(decision.getReasons().contains(
            String.format("User \"%s\" does not meet conditions to be in experiment \"%s\".", userId, experimentKey)
        ));
    }

    // utils

    Map<String, Object> createUserProfileMap(String experimentId, String variationId) {
        Map<String, Object> userProfileMap = new HashMap<String, Object>();
        userProfileMap.put(UserProfileService.userIdKey, userId);

        Map<String, String> decisionMap = new HashMap<String, String>(1);
        decisionMap.put(UserProfileService.variationIdKey, variationId);

        Map<String, Map<String, String>> decisionsMap = new HashMap<String, Map<String, String>>();
        decisionsMap.put(experimentId, decisionMap);
        userProfileMap.put(UserProfileService.experimentBucketMapKey, decisionsMap);

        return userProfileMap;
    }

    void setAudienceForFeatureTest(String flagKey, String audienceId) throws ConfigParseException {
        Experiment experiment = getSpyExperiment(flagKey);
        when(experiment.getAudienceIds()).thenReturn(Arrays.asList(audienceId));
        addSpyExperiment(experiment);
    }

    Experiment getSpyExperiment(String flagKey) {
        setMockConfig();
        String experimentId = config.getFeatureKeyMapping().get(flagKey).getExperimentIds().get(0);
        return spy(experimentIdMapping.get(experimentId));
    }

    FeatureFlag getSpyFeatureFlag(String flagKey) {
        setMockConfig();
        return spy(config.getFeatureKeyMapping().get(flagKey));
    }

    Group getSpyGroup(String groupId) {
        setMockConfig();
        return spy(groupIdMapping.get(groupId));
    }

    void addSpyExperiment(Experiment experiment) {
        experimentIdMapping.put(experiment.getId(), experiment);
        when(config.getExperimentIdMapping()).thenReturn(experimentIdMapping);
    }

    void addSpyFeatureFlag(FeatureFlag flag) {
        featureKeyMapping.put(flag.getKey(), flag);
        when(config.getFeatureKeyMapping()).thenReturn(featureKeyMapping);
    }

    void addSpyGroup(Group group) {
        groupIdMapping.put(group.getId(), group);
        when(config.getGroupIdMapping()).thenReturn(groupIdMapping);
    }

    void setMockConfig() {
        if (config != null) return;

        ProjectConfig configReal = null;
        try {
            configReal = new DatafileProjectConfig.Builder().withDatafile(datafile).build();
            config = spy(configReal);
            optimizely = Optimizely.builder().withConfig(config).build();
            experimentIdMapping = new HashMap<>(config.getExperimentIdMapping());
            groupIdMapping = new HashMap<>(config.getGroupIdMapping());
            featureKeyMapping = new HashMap<>(config.getFeatureKeyMapping());
        } catch (ConfigParseException e) {
            fail("ProjectConfig build failed");
        }
    }

    OptimizelyDecision callDecideWithIncludeReasons(String flagKey, Map<String, Object> attributes) {
        OptimizelyUserContext user = optimizely.createUserContext(userId, attributes);
        return user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS));
    }

    OptimizelyDecision callDecideWithIncludeReasons(String flagKey) {
        return callDecideWithIncludeReasons(flagKey, Collections.emptyMap());
    }

}

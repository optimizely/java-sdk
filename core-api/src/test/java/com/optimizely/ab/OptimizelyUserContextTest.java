/**
 *
 *    Copyright 2021-2024, Optimizely and contributors
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.optimizely.ab.bucketing.FeatureDecision;
import com.optimizely.ab.bucketing.UserProfile;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.bucketing.UserProfileUtils;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.Group;
import com.optimizely.ab.config.ProjectConfig;
import static com.optimizely.ab.config.ValidProjectConfigV4.ATTRIBUTE_HOUSE_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_GRYFFINDOR_VALUE;
import com.optimizely.ab.event.EventProcessor;
import com.optimizely.ab.event.ForwardingEventProcessor;
import com.optimizely.ab.event.internal.ImpressionEvent;
import com.optimizely.ab.event.internal.payload.DecisionMetadata;
import com.optimizely.ab.internal.LogbackVerifier;
import static com.optimizely.ab.notification.DecisionNotification.ExperimentDecisionNotificationBuilder.VARIATION_KEY;
import static com.optimizely.ab.notification.DecisionNotification.FlagDecisionNotificationBuilder.DECISION_EVENT_DISPATCHED;
import static com.optimizely.ab.notification.DecisionNotification.FlagDecisionNotificationBuilder.ENABLED;
import static com.optimizely.ab.notification.DecisionNotification.FlagDecisionNotificationBuilder.EXPERIMENT_ID;
import static com.optimizely.ab.notification.DecisionNotification.FlagDecisionNotificationBuilder.FLAG_KEY;
import static com.optimizely.ab.notification.DecisionNotification.FlagDecisionNotificationBuilder.REASONS;
import static com.optimizely.ab.notification.DecisionNotification.FlagDecisionNotificationBuilder.RULE_KEY;
import static com.optimizely.ab.notification.DecisionNotification.FlagDecisionNotificationBuilder.VARIABLES;
import static com.optimizely.ab.notification.DecisionNotification.FlagDecisionNotificationBuilder.VARIATION_ID;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;
import com.optimizely.ab.optimizelydecision.OptimizelyDecision;
import com.optimizely.ab.optimizelyjson.OptimizelyJSON;

import ch.qos.logback.classic.Level;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class OptimizelyUserContextTest {
    @Rule
    public EventHandlerRule eventHandler = new EventHandlerRule();

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    String userId = "tester";
    boolean isListenerCalled = false;

    Optimizely optimizely;
    String datafile;
    ProjectConfig config;
    Map<String, Experiment> experimentIdMapping;
    Map<String, FeatureFlag> featureKeyMapping;
    Map<String, Group> groupIdMapping;

    private String holdoutDatafile;

    @Before
    public void setUp() throws Exception {
        datafile = Resources.toString(Resources.getResource("config/decide-project-config.json"), Charsets.UTF_8);

        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .build();
    }

    private Optimizely createOptimizelyWithHoldouts() throws Exception {
        if (holdoutDatafile == null) {
            holdoutDatafile = com.google.common.io.Resources.toString(
                com.google.common.io.Resources.getResource("config/holdouts-project-config.json"),
                com.google.common.base.Charsets.UTF_8
            );
        }
        return new Optimizely.Builder().withDatafile(holdoutDatafile).withEventProcessor(new ForwardingEventProcessor(eventHandler, null)).build();
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
        OptimizelyUserContext user_1 = new OptimizelyUserContext(optimizely, userId);
        OptimizelyUserContext user_2 = new OptimizelyUserContext(optimizely, userId);

        assertEquals(user_1.getOptimizely(), optimizely);
        assertEquals(user_1.getUserId(), userId);
        assertTrue(user_1.getAttributes().isEmpty());
        assertEquals(user_1.hashCode(), user_2.hashCode());
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
        EventProcessor mockEventProcessor = mock(EventProcessor.class);

        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withEventProcessor(mockEventProcessor)
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
        assertEquals(decisions.size(), 3);

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

        ArgumentCaptor<ImpressionEvent> argumentCaptor = ArgumentCaptor.forClass(ImpressionEvent.class);
        verify(mockEventProcessor, times(3)).process(argumentCaptor.capture());

        List<ImpressionEvent> sentEvents = argumentCaptor.getAllValues();
        assertEquals(sentEvents.size(), 3);

        assertEquals(sentEvents.get(0).getExperimentKey(), "exp_with_audience");
        assertEquals(sentEvents.get(0).getVariationKey(), "a");
        assertEquals(sentEvents.get(0).getUserContext().getUserId(), userId);


        assertEquals(sentEvents.get(1).getExperimentKey(), "exp_no_audience");
        assertEquals(sentEvents.get(1).getVariationKey(), "variation_with_traffic");
        assertEquals(sentEvents.get(1).getUserContext().getUserId(), userId);

        assertEquals(sentEvents.get(2).getExperimentKey(), "");
        assertEquals(sentEvents.get(2).getUserContext().getUserId(), userId);
    }

    @Test
    public void decideForKeys_ups_batching() throws Exception {
        UserProfileService ups = mock(UserProfileService.class);

        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withUserProfileService(ups)
            .build();

        String flagKey1 = "feature_1";
        String flagKey2 = "feature_2";
        String flagKey3 = "feature_3";
        Map<String, Object> attributes = Collections.singletonMap("gender", "f");

        OptimizelyUserContext user = optimizely.createUserContext(userId, attributes);
        Map<String, OptimizelyDecision> decisions = user.decideForKeys(Arrays.asList(
            flagKey1, flagKey2, flagKey3
        ));

        assertEquals(decisions.size(), 3);

        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);


        verify(ups, times(1)).lookup(userId);
        verify(ups, times(1)).save(argumentCaptor.capture());

        Map<String, Object> savedUps = argumentCaptor.getValue();
        UserProfile savedProfile = UserProfileUtils.convertMapToUserProfile(savedUps);

        assertEquals(savedProfile.userId, userId);
    }

    @Test
    public void decideAll_ups_batching() throws Exception {
        UserProfileService ups = mock(UserProfileService.class);

        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withUserProfileService(ups)
            .build();

        Map<String, Object> attributes = Collections.singletonMap("gender", "f");

        OptimizelyUserContext user = optimizely.createUserContext(userId, attributes);
        Map<String, OptimizelyDecision> decisions = user.decideAll();

        assertEquals(decisions.size(), 3);

        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);


        verify(ups, times(1)).lookup(userId);
        verify(ups, times(1)).save(argumentCaptor.capture());

        Map<String, Object> savedUps = argumentCaptor.getValue();
        UserProfile savedProfile = UserProfileUtils.convertMapToUserProfile(savedUps);

        assertEquals(savedProfile.userId, userId);
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

        // impression event not expected here
    }

    @Test
    public void decide_sendEvent_featureTest_withSendFlagDecisionsOn() {
        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        Map<String, Object> attributes = Collections.singletonMap("gender", "f");
        OptimizelyUserContext user = optimizely.createUserContext(userId, attributes);

        optimizely.addDecisionNotificationHandler(
            decisionNotification -> {
                Assert.assertEquals(decisionNotification.getDecisionInfo().get(DECISION_EVENT_DISPATCHED), true);
                isListenerCalled = true;
            });

        String flagKey = "feature_2";
        String experimentId = "10420810910";
        String variationId = "10418551353";
        isListenerCalled = false;
        user.decide(flagKey);
        assertTrue(isListenerCalled);

        eventHandler.expectImpression(experimentId, variationId, userId, attributes);
    }

    @Test
    public void decide_sendEvent_rollout_withSendFlagDecisionsOn() {
        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        Map<String, Object> attributes = Collections.singletonMap("gender", "f");
        OptimizelyUserContext user = optimizely.createUserContext(userId, attributes);

        optimizely.addDecisionNotificationHandler(
            decisionNotification -> {
                Assert.assertEquals(decisionNotification.getDecisionInfo().get(DECISION_EVENT_DISPATCHED), true);
                isListenerCalled = true;
            });

        String flagKey = "feature_3";
        String experimentId = null;
        String variationId = null;
        isListenerCalled = false;
        user.decide(flagKey);
        assertTrue(isListenerCalled);

        eventHandler.expectImpression(null, "", userId, attributes);
    }

    @Test
    public void decide_sendEvent_featureTest_withSendFlagDecisionsOff() {
        String datafileWithSendFlagDecisionsOff = datafile.replace("\"sendFlagDecisions\": true", "\"sendFlagDecisions\": false");
        optimizely = new Optimizely.Builder()
            .withDatafile(datafileWithSendFlagDecisionsOff)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        Map<String, Object> attributes = Collections.singletonMap("gender", "f");
        OptimizelyUserContext user = optimizely.createUserContext(userId, attributes);

        optimizely.addDecisionNotificationHandler(
            decisionNotification -> {
                Assert.assertEquals(decisionNotification.getDecisionInfo().get(DECISION_EVENT_DISPATCHED), true);
                isListenerCalled = true;
            });

        String flagKey = "feature_2";
        String experimentId = "10420810910";
        String variationId = "10418551353";
        isListenerCalled = false;
        user.decide(flagKey);
        assertTrue(isListenerCalled);

        eventHandler.expectImpression(experimentId, variationId, userId, attributes);
    }

    @Test
    public void decide_sendEvent_rollout_withSendFlagDecisionsOff() {
        String datafileWithSendFlagDecisionsOff = datafile.replace("\"sendFlagDecisions\": true", "\"sendFlagDecisions\": false");
        optimizely = new Optimizely.Builder()
            .withDatafile(datafileWithSendFlagDecisionsOff)
            .withEventProcessor(new ForwardingEventProcessor(eventHandler, null))
            .build();

        Map<String, Object> attributes = Collections.singletonMap("gender", "f");
        OptimizelyUserContext user = optimizely.createUserContext(userId, attributes);

        optimizely.addDecisionNotificationHandler(
            decisionNotification -> {
                Assert.assertEquals(decisionNotification.getDecisionInfo().get(DECISION_EVENT_DISPATCHED), false);
                isListenerCalled = true;
            });

        String flagKey = "feature_3";
        isListenerCalled = false;
        user.decide(flagKey);
        assertTrue(isListenerCalled);

        // impression event not expected here
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
        String experimentId = "10420810910";
        String variationId = "10418551353";

        final Map<String, Object> testDecisionInfoMap = new HashMap<>();
        testDecisionInfoMap.put(FLAG_KEY, flagKey);
        testDecisionInfoMap.put(VARIATION_KEY, variationKey);
        testDecisionInfoMap.put(ENABLED, enabled);
        testDecisionInfoMap.put(VARIABLES, variables.toMap());
        testDecisionInfoMap.put(RULE_KEY, ruleKey);
        testDecisionInfoMap.put(REASONS, reasons);
        testDecisionInfoMap.put(EXPERIMENT_ID, experimentId);
        testDecisionInfoMap.put(VARIATION_ID, variationId);

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

    @Test
    public void decide_for_keys_with_holdout() throws Exception {
        Optimizely optWithHoldout = createOptimizelyWithHoldouts();
        String userId = "user123";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("$opt_bucketing_id", "ppid160000"); 
        OptimizelyUserContext user = optWithHoldout.createUserContext(userId, attrs);

        List<String> flagKeys = Arrays.asList(
            "boolean_feature",                    // previously validated basic_holdout membership
            "double_single_variable_feature",     // also subject to global/basic holdout
            "integer_single_variable_feature"     // also subject to global/basic holdout
        );

        Map<String, OptimizelyDecision> decisions = user.decideForKeys(flagKeys, Collections.singletonList(OptimizelyDecideOption.INCLUDE_REASONS));
        assertEquals(3, decisions.size());

        String holdoutExperimentId = "10075323428"; // basic_holdout id
        String variationId = "$opt_dummy_variation_id";
        String variationKey = "ho_off_key";
        String expectedReason = "User (" + userId + ") is in variation (" + variationKey + ") of holdout (basic_holdout).";

        for (String flagKey : flagKeys) {
            OptimizelyDecision d = decisions.get(flagKey);
            assertNotNull(d);
            assertEquals(flagKey, d.getFlagKey());
            assertEquals(variationKey, d.getVariationKey());
            assertFalse(d.getEnabled());
            assertTrue("Expected holdout reason for flag " + flagKey, d.getReasons().contains(expectedReason));
            DecisionMetadata metadata = new DecisionMetadata.Builder()
                .setFlagKey(flagKey)
                .setRuleKey("basic_holdout")
                .setRuleType("holdout")
                .setVariationKey(variationKey)
                .setEnabled(false)
                .build();
            // attributes map expected empty (reserved $opt_ attribute filtered out)
            eventHandler.expectImpression(holdoutExperimentId, variationId, userId, Collections.emptyMap(), metadata);
        }

        // At least one log message confirming holdout membership
        logbackVerifier.expectMessage(Level.INFO, expectedReason);
    }

    @Test
    public void decide_all_with_holdout() throws Exception {

        Optimizely optWithHoldout = createOptimizelyWithHoldouts();
        String userId = "user123";
        Map<String, Object> attrs = new HashMap<>();
        // ppid120000 buckets user into holdout_included_flags
        attrs.put("$opt_bucketing_id", "ppid120000");
        OptimizelyUserContext user = optWithHoldout.createUserContext(userId, attrs);

        // All flag keys present in holdouts-project-config.json
        List<String> allFlagKeys = Arrays.asList(
            "boolean_feature",
            "double_single_variable_feature",
            "integer_single_variable_feature",
            "boolean_single_variable_feature",
            "string_single_variable_feature",
            "multi_variate_feature",
            "multi_variate_future_feature",
            "mutex_group_feature"
        );

        // Flags INCLUDED in holdout_included_flags (only these should be holdout decisions)
        List<String> includedInHoldout = Arrays.asList(
            "boolean_feature",
            "double_single_variable_feature",
            "integer_single_variable_feature"
        );

        Map<String, OptimizelyDecision> decisions = user.decideAll(Arrays.asList(
            OptimizelyDecideOption.INCLUDE_REASONS,
            OptimizelyDecideOption.DISABLE_DECISION_EVENT
        ));
        assertEquals(allFlagKeys.size(), decisions.size());

        String holdoutExperimentId = "1007543323427"; // holdout_included_flags id
        String variationId = "$opt_dummy_variation_id";
        String variationKey = "ho_off_key";
        String expectedReason = "User (" + userId + ") is in variation (" + variationKey + ") of holdout (holdout_included_flags).";

        int holdoutCount = 0;
        for (String flagKey : allFlagKeys) {
            OptimizelyDecision d = decisions.get(flagKey);
            assertNotNull("Missing decision for flag " + flagKey, d);
            if (includedInHoldout.contains(flagKey)) {
                // Should be holdout decision
                assertEquals(variationKey, d.getVariationKey());
                assertFalse(d.getEnabled());
                assertTrue("Expected holdout reason for flag " + flagKey, d.getReasons().contains(expectedReason));
                DecisionMetadata metadata = new DecisionMetadata.Builder()
                    .setFlagKey(flagKey)
                    .setRuleKey("holdout_included_flags")
                    .setRuleType("holdout")
                    .setVariationKey(variationKey)
                    .setEnabled(false)
                    .build();
                holdoutCount++;
            } else {
                // Should NOT be a holdout decision
                assertFalse("Non-included flag should not have holdout reason: " + flagKey, d.getReasons().contains(expectedReason));
            }
        }
        assertEquals("Expected exactly the included flags to be in holdout", includedInHoldout.size(), holdoutCount);
        logbackVerifier.expectMessage(Level.INFO, expectedReason);
    }

    @Test
    public void decisionNotification_with_holdout() throws Exception {
        // Use holdouts datafile
        Optimizely optWithHoldout = createOptimizelyWithHoldouts();
        String flagKey = "boolean_feature";
        String userId = "user123";
        String ruleKey = "basic_holdout";              // holdout rule key
        String variationKey = "ho_off_key";            // holdout (off) variation key
        String experimentId = "10075323428";           // holdout experiment id in holdouts-project-config.json
        String variationId = "$opt_dummy_variation_id";// dummy variation id used for holdout impressions
        String expectedReason = "User (" + userId + ") is in variation (" + variationKey + ") of holdout (" + ruleKey + ").";

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("$opt_bucketing_id", "ppid160000");  // deterministic bucketing into basic_holdout
        attrs.put("nationality", "English");           // non-reserved attribute should appear in impression & notification

        OptimizelyUserContext user = optWithHoldout.createUserContext(userId, attrs);

        // Register notification handler similar to decisionNotification test
        isListenerCalled = false;
        optWithHoldout.addDecisionNotificationHandler(decisionNotification -> {
            Assert.assertEquals(NotificationCenter.DecisionNotificationType.FLAG.toString(), decisionNotification.getType());
            Assert.assertEquals(userId, decisionNotification.getUserId());

            Assert.assertEquals(attrs, decisionNotification.getAttributes());

            Map<String, ?> info = decisionNotification.getDecisionInfo();
            Assert.assertEquals(flagKey, info.get(FLAG_KEY));
            Assert.assertEquals(variationKey, info.get(VARIATION_KEY));
            Assert.assertEquals(false, info.get(ENABLED));
            Assert.assertEquals(ruleKey, info.get(RULE_KEY));
            Assert.assertEquals(experimentId, info.get(EXPERIMENT_ID));
            Assert.assertEquals(variationId, info.get(VARIATION_ID));
            // Variables should be empty because feature is disabled by holdout
            Assert.assertTrue(((Map<?, ?>) info.get(VARIABLES)).isEmpty());
            // Event should be dispatched (no DISABLE_DECISION_EVENT option)
            Assert.assertEquals(true, info.get(DECISION_EVENT_DISPATCHED));

            @SuppressWarnings("unchecked")
            List<String> reasons = (List<String>) info.get(REASONS);
            Assert.assertTrue("Expected holdout reason present", reasons.contains(expectedReason));
            isListenerCalled = true;
        });

        // Execute decision with INCLUDE_REASONS so holdout reason is present
        OptimizelyDecision decision = user.decide(flagKey, Collections.singletonList(OptimizelyDecideOption.INCLUDE_REASONS));
        assertTrue(isListenerCalled);

        // Sanity checks on returned decision
        assertEquals(variationKey, decision.getVariationKey());
        assertFalse(decision.getEnabled());
        assertTrue(decision.getReasons().contains(expectedReason));

        // Impression expectation (nationality only)
        DecisionMetadata metadata = new DecisionMetadata.Builder()
                .setFlagKey(flagKey)
                .setRuleKey(ruleKey)
                .setRuleType("holdout")
                .setVariationKey(variationKey)
                .setEnabled(false)
                .build();
        eventHandler.expectImpression(experimentId, variationId, userId, Collections.singletonMap("nationality", "English"), metadata);

        // Log expectation (reuse existing pattern)
        logbackVerifier.expectMessage(Level.INFO, expectedReason);
    }

}

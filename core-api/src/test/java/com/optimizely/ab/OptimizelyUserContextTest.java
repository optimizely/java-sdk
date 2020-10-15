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
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.DatafileProjectConfig;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Rollout;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.event.ForwardingEventProcessor;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class OptimizelyUserContextTest {
    @Rule
    public EventHandlerRule eventHandler = new EventHandlerRule();

    public Optimizely optimizely;
    public String datafile;
    public String userId = "tester";
    boolean isListenerCalled = false;

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
        assertEquals(decision.getRuleKey(), "exp_no_audience");
        assertEquals(decision.getFlagKey(), flagKey);
        assertEquals(decision.getUserContext(), user);
        assertTrue(decision.getReasons().isEmpty());
    }

    // decideAll

    @Test
    public void decideAll_oneFlag() {
        String flagKey = "feature_2";
        List<String> flagKeys = Arrays.asList(flagKey);
        OptimizelyJSON variablesExpected = optimizely.getAllFeatureVariables(flagKey, userId);

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        Map<String, OptimizelyDecision> decisions = user.decideForKeys(flagKeys);

        assertTrue(decisions.size() == 1);
        OptimizelyDecision decision = decisions.get(flagKey);

        OptimizelyDecision expDecision = new OptimizelyDecision(
            "variation_with_traffic",
            true,
            variablesExpected,
            "exp_no_audience",
            flagKey,
            user,
            Collections.emptyList());
        assertEquals(decision, expDecision);
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

        Map<String, ?> attributes = Collections.singletonMap("gender", "f");
        String eventKey = "event1";
        Map<String, ?> eventTags = Collections.singletonMap("name", "carrot");
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

        Map<String, ?> attributes = Collections.singletonMap("gender", "f");
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
        String experimentId = "10420810910";
        String variationId = "10418551353";

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey);

        assertEquals(decision.getVariationKey(), "variation_with_traffic");

        eventHandler.expectImpression(experimentId, variationId, userId);
    }

    @Test
    public void decide_doNotSendEvent() {
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
        assertNull(decision.getVariables());
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
            OptimizelyDecision.createErrorDecision(
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
    }

    // reasons (logs with includeReasons)

    @Test
    public void decideReasons_conditionNoMatchingAudience() throws ConfigParseException {
        String flagKey = "feature_1";
        String audienceId = "invalid_id";
        setAudienceForFeatureTest(flagKey, audienceId);

        OptimizelyUserContext user = optimizely.createUserContext(userId);
        OptimizelyDecision decision = user.decide(flagKey, Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS));

        assertTrue(decision.getReasons().contains(
            String.format("Audience %s could not be found.", audienceId)
        ));
    }

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

    }

    @Test
    public void decideReasons_forcedVariationFoundButInvalid() {

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
    }

    @Test
    public void decideReasons_userBucketedIntoInvalidVariation() {
    }

    @Test
    public void decideReasons_userBucketedIntoExperimentInGroup() {

    }
    @Test
    public void decideReasons_userNotBucketedIntoExperimentInGroup() {

    }
    @Test
    public void decideReasons_userNotBucketedIntoAnyExperimentInGroup() {

    }
    @Test
    public void decideReasons_userBucketedIntoInvalidExperiment() {

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
        ProjectConfig configReal = new DatafileProjectConfig.Builder().withDatafile(datafile).build();
        ProjectConfig config = spy(configReal);
        optimizely = Optimizely.builder().withConfig(config).build();

        String experimentId = config.getFeatureKeyMapping().get(flagKey).getExperimentIds().get(0);
        String rolloutId = config.getFeatureKeyMapping().get(flagKey).getRolloutId();
        Map<String, Experiment> experimentIdMapping = new HashMap<>(config.getExperimentIdMapping());
        Map<String, Rollout> rolloutIdMapping = new HashMap<>(config.getRolloutIdMapping());
        Experiment experimentReal = experimentIdMapping.get(experimentId);
        Rollout rolloutReal = rolloutIdMapping.get(rolloutId);

        Experiment experiment = spy(experimentReal);
        Rollout rollout = spy(rolloutReal);
        when(experiment.getAudienceIds()).thenReturn(Arrays.asList(audienceId));

        experimentIdMapping.put(experimentId, experiment);
        rolloutIdMapping.put(rolloutId, rollout);

        when(config.getExperimentIdMapping()).thenReturn(experimentIdMapping);
        when(config.getRolloutIdMapping()).thenReturn(rolloutIdMapping);
    }

}

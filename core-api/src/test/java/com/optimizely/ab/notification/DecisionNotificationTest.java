/**
 *
 *    Copyright 2019, Optimizely and contributors
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

package com.optimizely.ab.notification;

import com.optimizely.ab.OptimizelyRuntimeException;
import com.optimizely.ab.bucketing.FeatureDecision;
import com.optimizely.ab.config.FeatureVariable;
import com.optimizely.ab.config.Variation;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class DecisionNotificationTest {

    private static final Boolean FEATURE_ENABLED = Boolean.FALSE;
    private static final String EXPERIMENT_KEY = "experimentKey";
    private static final String EXPERIMENT_ID = "1234567";
    private static final String FEATURE_KEY = "featureKey";
    private static final String FEATURE_VARIABLE_KEY = "featureVariableKey";
    private static final String FEATURE_TEST = "featureTest";
    private static final String FEATURE_TEST_VARIATION = "featureTestVariation";
    private static final String USER_ID = "userID";
    private static final Map<String, String> USER_ATTRIBUTES = Collections.singletonMap("user", "attr");
    private static final Variation VARIATION = mock(Variation.class);
    private static final String VARIATION_ID = "1234567";

    private FeatureTestSourceInfo featureTestSourceInfo;
    private RolloutSourceInfo rolloutSourceInfo;
    private DecisionNotification experimentDecisionNotification;
    private DecisionNotification featureDecisionNotification;
    private DecisionNotification featureVariableDecisionNotification;

    @Before
    public void setUp() {
        experimentDecisionNotification = DecisionNotification.newExperimentDecisionNotificationBuilder()
            .withUserId(USER_ID)
            .withAttributes(USER_ATTRIBUTES)
            .withExperimentKey(EXPERIMENT_KEY)
            .withVariation(VARIATION)
            .withExperimentId(EXPERIMENT_ID)
            .withVariationId(VARIATION_ID)
            .withType(NotificationCenter.DecisionNotificationType.AB_TEST.toString())
            .build();
        featureTestSourceInfo = new FeatureTestSourceInfo(FEATURE_TEST, FEATURE_TEST_VARIATION);
        rolloutSourceInfo = new RolloutSourceInfo();
        featureDecisionNotification = DecisionNotification.newFeatureDecisionNotificationBuilder()
            .withUserId(USER_ID)
            .withFeatureKey(FEATURE_KEY)
            .withFeatureEnabled(FEATURE_ENABLED)
            .withSource(FeatureDecision.DecisionSource.FEATURE_TEST)
            .withAttributes(USER_ATTRIBUTES)
            .withSourceInfo(featureTestSourceInfo)
            .build();
        featureVariableDecisionNotification = DecisionNotification.newFeatureVariableDecisionNotificationBuilder()
            .withUserId(USER_ID)
            .withFeatureKey(FEATURE_KEY)
            .withFeatureEnabled(Boolean.TRUE)
            .withVariableKey(FEATURE_VARIABLE_KEY)
            .withVariableType(FeatureVariable.STRING_TYPE)
            .withAttributes(USER_ATTRIBUTES)
            .build();
    }

    @Test
    public void testGetType() {
        assertEquals(NotificationCenter.DecisionNotificationType.AB_TEST.toString(), experimentDecisionNotification.getType());
        assertEquals(NotificationCenter.DecisionNotificationType.FEATURE.toString(), featureDecisionNotification.getType());
        assertEquals(NotificationCenter.DecisionNotificationType.FEATURE_VARIABLE.toString(), featureVariableDecisionNotification.getType());
    }

    @Test
    public void testGetUserId() {
        assertEquals(USER_ID, experimentDecisionNotification.getUserId());
        assertEquals(USER_ID, featureDecisionNotification.getUserId());
        assertEquals(USER_ID, featureVariableDecisionNotification.getUserId());
    }

    @Test
    public void testGetAttributes() {
        assertEquals(USER_ATTRIBUTES, experimentDecisionNotification.getAttributes());
        assertEquals(USER_ATTRIBUTES, featureDecisionNotification.getAttributes());
        assertEquals(USER_ATTRIBUTES, featureVariableDecisionNotification.getAttributes());
    }

    @Test
    public void testGetDecisionInfo() {
        // Assert for Experiment's DecisionInfo
        HashMap<String, String> expectedExperimentDecisionInfo = new HashMap<>();
        expectedExperimentDecisionInfo.put(DecisionNotification.ExperimentDecisionNotificationBuilder.EXPERIMENT_KEY, EXPERIMENT_KEY);
        expectedExperimentDecisionInfo.put(DecisionNotification.ExperimentDecisionNotificationBuilder.VARIATION_KEY, VARIATION.getKey());
        expectedExperimentDecisionInfo.put(DecisionNotification.ExperimentDecisionNotificationBuilder.EXPERIMENT_ID, EXPERIMENT_ID);
        expectedExperimentDecisionInfo.put(DecisionNotification.ExperimentDecisionNotificationBuilder.VARIATION_ID, VARIATION_ID);
        assertEquals(expectedExperimentDecisionInfo, experimentDecisionNotification.getDecisionInfo());

        // Assert for Feature's DecisionInfo
        Map<String, ?> actualFeatureDecisionInfo = featureDecisionNotification.getDecisionInfo();
        assertFalse((Boolean) actualFeatureDecisionInfo.get(DecisionNotification.FeatureDecisionNotificationBuilder.FEATURE_ENABLED));
        assertEquals(FEATURE_KEY, actualFeatureDecisionInfo.get(DecisionNotification.FeatureDecisionNotificationBuilder.FEATURE_KEY));
        assertEquals(FeatureDecision.DecisionSource.FEATURE_TEST.toString(), actualFeatureDecisionInfo.get(DecisionNotification.FeatureDecisionNotificationBuilder.SOURCE));
        assertEquals(featureTestSourceInfo.get(), actualFeatureDecisionInfo.get(DecisionNotification.FeatureDecisionNotificationBuilder.SOURCE_INFO));

        // Assert for Feature Variable's DecisionInfo
        Map<String, ?> actualFeatureVariableDecisionInfo = featureVariableDecisionNotification.getDecisionInfo();
        assertTrue((Boolean) actualFeatureVariableDecisionInfo.get(DecisionNotification.FeatureVariableDecisionNotificationBuilder.FEATURE_ENABLED));
        assertEquals(FEATURE_KEY, actualFeatureVariableDecisionInfo.get(DecisionNotification.FeatureVariableDecisionNotificationBuilder.FEATURE_KEY));
        assertEquals(FEATURE_VARIABLE_KEY, actualFeatureVariableDecisionInfo.get(DecisionNotification.FeatureVariableDecisionNotificationBuilder.VARIABLE_KEY));
        assertEquals(FeatureVariable.STRING_TYPE, actualFeatureVariableDecisionInfo.get(DecisionNotification.FeatureVariableDecisionNotificationBuilder.VARIABLE_TYPE));
        assertEquals(FeatureDecision.DecisionSource.ROLLOUT.toString(), actualFeatureVariableDecisionInfo.get(DecisionNotification.FeatureVariableDecisionNotificationBuilder.SOURCE));
        assertEquals(rolloutSourceInfo.get(), actualFeatureVariableDecisionInfo.get(DecisionNotification.FeatureVariableDecisionNotificationBuilder.SOURCE_INFO));
    }

    @Test
    public void testToString() {
        assertEquals("DecisionNotification{type='ab-test', userId='userID', attributes={user=attr}, decisionInfo={experimentKey=experimentKey, variationKey=null, experimentId='1234567', variationId='1234567'}}", experimentDecisionNotification.toString());
        assertEquals("DecisionNotification{type='feature', userId='userID', attributes={user=attr}, decisionInfo={featureEnabled=false, sourceInfo={experimentKey=featureTest, variationKey=featureTestVariation}, source=feature-test, featureKey=featureKey}}", featureDecisionNotification.toString());
        assertEquals("DecisionNotification{type='feature-variable', userId='userID', attributes={user=attr}, decisionInfo={variableType=string, featureEnabled=true, sourceInfo={}, variableValue=null, variableKey=featureVariableKey, source=rollout, featureKey=featureKey}}", featureVariableDecisionNotification.toString());
    }

    @Test(expected = OptimizelyRuntimeException.class)
    public void nullTypeFailsExperimentNotificationBuild() {
        DecisionNotification.newExperimentDecisionNotificationBuilder()
            .withExperimentKey(EXPERIMENT_KEY)
            .build();
    }

    @Test(expected = OptimizelyRuntimeException.class)
    public void nullExperimentKeyFailsExperimentNotificationBuild() {
        DecisionNotification.newExperimentDecisionNotificationBuilder()
            .withType(NotificationCenter.DecisionNotificationType.AB_TEST.toString())
            .build();
    }

    @Test(expected = OptimizelyRuntimeException.class)
    public void nullSourceFailsFeatureNotificationBuild() {
        DecisionNotification.newFeatureDecisionNotificationBuilder()
            .withFeatureKey(FEATURE_KEY)
            .withFeatureEnabled(FEATURE_ENABLED)
            .build();
    }

    @Test(expected = OptimizelyRuntimeException.class)
    public void nullFeatureKeyFailsFeatureNotificationBuild() {
        DecisionNotification.newFeatureDecisionNotificationBuilder()
            .withFeatureEnabled(FEATURE_ENABLED)
            .withSource(FeatureDecision.DecisionSource.ROLLOUT)
            .build();
    }

    @Test(expected = OptimizelyRuntimeException.class)
    public void nullFeatureEnabledFailsFeatureNotificationBuild() {
        DecisionNotification.newFeatureDecisionNotificationBuilder()
            .withFeatureKey(FEATURE_KEY)
            .withSource(FeatureDecision.DecisionSource.ROLLOUT)
            .build();
    }

    @Test(expected = OptimizelyRuntimeException.class)
    public void nullFeatureKeyFailsFeatureVariableNotificationBuild() {
        DecisionNotification.newFeatureVariableDecisionNotificationBuilder()
            .withFeatureEnabled(Boolean.TRUE)
            .withVariableKey(FEATURE_VARIABLE_KEY)
            .withVariableType(FeatureVariable.STRING_TYPE)
            .build();
    }

    @Test(expected = OptimizelyRuntimeException.class)
    public void nullFeatureEnabledFailsFeatureVariableNotificationBuild() {
        DecisionNotification.newFeatureVariableDecisionNotificationBuilder()
            .withFeatureKey(FEATURE_KEY)
            .withVariableKey(FEATURE_VARIABLE_KEY)
            .withVariableType(FeatureVariable.STRING_TYPE)
            .build();
    }

    @Test(expected = OptimizelyRuntimeException.class)
    public void nullVariableKeyFailsFeatureVariableNotificationBuild() {
        DecisionNotification.newFeatureVariableDecisionNotificationBuilder()
            .withFeatureKey(FEATURE_KEY)
            .withFeatureEnabled(Boolean.TRUE)
            .withVariableType(FeatureVariable.STRING_TYPE)
            .build();
    }

    @Test(expected = OptimizelyRuntimeException.class)
    public void nullVariableTypeFailsFeatureVariableNotificationBuild() {
        DecisionNotification.newFeatureVariableDecisionNotificationBuilder()
            .withFeatureKey(FEATURE_KEY)
            .withFeatureEnabled(Boolean.TRUE)
            .withVariableKey(FEATURE_VARIABLE_KEY)
            .build();
    }
}

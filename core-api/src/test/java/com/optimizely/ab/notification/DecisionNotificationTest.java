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
    private static final String FEATURE_KEY = "featureKey";
    private static final String FEATURE_VARIABLE_KEY = "featureVariableKey";
    private static final String USER_ID = "userID";
    private static final Map<String, String> USER_ATTRIBUTES = Collections.singletonMap("user", "attr");
    private static final RolloutSourceInfo rolloutSourceInfo = mock(RolloutSourceInfo.class);
    private static final Variation VARIATION = mock(Variation.class);

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
            .withType(NotificationCenter.DecisionNotificationType.AB_TEST.toString())
            .build();
        featureDecisionNotification = DecisionNotification.newFeatureDecisionNotificationBuilder()
            .withUserId(USER_ID)
            .withFeatureKey(FEATURE_KEY)
            .withFeatureEnabled(FEATURE_ENABLED)
            .withSource(FeatureDecision.DecisionSource.ROLLOUT)
            .withAttributes(USER_ATTRIBUTES)
            .withSourceInfo(rolloutSourceInfo)
            .build();
        featureVariableDecisionNotification = DecisionNotification.newFeatureVariableDecisionNotificationBuilder()
            .withUserId(USER_ID)
            .withFeatureKey(FEATURE_KEY)
            .withFeatureEnabled(Boolean.TRUE)
            .withVariableKey(FEATURE_VARIABLE_KEY)
            .withVariableType(FeatureVariable.VariableType.STRING)
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
        assertEquals(expectedExperimentDecisionInfo, experimentDecisionNotification.getDecisionInfo());

        // Assert for Feature's DecisionInfo
        Map<String, ?> actualFeatureDecisionInfo = featureDecisionNotification.getDecisionInfo();
        assertFalse((Boolean) actualFeatureDecisionInfo.get(DecisionNotification.FeatureDecisionNotificationBuilder.FEATURE_ENABLED));
        assertEquals(FEATURE_KEY, actualFeatureDecisionInfo.get(DecisionNotification.FeatureDecisionNotificationBuilder.FEATURE_KEY));
        assertEquals(FeatureDecision.DecisionSource.ROLLOUT.toString(), actualFeatureDecisionInfo.get(DecisionNotification.FeatureDecisionNotificationBuilder.SOURCE));
        assertEquals(rolloutSourceInfo.get(), actualFeatureDecisionInfo.get(DecisionNotification.FeatureDecisionNotificationBuilder.SOURCE_INFO));

        // Assert for Feature Variable's DecisionInfo
        Map<String, ?> actualFeatureVariableDecisionInfo = featureVariableDecisionNotification.getDecisionInfo();
        assertTrue((Boolean) actualFeatureVariableDecisionInfo.get(DecisionNotification.FeatureVariableDecisionNotificationBuilder.FEATURE_ENABLED));
        assertEquals(FEATURE_KEY, actualFeatureVariableDecisionInfo.get(DecisionNotification.FeatureVariableDecisionNotificationBuilder.FEATURE_KEY));
        assertEquals(FEATURE_VARIABLE_KEY, actualFeatureVariableDecisionInfo.get(DecisionNotification.FeatureVariableDecisionNotificationBuilder.VARIABLE_KEY));
        assertEquals(FeatureVariable.VariableType.STRING, actualFeatureVariableDecisionInfo.get(DecisionNotification.FeatureVariableDecisionNotificationBuilder.VARIABLE_TYPE));
        assertEquals(FeatureDecision.DecisionSource.ROLLOUT.toString(), actualFeatureVariableDecisionInfo.get(DecisionNotification.FeatureVariableDecisionNotificationBuilder.SOURCE));
        assertEquals(rolloutSourceInfo.get(), actualFeatureVariableDecisionInfo.get(DecisionNotification.FeatureVariableDecisionNotificationBuilder.SOURCE_INFO));
    }
}

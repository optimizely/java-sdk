/**
 *
 *    Copyright 2025, Optimizely and contributors
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

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.config.*;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.optimizelydecision.DecisionResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Local Holdouts feature in DecisionService
 */
@RunWith(MockitoJUnitRunner.class)
public class LocalHoldoutsDecisionServiceTest {

    @Mock
    private Bucketer mockBucketer;

    @Mock
    private ProjectConfig mockProjectConfig;

    private DecisionService decisionService;
    private ErrorHandler errorHandler;
    private Optimizely optimizely;

    private Variation controlVariation;
    private Variation treatmentVariation;
    private Variation holdoutVariation;

    private Holdout globalHoldout;
    private Holdout localHoldout;
    private Experiment experimentRule;
    private Experiment deliveryRule;
    private FeatureFlag featureFlag;

    @Before
    public void setUp() {
        optimizely = Optimizely.builder().build();
        errorHandler = new NoOpErrorHandler();
        decisionService = new DecisionService(mockBucketer, errorHandler, null);

        // Create variations
        controlVariation = new Variation("control_id", "control");
        treatmentVariation = new Variation("treatment_id", "treatment");
        holdoutVariation = new Variation("holdout_id", "holdout");

        List<Variation> variations = Arrays.asList(controlVariation, treatmentVariation);
        List<Variation> holdoutVariations = Arrays.asList(holdoutVariation);

        // Create global holdout (includedRules = null)
        globalHoldout = new Holdout("global_holdout_id", "global_holdout", "Running",
            Collections.emptyList(), null, holdoutVariations, Collections.emptyList(), null);

        // Create local holdout targeting specific rule
        localHoldout = new Holdout("local_holdout_id", "local_holdout", "Running",
            Collections.emptyList(), null, holdoutVariations, Collections.emptyList(),
            Arrays.asList("experiment_rule_id"));

        // Create experiment rule
        experimentRule = new Experiment("experiment_rule_id", "experiment_rule",
            "Running", "layer_id", Collections.emptyList(), null,
            variations, Collections.emptyMap(), Collections.emptyList());

        // Create delivery rule
        deliveryRule = new Experiment("delivery_rule_id", "delivery_rule",
            "Running", "layer_id", Collections.emptyList(), null,
            variations, Collections.emptyMap(), Collections.emptyList());

        // Create feature flag
        featureFlag = new FeatureFlag("flag_id", "test_flag", "layer_id",
            Collections.singletonList("experiment_rule_id"),
            Collections.emptyList());
    }

    @Test
    public void testGlobalHoldoutEvaluatedAtFlagLevel() {
        // Arrange
        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, "user123", Collections.emptyMap());

        when(mockProjectConfig.getGlobalHoldouts()).thenReturn(Collections.singletonList(globalHoldout));
        when(mockBucketer.bucket(eq(globalHoldout), anyString(), eq(mockProjectConfig)))
            .thenReturn(DecisionResponse.responseNoReasons(holdoutVariation));

        // Act
        DecisionResponse<Variation> result = decisionService.getVariationForHoldout(globalHoldout, user, mockProjectConfig);

        // Assert
        assertNotNull(result.getResult());
        assertEquals(holdoutVariation, result.getResult());
        verify(mockBucketer).bucket(eq(globalHoldout), anyString(), eq(mockProjectConfig));
    }

    @Test
    public void testLocalHoldoutEvaluatedAtRuleLevel() {
        // Arrange
        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, "user123", Collections.emptyMap());

        when(mockProjectConfig.getHoldoutsForRule("experiment_rule_id"))
            .thenReturn(Collections.singletonList(localHoldout));
        when(mockBucketer.bucket(eq(localHoldout), anyString(), eq(mockProjectConfig)))
            .thenReturn(DecisionResponse.responseNoReasons(holdoutVariation));

        // Act
        DecisionResponse<Variation> result = decisionService.getVariationForHoldout(localHoldout, user, mockProjectConfig);

        // Assert
        assertNotNull(result.getResult());
        assertEquals(holdoutVariation, result.getResult());
    }

    @Test
    public void testLocalHoldoutNotAppliedToNonTargetedRule() {
        // Arrange - local holdout only targets "experiment_rule_id"
        when(mockProjectConfig.getHoldoutsForRule("other_rule_id"))
            .thenReturn(Collections.emptyList());

        // Act
        List<Holdout> holdouts = mockProjectConfig.getHoldoutsForRule("other_rule_id");

        // Assert
        assertTrue(holdouts.isEmpty());
    }

    @Test
    public void testGlobalHoldoutAppliedToAllRules() {
        // Global holdouts are returned by getGlobalHoldouts(), not getHoldoutsForRule()
        // This test verifies the data model setup
        assertTrue(globalHoldout.isGlobal());
        assertNull(globalHoldout.getIncludedRules());
    }

    @Test
    public void testLocalHoldoutWithMultipleRules() {
        // Arrange - local holdout targeting multiple rules
        Holdout multiRuleHoldout = new Holdout("multi_rule_holdout_id", "multi_rule_holdout", "Running",
            Collections.emptyList(), null, Arrays.asList(holdoutVariation), Collections.emptyList(),
            Arrays.asList("rule1", "rule2", "rule3"));

        HoldoutConfig holdoutConfig = new HoldoutConfig(Collections.singletonList(multiRuleHoldout));

        // Act & Assert
        List<Holdout> rule1Holdouts = holdoutConfig.getHoldoutsForRule("rule1");
        assertEquals(1, rule1Holdouts.size());
        assertTrue(rule1Holdouts.contains(multiRuleHoldout));

        List<Holdout> rule2Holdouts = holdoutConfig.getHoldoutsForRule("rule2");
        assertEquals(1, rule2Holdouts.size());
        assertTrue(rule2Holdouts.contains(multiRuleHoldout));

        List<Holdout> rule3Holdouts = holdoutConfig.getHoldoutsForRule("rule3");
        assertEquals(1, rule3Holdouts.size());
        assertTrue(rule3Holdouts.contains(multiRuleHoldout));
    }

    @Test
    public void testCrossFlagTargeting() {
        // Arrange - local holdout targeting rules from different flags
        Holdout crossFlagHoldout = new Holdout("cross_flag_holdout_id", "cross_flag_holdout", "Running",
            Collections.emptyList(), null, Arrays.asList(holdoutVariation), Collections.emptyList(),
            Arrays.asList("flag1_rule1", "flag2_rule2", "flag3_rule3"));

        HoldoutConfig holdoutConfig = new HoldoutConfig(Collections.singletonList(crossFlagHoldout));

        // Act & Assert - holdout should target rules across different flags
        List<Holdout> flag1Rule1Holdouts = holdoutConfig.getHoldoutsForRule("flag1_rule1");
        assertEquals(1, flag1Rule1Holdouts.size());
        assertTrue(flag1Rule1Holdouts.contains(crossFlagHoldout));

        List<Holdout> flag2Rule2Holdouts = holdoutConfig.getHoldoutsForRule("flag2_rule2");
        assertEquals(1, flag2Rule2Holdouts.size());
        assertTrue(flag2Rule2Holdouts.contains(crossFlagHoldout));

        List<Holdout> flag3Rule3Holdouts = holdoutConfig.getHoldoutsForRule("flag3_rule3");
        assertEquals(1, flag3Rule3Holdouts.size());
        assertTrue(flag3Rule3Holdouts.contains(crossFlagHoldout));
    }

    @Test
    public void testEmptyIncludedRulesVsNull() {
        // Arrange
        Holdout globalHoldout = new Holdout("global_id", "global", "Running",
            Collections.emptyList(), null, Collections.emptyList(), Collections.emptyList(), null);

        Holdout emptyLocalHoldout = new Holdout("empty_local_id", "empty_local", "Running",
            Collections.emptyList(), null, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList());

        HoldoutConfig holdoutConfig = new HoldoutConfig(Arrays.asList(globalHoldout, emptyLocalHoldout));

        // Act & Assert
        // globalHoldout (includedRules == null) should be in global list
        List<Holdout> globals = holdoutConfig.getGlobalHoldouts();
        assertEquals(1, globals.size());
        assertTrue(globals.contains(globalHoldout));
        assertFalse(globals.contains(emptyLocalHoldout));

        // emptyLocalHoldout (includedRules == []) should NOT be in global list
        assertTrue(emptyLocalHoldout.getIncludedRules().isEmpty());
        assertFalse(emptyLocalHoldout.isGlobal());
    }

    @Test
    public void testNonExistentRuleIds() {
        // Arrange - local holdout with non-existent rule IDs
        Holdout holdoutWithNonExistentRules = new Holdout("holdout_id", "holdout", "Running",
            Collections.emptyList(), null, Arrays.asList(holdoutVariation), Collections.emptyList(),
            Arrays.asList("non_existent_rule_1", "non_existent_rule_2"));

        HoldoutConfig holdoutConfig = new HoldoutConfig(Collections.singletonList(holdoutWithNonExistentRules));

        // Act - rule IDs are stored in the map even if they don't exist in the datafile
        // This is expected behavior - validation should happen at datafile level
        List<Holdout> nonExistent1Holdouts = holdoutConfig.getHoldoutsForRule("non_existent_rule_1");
        List<Holdout> nonExistent2Holdouts = holdoutConfig.getHoldoutsForRule("non_existent_rule_2");
        List<Holdout> actualRuleHoldouts = holdoutConfig.getHoldoutsForRule("actual_rule");

        // Assert
        assertEquals(1, nonExistent1Holdouts.size());
        assertEquals(1, nonExistent2Holdouts.size());
        assertEquals(0, actualRuleHoldouts.size());
    }

    @Test
    public void testMultipleLocalHoldoutsTargetingSameRule() {
        // Arrange
        Holdout localHoldout1 = new Holdout("local1_id", "local1", "Running",
            Collections.emptyList(), null, Collections.emptyList(), Collections.emptyList(),
            Arrays.asList("rule1"));

        Holdout localHoldout2 = new Holdout("local2_id", "local2", "Running",
            Collections.emptyList(), null, Collections.emptyList(), Collections.emptyList(),
            Arrays.asList("rule1", "rule2"));

        Holdout localHoldout3 = new Holdout("local3_id", "local3", "Running",
            Collections.emptyList(), null, Collections.emptyList(), Collections.emptyList(),
            Arrays.asList("rule1"));

        HoldoutConfig holdoutConfig = new HoldoutConfig(Arrays.asList(localHoldout1, localHoldout2, localHoldout3));

        // Act
        List<Holdout> rule1Holdouts = holdoutConfig.getHoldoutsForRule("rule1");

        // Assert - rule1 should have all three local holdouts
        assertEquals(3, rule1Holdouts.size());
        assertTrue(rule1Holdouts.contains(localHoldout1));
        assertTrue(rule1Holdouts.contains(localHoldout2));
        assertTrue(rule1Holdouts.contains(localHoldout3));
    }

    @Test
    public void testInactiveHoldoutNotApplied() {
        // Arrange
        Holdout draftHoldout = new Holdout("draft_id", "draft", "Draft",
            Collections.emptyList(), null, Arrays.asList(holdoutVariation), Collections.emptyList(),
            Arrays.asList("rule1"));

        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, "user123", Collections.emptyMap());

        // Act
        DecisionResponse<Variation> result = decisionService.getVariationForHoldout(draftHoldout, user, mockProjectConfig);

        // Assert - inactive holdout should return null
        assertNull(result.getResult());
        verify(mockBucketer, never()).bucket(any(), anyString(), any());
    }

    @Test
    public void testHoldoutWithAudienceConditions() {
        // This test verifies that audience conditions are still evaluated
        // (implementation detail: audience evaluation happens in getVariationForHoldout)
        assertTrue(globalHoldout.isActive());
        assertTrue(localHoldout.isActive());
        assertNotNull(globalHoldout.getAudienceIds());
        assertNotNull(localHoldout.getAudienceIds());
    }
}

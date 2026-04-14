/**
 *
 *    Copyright 2016-2019, 2021, Optimizely and contributors
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
package com.optimizely.ab.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

public class HoldoutConfigTest {

    private Holdout globalHoldout1;
    private Holdout globalHoldout2;
    private Holdout localSingleRule;
    private Holdout localMultipleRulesSameFlag;
    private Holdout localCrossFlagRules;
    private Holdout emptyLocalHoldout;

    @Before
    public void setUp() {
        // Global holdout (includedRules == null)
        globalHoldout1 = new Holdout("global1", "global_holdout_1", "Running",
                Collections.emptyList(), null, Collections.emptyList(),
                Collections.emptyList(), null);

        // Another global holdout
        globalHoldout2 = new Holdout("global2", "global_holdout_2", "Running",
                Collections.emptyList(), null, Collections.emptyList(),
                Collections.emptyList(), null);

        // Local holdout with single rule
        localSingleRule = new Holdout("local1", "local_single_rule", "Running",
                Collections.emptyList(), null, Collections.emptyList(),
                Collections.emptyList(), Arrays.asList("rule1"));

        // Local holdout with multiple rules (same flag)
        localMultipleRulesSameFlag = new Holdout("local2", "local_multiple_same_flag", "Running",
                Collections.emptyList(), null, Collections.emptyList(),
                Collections.emptyList(), Arrays.asList("rule1", "rule2", "rule3"));

        // Local holdout with cross-flag targeting
        localCrossFlagRules = new Holdout("local3", "local_cross_flag", "Running",
                Collections.emptyList(), null, Collections.emptyList(),
                Collections.emptyList(), Arrays.asList("rule1", "rule4", "rule5"));

        // Local holdout with empty rules list (different from null)
        emptyLocalHoldout = new Holdout("local_empty", "empty_rules", "Running",
                Collections.emptyList(), null, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList());
    }

    @Test
    public void testEmptyConstructor() {
        HoldoutConfig config = new HoldoutConfig();

        assertTrue(config.getAllHoldouts().isEmpty());
        assertTrue(config.getGlobalHoldouts().isEmpty());
        assertTrue(config.getHoldoutsForRule("any_rule").isEmpty());
        assertNull(config.getHoldout("any_id"));
    }

    @Test
    public void testConstructorWithEmptyList() {
        HoldoutConfig config = new HoldoutConfig(Collections.emptyList());

        assertTrue(config.getAllHoldouts().isEmpty());
        assertTrue(config.getGlobalHoldouts().isEmpty());
        assertTrue(config.getHoldoutsForRule("any_rule").isEmpty());
        assertNull(config.getHoldout("any_id"));
    }

    @Test
    public void testGetHoldout() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout1, localSingleRule);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        assertEquals(globalHoldout1, config.getHoldout("global1"));
        assertEquals(localSingleRule, config.getHoldout("local1"));
        assertNull(config.getHoldout("nonexistent"));
    }

    @Test
    public void testGetAllHoldouts() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout1, localSingleRule, globalHoldout2);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        List<Holdout> allHoldouts = config.getAllHoldouts();
        assertEquals(3, allHoldouts.size());
        assertTrue(allHoldouts.contains(globalHoldout1));
        assertTrue(allHoldouts.contains(localSingleRule));
        assertTrue(allHoldouts.contains(globalHoldout2));
    }

    @Test
    public void testGetAllHoldoutsIsUnmodifiable() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout1, localSingleRule);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        List<Holdout> allHoldouts = config.getAllHoldouts();

        try {
            allHoldouts.add(globalHoldout2);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testGlobalHoldoutsOnly() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout1, globalHoldout2);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        List<Holdout> globals = config.getGlobalHoldouts();
        assertEquals(2, globals.size());
        assertTrue(globals.contains(globalHoldout1));
        assertTrue(globals.contains(globalHoldout2));

        // No rules should have local holdouts
        assertTrue(config.getHoldoutsForRule("rule1").isEmpty());
        assertTrue(config.getHoldoutsForRule("rule2").isEmpty());
    }

    @Test
    public void testLocalHoldoutSingleRule() {
        List<Holdout> holdouts = Arrays.asList(localSingleRule);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        // Global holdouts should be empty
        assertTrue(config.getGlobalHoldouts().isEmpty());

        // rule1 should have the local holdout
        List<Holdout> rule1Holdouts = config.getHoldoutsForRule("rule1");
        assertEquals(1, rule1Holdouts.size());
        assertTrue(rule1Holdouts.contains(localSingleRule));

        // Other rules should not have this holdout
        assertTrue(config.getHoldoutsForRule("rule2").isEmpty());
        assertTrue(config.getHoldoutsForRule("rule3").isEmpty());
    }

    @Test
    public void testLocalHoldoutMultipleRulesSameFlag() {
        List<Holdout> holdouts = Arrays.asList(localMultipleRulesSameFlag);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        // Global holdouts should be empty
        assertTrue(config.getGlobalHoldouts().isEmpty());

        // rule1, rule2, rule3 should all have the local holdout
        List<Holdout> rule1Holdouts = config.getHoldoutsForRule("rule1");
        assertEquals(1, rule1Holdouts.size());
        assertTrue(rule1Holdouts.contains(localMultipleRulesSameFlag));

        List<Holdout> rule2Holdouts = config.getHoldoutsForRule("rule2");
        assertEquals(1, rule2Holdouts.size());
        assertTrue(rule2Holdouts.contains(localMultipleRulesSameFlag));

        List<Holdout> rule3Holdouts = config.getHoldoutsForRule("rule3");
        assertEquals(1, rule3Holdouts.size());
        assertTrue(rule3Holdouts.contains(localMultipleRulesSameFlag));

        // Other rules should not have this holdout
        assertTrue(config.getHoldoutsForRule("rule4").isEmpty());
    }

    @Test
    public void testLocalHoldoutCrossFlagTargeting() {
        List<Holdout> holdouts = Arrays.asList(localCrossFlagRules);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        // Global holdouts should be empty
        assertTrue(config.getGlobalHoldouts().isEmpty());

        // rule1, rule4, rule5 should all have the local holdout
        List<Holdout> rule1Holdouts = config.getHoldoutsForRule("rule1");
        assertEquals(1, rule1Holdouts.size());
        assertTrue(rule1Holdouts.contains(localCrossFlagRules));

        List<Holdout> rule4Holdouts = config.getHoldoutsForRule("rule4");
        assertEquals(1, rule4Holdouts.size());
        assertTrue(rule4Holdouts.contains(localCrossFlagRules));

        List<Holdout> rule5Holdouts = config.getHoldoutsForRule("rule5");
        assertEquals(1, rule5Holdouts.size());
        assertTrue(rule5Holdouts.contains(localCrossFlagRules));

        // Other rules should not have this holdout
        assertTrue(config.getHoldoutsForRule("rule2").isEmpty());
        assertTrue(config.getHoldoutsForRule("rule3").isEmpty());
    }

    @Test
    public void testMixedGlobalAndLocalHoldouts() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout1, localSingleRule, globalHoldout2, localCrossFlagRules);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        // Check global holdouts
        List<Holdout> globals = config.getGlobalHoldouts();
        assertEquals(2, globals.size());
        assertTrue(globals.contains(globalHoldout1));
        assertTrue(globals.contains(globalHoldout2));

        // rule1 should have two local holdouts (from localSingleRule and localCrossFlagRules)
        List<Holdout> rule1Holdouts = config.getHoldoutsForRule("rule1");
        assertEquals(2, rule1Holdouts.size());
        assertTrue(rule1Holdouts.contains(localSingleRule));
        assertTrue(rule1Holdouts.contains(localCrossFlagRules));

        // rule4 should have one local holdout (from localCrossFlagRules)
        List<Holdout> rule4Holdouts = config.getHoldoutsForRule("rule4");
        assertEquals(1, rule4Holdouts.size());
        assertTrue(rule4Holdouts.contains(localCrossFlagRules));

        // rule2 should have no local holdouts
        assertTrue(config.getHoldoutsForRule("rule2").isEmpty());
    }

    @Test
    public void testEmptyRulesListVsNull() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout1, emptyLocalHoldout);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        // globalHoldout1 should be in global list (includedRules == null)
        List<Holdout> globals = config.getGlobalHoldouts();
        assertEquals(1, globals.size());
        assertTrue(globals.contains(globalHoldout1));
        assertFalse(globals.contains(emptyLocalHoldout));

        // emptyLocalHoldout should NOT be in global list (includedRules == empty list, not null)
        // No rules should have emptyLocalHoldout since its list is empty
        assertTrue(config.getHoldoutsForRule("rule1").isEmpty());
        assertTrue(config.getHoldoutsForRule("rule2").isEmpty());
    }

    @Test
    public void testHoldoutIsGlobalMethod() {
        assertTrue(globalHoldout1.isGlobal());
        assertTrue(globalHoldout2.isGlobal());
        assertFalse(localSingleRule.isGlobal());
        assertFalse(localMultipleRulesSameFlag.isGlobal());
        assertFalse(localCrossFlagRules.isGlobal());
        assertFalse(emptyLocalHoldout.isGlobal());
    }

    @Test
    public void testGetHoldoutsForRuleReturnsUnmodifiableList() {
        List<Holdout> holdouts = Arrays.asList(localSingleRule);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        List<Holdout> rule1Holdouts = config.getHoldoutsForRule("rule1");

        try {
            rule1Holdouts.add(localMultipleRulesSameFlag);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testGetGlobalHoldoutsReturnsUnmodifiableList() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout1);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        List<Holdout> globals = config.getGlobalHoldouts();

        try {
            globals.add(globalHoldout2);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testNonExistentRuleReturnsEmptyList() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout1, localSingleRule);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        List<Holdout> nonExistentRuleHoldouts = config.getHoldoutsForRule("nonexistent_rule_id");
        assertTrue(nonExistentRuleHoldouts.isEmpty());
    }

    @Test
    public void testMultipleLocalHoldoutsTargetingSameRule() {
        List<Holdout> holdouts = Arrays.asList(localSingleRule, localMultipleRulesSameFlag, localCrossFlagRules);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        // rule1 is targeted by all three local holdouts
        List<Holdout> rule1Holdouts = config.getHoldoutsForRule("rule1");
        assertEquals(3, rule1Holdouts.size());
        assertTrue(rule1Holdouts.contains(localSingleRule));
        assertTrue(rule1Holdouts.contains(localMultipleRulesSameFlag));
        assertTrue(rule1Holdouts.contains(localCrossFlagRules));
    }

    @Test
    public void testPrecedenceGlobalBeforeLocal() {
        // This test verifies the data structure setup
        // The actual precedence in decision flow is handled by DecisionService
        List<Holdout> holdouts = Arrays.asList(globalHoldout1, localSingleRule);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        // Global holdouts are separate from local holdouts
        List<Holdout> globals = config.getGlobalHoldouts();
        assertEquals(1, globals.size());
        assertTrue(globals.contains(globalHoldout1));

        List<Holdout> rule1Holdouts = config.getHoldoutsForRule("rule1");
        assertEquals(1, rule1Holdouts.size());
        assertTrue(rule1Holdouts.contains(localSingleRule));
        assertFalse(rule1Holdouts.contains(globalHoldout1)); // Global not in rule-specific list
    }
}

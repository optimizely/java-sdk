/**
 *
 *    Copyright 2016-2019, 2021, 2025, Optimizely and contributors
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

public class HoldoutConfigTest {

    private Holdout holdout1;
    private Holdout holdout2;
    private Holdout holdout3;

    @Before
    public void setUp() {
        // Global holdouts (no includedRules)
        holdout1 = new Holdout("holdout1", "first_holdout");
        holdout2 = new Holdout("holdout2", "second_holdout");
        holdout3 = new Holdout("holdout3", "third_holdout");
    }

    @Test
    public void testEmptyConstructor() {
        HoldoutConfig config = new HoldoutConfig();

        assertTrue(config.getAllHoldouts().isEmpty());
        assertTrue(config.getHoldoutForFlag("any_flag").isEmpty());
        assertNull(config.getHoldout("any_id"));
    }

    @Test
    public void testConstructorWithEmptyList() {
        HoldoutConfig config = new HoldoutConfig(Collections.emptyList());

        assertTrue(config.getAllHoldouts().isEmpty());
        assertTrue(config.getHoldoutForFlag("any_flag").isEmpty());
        assertNull(config.getHoldout("any_id"));
    }

    @Test
    public void testConstructorWithHoldouts() {
        List<Holdout> holdouts = Arrays.asList(holdout1, holdout2);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        assertEquals(2, config.getAllHoldouts().size());
        assertTrue(config.getAllHoldouts().contains(holdout1));
    }

    @Test
    public void testGetHoldout() {
        List<Holdout> holdouts = Arrays.asList(holdout1, holdout2);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        assertEquals(holdout1, config.getHoldout("holdout1"));
        assertEquals(holdout2, config.getHoldout("holdout2"));
        assertNull(config.getHoldout("nonexistent"));
    }

    @Test
    public void testGetHoldoutForFlagReturnsAllGlobalHoldouts() {
        List<Holdout> holdouts = Arrays.asList(holdout1, holdout2, holdout3);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        // All holdouts are global and apply to all flags
        List<Holdout> flag1Holdouts = config.getHoldoutForFlag("flag1");
        assertEquals(3, flag1Holdouts.size());
        assertTrue(flag1Holdouts.contains(holdout1));
        assertTrue(flag1Holdouts.contains(holdout2));
        assertTrue(flag1Holdouts.contains(holdout3));

        List<Holdout> flag2Holdouts = config.getHoldoutForFlag("flag2");
        assertEquals(3, flag2Holdouts.size());

        // Any flag should return all global holdouts
        List<Holdout> anyFlagHoldouts = config.getHoldoutForFlag("any_flag");
        assertEquals(3, anyFlagHoldouts.size());
    }

    @Test
    public void testGetAllHoldoutsIsUnmodifiable() {
        List<Holdout> holdouts = Arrays.asList(holdout1, holdout2);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        List<Holdout> allHoldouts = config.getAllHoldouts();

        try {
            allHoldouts.add(holdout3);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testEmptyFlagHoldouts() {
        HoldoutConfig config = new HoldoutConfig();

        List<Holdout> flagHoldouts = config.getHoldoutForFlag("any_flag");
        assertTrue(flagHoldouts.isEmpty());
    }

    // --- Local holdout tests ---

    @Test
    public void testGetGlobalHoldoutsReturnsOnlyGlobalHoldouts() {
        // Global holdout has null includedRules
        Holdout globalHoldout = new Holdout("global_1", "global_holdout",
            "Running",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            null); // null = global

        // Local holdout has non-null includedRules
        Holdout localHoldout = new Holdout("local_1", "local_holdout",
            "Running",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            Arrays.asList("rule_123")); // non-null = local

        HoldoutConfig config = new HoldoutConfig(Arrays.asList(globalHoldout, localHoldout));

        List<Holdout> globalHoldouts = config.getGlobalHoldouts();
        assertEquals(1, globalHoldouts.size());
        assertEquals("global_1", globalHoldouts.get(0).getId());
    }

    @Test
    public void testGetHoldoutsForRuleReturnsLocalHoldoutsForRule() {
        Holdout localHoldout = new Holdout("local_1", "local_holdout",
            "Running",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            Arrays.asList("rule_123", "rule_456"));

        HoldoutConfig config = new HoldoutConfig(Arrays.asList(localHoldout));

        List<Holdout> rule123Holdouts = config.getHoldoutsForRule("rule_123");
        assertEquals(1, rule123Holdouts.size());
        assertEquals("local_1", rule123Holdouts.get(0).getId());

        List<Holdout> rule456Holdouts = config.getHoldoutsForRule("rule_456");
        assertEquals(1, rule456Holdouts.size());

        // Other rules should return empty
        List<Holdout> otherRuleHoldouts = config.getHoldoutsForRule("rule_999");
        assertTrue(otherRuleHoldouts.isEmpty());
    }

    @Test
    public void testLocalHoldoutDoesNotAppearInGlobalHoldouts() {
        Holdout localHoldout = new Holdout("local_1", "local_holdout",
            "Running",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            Arrays.asList("rule_123"));

        HoldoutConfig config = new HoldoutConfig(Arrays.asList(localHoldout));

        // Local holdout should not appear in global holdouts
        assertTrue(config.getGlobalHoldouts().isEmpty());

        // Local holdout should appear via getHoldoutsForRule
        List<Holdout> ruleHoldouts = config.getHoldoutsForRule("rule_123");
        assertEquals(1, ruleHoldouts.size());
    }

    @Test
    public void testGlobalHoldoutDoesNotAppearInRuleHoldouts() {
        Holdout globalHoldout = new Holdout("global_1", "global_holdout",
            "Running",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            null); // null = global

        HoldoutConfig config = new HoldoutConfig(Arrays.asList(globalHoldout));

        // Global holdout should not appear in rule holdouts
        assertTrue(config.getHoldoutsForRule("rule_123").isEmpty());

        // Global holdout should appear in getGlobalHoldouts
        assertEquals(1, config.getGlobalHoldouts().size());
    }

    @Test
    public void testEmptyIncludedRulesIsLocalNotGlobal() {
        // Empty list (not null) means local holdout with no rules
        Holdout emptyRulesHoldout = new Holdout("empty_rules", "empty_rules_holdout",
            "Running",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()); // empty list = local holdout with no rules

        HoldoutConfig config = new HoldoutConfig(Arrays.asList(emptyRulesHoldout));

        // Empty-rules holdout is NOT global
        assertTrue(config.getGlobalHoldouts().isEmpty());

        // Empty-rules holdout doesn't match any rule
        assertTrue(config.getHoldoutsForRule("any_rule").isEmpty());
    }

    @Test
    public void testMixedGlobalAndLocalHoldouts() {
        Holdout globalHoldout = new Holdout("global_h", "global",
            "Running",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            null);

        Holdout localHoldout = new Holdout("local_h", "local",
            "Running",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            Arrays.asList("rule_abc"));

        HoldoutConfig config = new HoldoutConfig(Arrays.asList(globalHoldout, localHoldout));

        // Global holdouts
        List<Holdout> globals = config.getGlobalHoldouts();
        assertEquals(1, globals.size());
        assertEquals("global_h", globals.get(0).getId());

        // Local holdouts by rule
        List<Holdout> ruleHoldouts = config.getHoldoutsForRule("rule_abc");
        assertEquals(1, ruleHoldouts.size());
        assertEquals("local_h", ruleHoldouts.get(0).getId());

        // Deprecated getHoldoutForFlag returns only global
        List<Holdout> flagHoldouts = config.getHoldoutForFlag("any_flag");
        assertEquals(1, flagHoldouts.size());
        assertEquals("global_h", flagHoldouts.get(0).getId());
    }

    @Test
    public void testBackwardCompatibilityOldDatafileNullIncludedRules() {
        // Old datafile holdout — no includedRules field, defaults to null = global
        Holdout oldHoldout = new Holdout("old_holdout", "old");
        assertTrue(oldHoldout.isGlobal());
        assertNull(oldHoldout.getIncludedRules());

        HoldoutConfig config = new HoldoutConfig(Arrays.asList(oldHoldout));

        // Should behave as global holdout
        assertEquals(1, config.getGlobalHoldouts().size());
        assertTrue(config.getHoldoutsForRule("any_rule").isEmpty());
    }

    @Test
    public void testIsGlobalReturnsTrueWhenIncludedRulesIsNull() {
        Holdout globalHoldout = new Holdout("g1", "global",
            "Running",
            Collections.emptyList(), null,
            Collections.emptyList(), Collections.emptyList(),
            null);
        assertTrue(globalHoldout.isGlobal());
        assertNull(globalHoldout.getIncludedRules());
    }

    @Test
    public void testIsGlobalReturnsFalseWhenIncludedRulesIsNonNull() {
        Holdout localHoldout = new Holdout("l1", "local",
            "Running",
            Collections.emptyList(), null,
            Collections.emptyList(), Collections.emptyList(),
            Arrays.asList("rule_1"));
        assertFalse(localHoldout.isGlobal());
        assertEquals(Arrays.asList("rule_1"), localHoldout.getIncludedRules());
    }

    @Test
    public void testGetHoldoutsForRuleReturnsEmptyForUnknownRule() {
        HoldoutConfig config = new HoldoutConfig(Arrays.asList(holdout1, holdout2));
        assertTrue(config.getHoldoutsForRule("unknown_rule").isEmpty());
    }

    @Test
    public void testMultipleLocalHoldoutsForSameRule() {
        Holdout local1 = new Holdout("local_1", "local_holdout_1",
            "Running",
            Collections.emptyList(), null,
            Collections.emptyList(), Collections.emptyList(),
            Arrays.asList("rule_abc"));

        Holdout local2 = new Holdout("local_2", "local_holdout_2",
            "Running",
            Collections.emptyList(), null,
            Collections.emptyList(), Collections.emptyList(),
            Arrays.asList("rule_abc", "rule_xyz"));

        HoldoutConfig config = new HoldoutConfig(Arrays.asList(local1, local2));

        List<Holdout> ruleAbcHoldouts = config.getHoldoutsForRule("rule_abc");
        assertEquals(2, ruleAbcHoldouts.size());

        List<Holdout> ruleXyzHoldouts = config.getHoldoutsForRule("rule_xyz");
        assertEquals(1, ruleXyzHoldouts.size());
        assertEquals("local_2", ruleXyzHoldouts.get(0).getId());
    }
}

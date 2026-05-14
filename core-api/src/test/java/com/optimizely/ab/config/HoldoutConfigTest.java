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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

public class HoldoutConfigTest {

    private Holdout holdout1;
    private Holdout holdout2;
    private Holdout holdout3;

    /** Local holdout that targets rule "ruleA" only. */
    private Holdout localHoldoutRuleA;
    /** Local holdout that targets both "ruleA" and "ruleB". */
    private Holdout localHoldoutRuleAAndB;
    /** Local holdout with an empty includedRules list — targets no rule. */
    private Holdout localHoldoutEmptyRules;

    @Before
    public void setUp() {
        // Global holdouts — includedRules is null
        holdout1 = new Holdout("holdout1", "first_holdout");
        holdout2 = new Holdout("holdout2", "second_holdout");
        holdout3 = new Holdout("holdout3", "third_holdout");

        // Local holdouts — includedRules is non-null
        localHoldoutRuleA = new Holdout(
            "local1", "local_ruleA",
            "Running",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.singletonList("ruleA")
        );

        localHoldoutRuleAAndB = new Holdout(
            "local2", "local_ruleAB",
            "Running",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            Arrays.asList("ruleA", "ruleB")
        );

        localHoldoutEmptyRules = new Holdout(
            "local3", "local_empty",
            "Running",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()  // empty list — not global, but targets no rule
        );
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
    public void testGetHoldoutForFlagReturnsAllHoldouts() {
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
        assertTrue(flag2Holdouts.contains(holdout1));
        assertTrue(flag2Holdouts.contains(holdout2));
        assertTrue(flag2Holdouts.contains(holdout3));

        // Any flag should return all holdouts
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

    // ========= Local Holdouts — FSSDK-12369 =========

    /**
     * isGlobal() returns true when includedRules is null (field absent in datafile).
     */
    @Test
    public void testIsGlobalReturnsTrueWhenIncludedRulesIsNull() {
        // holdout1 was created with the 2-arg convenience constructor → includedRules == null
        assertTrue(holdout1.isGlobal());
        assertNull(holdout1.getIncludedRules());
    }

    /**
     * isGlobal() returns false when includedRules is a non-null list (even if empty).
     */
    @Test
    public void testIsGlobalReturnsFalseWhenIncludedRulesIsNonNull() {
        assertFalse(localHoldoutRuleA.isGlobal());
        assertFalse(localHoldoutEmptyRules.isGlobal());
    }

    /**
     * Empty includedRules list is NOT treated as global.
     */
    @Test
    public void testEmptyIncludedRulesIsNotGlobal() {
        assertFalse(localHoldoutEmptyRules.isGlobal());
        assertNotNull(localHoldoutEmptyRules.getIncludedRules());
        assertTrue(localHoldoutEmptyRules.getIncludedRules().isEmpty());
    }

    /**
     * getGlobalHoldouts() returns only holdouts with includedRules == null.
     */
    @Test
    public void testGetGlobalHoldoutsReturnsOnlyGlobalHoldouts() {
        List<Holdout> holdouts = Arrays.asList(holdout1, localHoldoutRuleA, holdout2, localHoldoutEmptyRules);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        List<Holdout> globals = config.getGlobalHoldouts();
        assertEquals(2, globals.size());
        assertTrue(globals.contains(holdout1));
        assertTrue(globals.contains(holdout2));
        assertFalse(globals.contains(localHoldoutRuleA));
        assertFalse(globals.contains(localHoldoutEmptyRules));
    }

    /**
     * getHoldoutsForRule() returns local holdouts that target a given rule ID.
     */
    @Test
    public void testGetHoldoutsForRuleReturnMatchingLocalHoldouts() {
        List<Holdout> holdouts = Arrays.asList(holdout1, localHoldoutRuleA, localHoldoutRuleAAndB);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        List<Holdout> ruleAHoldouts = config.getHoldoutsForRule("ruleA");
        assertEquals(2, ruleAHoldouts.size());
        assertTrue(ruleAHoldouts.contains(localHoldoutRuleA));
        assertTrue(ruleAHoldouts.contains(localHoldoutRuleAAndB));

        List<Holdout> ruleBHoldouts = config.getHoldoutsForRule("ruleB");
        assertEquals(1, ruleBHoldouts.size());
        assertTrue(ruleBHoldouts.contains(localHoldoutRuleAAndB));
    }

    /**
     * getHoldoutsForRule() returns an empty list for an unknown rule ID.
     */
    @Test
    public void testGetHoldoutsForRuleUnknownRuleReturnsEmpty() {
        HoldoutConfig config = new HoldoutConfig(Arrays.asList(localHoldoutRuleA));
        assertTrue(config.getHoldoutsForRule("unknownRule").isEmpty());
    }

    /**
     * A holdout with an empty includedRules list does not appear in any rule's local holdout list.
     */
    @Test
    public void testEmptyIncludedRulesHoldoutDoesNotMatchAnyRule() {
        HoldoutConfig config = new HoldoutConfig(Arrays.asList(localHoldoutEmptyRules));
        assertTrue(config.getGlobalHoldouts().isEmpty());
        assertTrue(config.getHoldoutsForRule("anyRule").isEmpty());
    }

    /**
     * Backward compatibility: old datafiles without includedRules field → holdout treated as global.
     */
    @Test
    public void testBackwardCompatibilityNoIncludedRulesFieldTreatedAsGlobal() {
        // The 7-arg constructor sets includedRules to null → global
        Holdout legacyHoldout = new Holdout(
            "legacy1", "legacy_holdout",
            "Running",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Collections.emptyList()
        );
        assertTrue(legacyHoldout.isGlobal());

        HoldoutConfig config = new HoldoutConfig(Collections.singletonList(legacyHoldout));
        assertEquals(1, config.getGlobalHoldouts().size());
        assertTrue(config.getGlobalHoldouts().contains(legacyHoldout));
    }

    /**
     * getAllHoldouts() still returns both global and local holdouts.
     */
    @Test
    public void testGetAllHoldoutsContainsBothGlobalAndLocal() {
        List<Holdout> holdouts = Arrays.asList(holdout1, localHoldoutRuleA, localHoldoutEmptyRules);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        assertEquals(3, config.getAllHoldouts().size());
    }

    private static void assertNotNull(Object obj) {
        assertTrue("Expected non-null value", obj != null);
    }
}
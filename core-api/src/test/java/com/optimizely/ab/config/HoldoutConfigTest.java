/**
 *
 *    Copyright 2016-2019, 2021, 2026, Optimizely and contributors
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

@SuppressWarnings("deprecation")
public class HoldoutConfigTest {

    private Holdout globalHoldout1;
    private Holdout globalHoldout2;
    private Holdout localHoldoutRuleA;
    private Holdout localHoldoutRuleB;
    private Holdout localHoldoutEmpty;

    @Before
    public void setUp() {
        // Global holdouts — includedRules == null
        globalHoldout1 = new Holdout("holdout1", "first_holdout");
        globalHoldout2 = new Holdout("holdout2", "second_holdout");

        // Local holdout targeting rule "ruleA"
        localHoldoutRuleA = new Holdout(
            "local_holdout_a", "local_a",
            "Running",
            Collections.<String>emptyList(),
            null,
            Collections.<Variation>emptyList(),
            Collections.<TrafficAllocation>emptyList(),
            Arrays.asList("ruleA"),
            false
        );

        // Local holdout targeting rules "ruleA" and "ruleB"
        localHoldoutRuleB = new Holdout(
            "local_holdout_b", "local_b",
            "Running",
            Collections.<String>emptyList(),
            null,
            Collections.<Variation>emptyList(),
            Collections.<TrafficAllocation>emptyList(),
            Arrays.asList("ruleA", "ruleB"),
            false
        );

        // Local holdout with empty includedRules list — targets no rules
        localHoldoutEmpty = new Holdout(
            "local_holdout_empty", "local_empty",
            "Running",
            Collections.<String>emptyList(),
            null,
            Collections.<Variation>emptyList(),
            Collections.<TrafficAllocation>emptyList(),
            Collections.<String>emptyList(),
            false
        );
    }

    // -----------------------------------------------------------------------
    // isGlobal classification
    // -----------------------------------------------------------------------

    @Test
    public void testIsGlobalReturnsTrueWhenIncludedRulesIsNull() {
        assertTrue("Holdout with null includedRules must be global", globalHoldout1.isGlobal());
        assertTrue("Holdout with null includedRules must be global", globalHoldout2.isGlobal());
    }

    @Test
    public void testIsGlobalReturnsFalseWhenIncludedRulesIsNonNull() {
        assertFalse("Holdout with non-null includedRules must be local", localHoldoutRuleA.isGlobal());
        assertFalse("Holdout with non-null includedRules must be local", localHoldoutRuleB.isGlobal());
    }

    @Test
    public void testEmptyIncludedRulesIsLocalNotGlobal() {
        // Empty list is still a local holdout — nil vs empty list are different
        assertFalse("Holdout with empty includedRules list must be local, not global", localHoldoutEmpty.isGlobal());
        assertNotNull("Empty list should be returned, not null", localHoldoutEmpty.getIncludedRules());
        assertTrue("Empty includedRules list should be empty", localHoldoutEmpty.getIncludedRules().isEmpty());
    }

    // -----------------------------------------------------------------------
    // getGlobalHoldouts
    // -----------------------------------------------------------------------

    @Test
    public void testGetGlobalHoldoutsReturnsOnlyGlobalHoldouts() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout1, localHoldoutRuleA, globalHoldout2, localHoldoutRuleB);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        List<Holdout> globals = config.getGlobalHoldouts();
        assertEquals(2, globals.size());
        assertTrue(globals.contains(globalHoldout1));
        assertTrue(globals.contains(globalHoldout2));
        assertFalse(globals.contains(localHoldoutRuleA));
        assertFalse(globals.contains(localHoldoutRuleB));
    }

    @Test
    public void testGetGlobalHoldoutsIsEmptyWhenNoGlobalHoldouts() {
        HoldoutConfig config = new HoldoutConfig(Arrays.asList(localHoldoutRuleA));
        assertTrue(config.getGlobalHoldouts().isEmpty());
    }

    @Test
    public void testGetGlobalHoldoutsIsUnmodifiable() {
        HoldoutConfig config = new HoldoutConfig(Arrays.asList(globalHoldout1));
        try {
            config.getGlobalHoldouts().add(globalHoldout2);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    // -----------------------------------------------------------------------
    // getHoldoutsForRule
    // -----------------------------------------------------------------------

    @Test
    public void testGetHoldoutsForRuleReturnsMatchingLocalHoldouts() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout1, localHoldoutRuleA, localHoldoutRuleB);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        // ruleA is targeted by both localHoldoutRuleA and localHoldoutRuleB
        List<Holdout> forRuleA = config.getHoldoutsForRule("ruleA");
        assertEquals(2, forRuleA.size());
        assertTrue(forRuleA.contains(localHoldoutRuleA));
        assertTrue(forRuleA.contains(localHoldoutRuleB));

        // ruleB is targeted only by localHoldoutRuleB
        List<Holdout> forRuleB = config.getHoldoutsForRule("ruleB");
        assertEquals(1, forRuleB.size());
        assertTrue(forRuleB.contains(localHoldoutRuleB));
    }

    @Test
    public void testGetHoldoutsForRuleReturnsEmptyListForUnknownRule() {
        HoldoutConfig config = new HoldoutConfig(Arrays.asList(localHoldoutRuleA));
        assertTrue("Unknown rule should return empty list", config.getHoldoutsForRule("unknownRule").isEmpty());
    }

    @Test
    public void testGetHoldoutsForRuleDoesNotReturnGlobalHoldouts() {
        // Global holdouts must NOT appear in getHoldoutsForRule — only local ones do
        HoldoutConfig config = new HoldoutConfig(Arrays.asList(globalHoldout1, localHoldoutRuleA));

        List<Holdout> forRuleA = config.getHoldoutsForRule("ruleA");
        assertFalse("Global holdouts must not appear in getHoldoutsForRule", forRuleA.contains(globalHoldout1));
    }

    @Test
    public void testEmptyIncludedRulesHoldoutDoesNotMatchAnyRule() {
        // A local holdout with empty includedRules targets no rules
        HoldoutConfig config = new HoldoutConfig(Arrays.asList(localHoldoutEmpty));
        assertTrue(config.getHoldoutsForRule("ruleA").isEmpty());
        assertTrue(config.getHoldoutsForRule("ruleB").isEmpty());
    }

    @Test
    public void testGetHoldoutsForRuleIsUnmodifiable() {
        HoldoutConfig config = new HoldoutConfig(Arrays.asList(localHoldoutRuleA));
        try {
            config.getHoldoutsForRule("ruleA").add(globalHoldout1);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    // -----------------------------------------------------------------------
    // Backward compatibility: getHoldoutForFlag (deprecated)
    // -----------------------------------------------------------------------

    @Test
    @SuppressWarnings("deprecation")
    public void testGetHoldoutForFlagReturnsOnlyGlobalHoldoutsForBackwardCompatibility() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout1, localHoldoutRuleA, globalHoldout2);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        // The deprecated getHoldoutForFlag should return only global holdouts (not local ones)
        List<Holdout> result = config.getHoldoutForFlag("any_flag");
        assertEquals(2, result.size());
        assertTrue(result.contains(globalHoldout1));
        assertTrue(result.contains(globalHoldout2));
        assertFalse(result.contains(localHoldoutRuleA));
    }

    // -----------------------------------------------------------------------
    // General functionality
    // -----------------------------------------------------------------------

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
        HoldoutConfig config = new HoldoutConfig(Collections.<Holdout>emptyList());

        assertTrue(config.getAllHoldouts().isEmpty());
        assertTrue(config.getGlobalHoldouts().isEmpty());
        assertTrue(config.getHoldoutsForRule("any_rule").isEmpty());
        assertNull(config.getHoldout("any_id"));
    }

    @Test
    public void testGetHoldout() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout1, localHoldoutRuleA);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        assertEquals(globalHoldout1, config.getHoldout("holdout1"));
        assertEquals(localHoldoutRuleA, config.getHoldout("local_holdout_a"));
        assertNull(config.getHoldout("nonexistent"));
    }

    @Test
    public void testGetAllHoldoutsIncludesBothGlobalAndLocal() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout1, localHoldoutRuleA);
        HoldoutConfig config = new HoldoutConfig(holdouts);

        assertEquals(2, config.getAllHoldouts().size());
        assertTrue(config.getAllHoldouts().contains(globalHoldout1));
        assertTrue(config.getAllHoldouts().contains(localHoldoutRuleA));
    }

    @Test
    public void testGetAllHoldoutsIsUnmodifiable() {
        HoldoutConfig config = new HoldoutConfig(Arrays.asList(globalHoldout1));
        try {
            config.getAllHoldouts().add(globalHoldout2);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    // -----------------------------------------------------------------------
    // Section-aware constructor: localHoldouts datafile section
    // -----------------------------------------------------------------------

    @Test
    public void testSectionConstructorPartitionsByDatafileSection() {
        // Entries are routed by section membership, not by includedRules.
        HoldoutConfig config = new HoldoutConfig(
            Arrays.asList(globalHoldout1, globalHoldout2),
            Arrays.asList(localHoldoutRuleA, localHoldoutRuleB)
        );

        List<Holdout> globals = config.getGlobalHoldouts();
        assertEquals(2, globals.size());
        assertTrue(globals.contains(globalHoldout1));
        assertTrue(globals.contains(globalHoldout2));

        // Local entries are registered under their includedRules.
        List<Holdout> forRuleA = config.getHoldoutsForRule("ruleA");
        assertEquals(2, forRuleA.size());
        assertTrue(forRuleA.contains(localHoldoutRuleA));
        assertTrue(forRuleA.contains(localHoldoutRuleB));
    }

    @Test
    public void testGlobalSectionEntriesIgnoreIncludedRules() {
        // A holdout placed in the global 'holdouts' section must be treated as global
        // and have its includedRules stripped — even if the datafile incorrectly carries one.
        Holdout strayGlobalWithRules = new Holdout(
            "stray", "stray_holdout",
            "Running",
            Collections.<String>emptyList(),
            null,
            Collections.<Variation>emptyList(),
            Collections.<TrafficAllocation>emptyList(),
            Arrays.asList("ruleA"),
            false
        );

        HoldoutConfig config = new HoldoutConfig(
            Arrays.asList(strayGlobalWithRules),
            Collections.<Holdout>emptyList()
        );

        // The entity-level isGlobal must report true after stripping
        List<Holdout> globals = config.getGlobalHoldouts();
        assertEquals(1, globals.size());
        Holdout sanitized = globals.get(0);
        assertEquals("stray", sanitized.getId());
        assertTrue("includedRules should be stripped on global-section entries", sanitized.isGlobal());
        assertNull(sanitized.getIncludedRules());

        // And the stray rule ID must NOT be registered as a local rule
        assertTrue(config.getHoldoutsForRule("ruleA").isEmpty());

        // holdoutIdMap returns the sanitized copy (verifies that callers see the stripped form)
        Holdout viaIdMap = config.getHoldout("stray");
        assertNotNull("Stray global must be retrievable by ID", viaIdMap);
        assertTrue(viaIdMap.isGlobal());
        assertNull(viaIdMap.getIncludedRules());
    }

    @Test
    public void testLocalSectionEntryWithoutIncludedRulesIsExcluded() {
        // Entry placed in the localHoldouts section but missing includedRules is invalid.
        Holdout invalidLocal = new Holdout(
            "invalid", "invalid_local",
            "Running",
            Collections.<String>emptyList(),
            null,
            Collections.<Variation>emptyList(),
            Collections.<TrafficAllocation>emptyList(),
            null,  // missing includedRules
            false
        );

        HoldoutConfig config = new HoldoutConfig(
            Collections.<Holdout>emptyList(),
            Arrays.asList(invalidLocal)
        );

        // Excluded from every map: must NOT fall back to global application
        assertTrue("Invalid local must not be promoted to global", config.getGlobalHoldouts().isEmpty());
        assertTrue("Invalid local must not be registered for any rule",
            config.getHoldoutsForRule("ruleA").isEmpty());
        assertNull("Invalid local must not be retrievable by ID", config.getHoldout("invalid"));
    }

    @Test
    public void testLocalSectionEntryWithEmptyIncludedRulesIsValidButInert() {
        // includedRules == [] is valid but inert: the entity is tracked in the id map
        // (no error logged) but is not registered under any rule. Matches the spec's
        // entity-level semantics where [] != null.
        HoldoutConfig config = new HoldoutConfig(
            Collections.<Holdout>emptyList(),
            Arrays.asList(localHoldoutEmpty)
        );

        // Not promoted to global
        assertTrue(config.getGlobalHoldouts().isEmpty());
        // Not registered under any rule (no rule IDs to target)
        assertTrue(config.getHoldoutsForRule("ruleA").isEmpty());
        // But the entity is tracked in the id map
        Holdout stored = config.getHoldout("local_holdout_empty");
        assertNotNull("Empty-includedRules local must still be retrievable by ID", stored);
        assertFalse("Empty-includedRules local must not report isGlobal", stored.isGlobal());
    }

    @Test
    public void testBothSectionsPresentPartitionCorrectly() {
        // When both sections are non-empty, scope is enforced strictly by section.
        HoldoutConfig config = new HoldoutConfig(
            Arrays.asList(globalHoldout1),
            Arrays.asList(localHoldoutRuleA)
        );

        List<Holdout> globals = config.getGlobalHoldouts();
        assertEquals(1, globals.size());
        assertSame(globalHoldout1, globals.get(0));

        List<Holdout> forRuleA = config.getHoldoutsForRule("ruleA");
        assertEquals(1, forRuleA.size());
        assertSame(localHoldoutRuleA, forRuleA.get(0));

        // ID map covers both
        assertNotNull("Global must be in id map", config.getHoldout("holdout1"));
        assertNotNull("Local must be in id map", config.getHoldout("local_holdout_a"));
    }

    @Test
    public void testBackwardCompatNoLocalSection() {
        // Older datafiles emit only the 'holdouts' section. Passing an empty list for
        // localHoldoutsSection must match the legacy behavior exactly.
        HoldoutConfig config = new HoldoutConfig(
            Arrays.asList(globalHoldout1, globalHoldout2),
            Collections.<Holdout>emptyList()
        );

        assertEquals(2, config.getGlobalHoldouts().size());
        assertTrue(config.getHoldoutsForRule("any_rule").isEmpty());
        assertEquals(2, config.getAllHoldouts().size());
    }

    @Test
    public void testEmptyBothSections() {
        HoldoutConfig config = new HoldoutConfig(
            Collections.<Holdout>emptyList(),
            Collections.<Holdout>emptyList()
        );

        assertTrue(config.getAllHoldouts().isEmpty());
        assertTrue(config.getGlobalHoldouts().isEmpty());
        assertTrue(config.getHoldoutsForRule("any_rule").isEmpty());
        assertNull(config.getHoldout("any_id"));
    }

    @Test
    public void testSectionConstructorIdMapTracksBothSections() {
        HoldoutConfig config = new HoldoutConfig(
            Arrays.asList(globalHoldout1),
            Arrays.asList(localHoldoutRuleA)
        );

        assertSame(globalHoldout1, config.getHoldout("holdout1"));
        assertSame(localHoldoutRuleA, config.getHoldout("local_holdout_a"));
    }

    // Helper for assertNotNull/assertNull (avoids import of static from junit 4.x)
    private static void assertNotNull(String message, Object obj) {
        assertTrue(message, obj != null);
    }

    private static void assertNotNull(Object obj) {
        assertTrue("Expected non-null", obj != null);
    }
}

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

    private Holdout globalHoldout;
    private Holdout includedHoldout;
    private Holdout excludedHoldout;
    private Holdout mixedHoldout;

    @Before
    public void setUp() {
        // Global holdout (no included/excluded flags)
        globalHoldout = new Holdout("global1", "global_holdout");

        // Holdout with included flags
        includedHoldout = new Holdout("included1", "included_holdout", null,
                Collections.emptyList(), null, Collections.emptyList(),
                Collections.emptyList(), Arrays.asList("flag1", "flag2"), null, "");

        // Global holdout with excluded flags
        excludedHoldout = new Holdout("excluded1", "excluded_holdout", null,
                Collections.emptyList(), null, Collections.emptyList(),
                Collections.emptyList(), null, Arrays.asList("flag3"), "");

        // Another global holdout for testing
        mixedHoldout = new Holdout("mixed1", "mixed_holdout");
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
    public void testConstructorWithGlobalHoldouts() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout, mixedHoldout);
        HoldoutConfig config = new HoldoutConfig(holdouts);
        
        assertEquals(2, config.getAllHoldouts().size());
        assertTrue(config.getAllHoldouts().contains(globalHoldout));
    }

    @Test
    public void testGetHoldout() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout, includedHoldout);
        HoldoutConfig config = new HoldoutConfig(holdouts);
        
        assertEquals(globalHoldout, config.getHoldout("global1"));
        assertEquals(includedHoldout, config.getHoldout("included1"));
        assertNull(config.getHoldout("nonexistent"));
    }

    @Test
    public void testGetHoldoutForFlagWithGlobalHoldouts() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout, mixedHoldout);
        HoldoutConfig config = new HoldoutConfig(holdouts);
        
        List<Holdout> flagHoldouts = config.getHoldoutForFlag("any_flag");
        assertEquals(2, flagHoldouts.size());
        assertTrue(flagHoldouts.contains(globalHoldout));
        assertTrue(flagHoldouts.contains(mixedHoldout));
    }

    @Test
    public void testGetHoldoutForFlagWithIncludedHoldouts() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout, includedHoldout);
        HoldoutConfig config = new HoldoutConfig(holdouts);
        
        // Flag included in holdout
        List<Holdout> flag1Holdouts = config.getHoldoutForFlag("flag1");
        assertEquals(2, flag1Holdouts.size());
        assertTrue(flag1Holdouts.contains(globalHoldout)); // Global first
        assertTrue(flag1Holdouts.contains(includedHoldout)); // Included second
        
        List<Holdout> flag2Holdouts = config.getHoldoutForFlag("flag2");
        assertEquals(2, flag2Holdouts.size());
        assertTrue(flag2Holdouts.contains(globalHoldout));
        assertTrue(flag2Holdouts.contains(includedHoldout));
        
        // Flag not included in holdout
        List<Holdout> flag3Holdouts = config.getHoldoutForFlag("flag3");
        assertEquals(1, flag3Holdouts.size());
        assertTrue(flag3Holdouts.contains(globalHoldout)); // Only global
    }

    @Test
    public void testGetHoldoutForFlagWithExcludedHoldouts() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout, excludedHoldout);
        HoldoutConfig config = new HoldoutConfig(holdouts);
        
        // Flag excluded from holdout
        List<Holdout> flag3Holdouts = config.getHoldoutForFlag("flag3");
        assertEquals(1, flag3Holdouts.size());
        assertTrue(flag3Holdouts.contains(globalHoldout)); // excludedHoldout should be filtered out
        
        // Flag not excluded
        List<Holdout> flag1Holdouts = config.getHoldoutForFlag("flag1");
        assertEquals(2, flag1Holdouts.size());
        assertTrue(flag1Holdouts.contains(globalHoldout));
        assertTrue(flag1Holdouts.contains(excludedHoldout));
    }

    @Test
    public void testGetHoldoutForFlagWithMixedHoldouts() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout, includedHoldout, excludedHoldout);
        HoldoutConfig config = new HoldoutConfig(holdouts);
        
        // flag1 is included in includedHoldout
        List<Holdout> flag1Holdouts = config.getHoldoutForFlag("flag1");
        assertEquals(3, flag1Holdouts.size());
        assertTrue(flag1Holdouts.contains(globalHoldout));
        assertTrue(flag1Holdouts.contains(excludedHoldout));
        assertTrue(flag1Holdouts.contains(includedHoldout));
        
        // flag3 is excluded from excludedHoldout
        List<Holdout> flag3Holdouts = config.getHoldoutForFlag("flag3");
        assertEquals(1, flag3Holdouts.size());
        assertTrue(flag3Holdouts.contains(globalHoldout)); // Only global, excludedHoldout filtered out
        
        // flag4 has no specific inclusion/exclusion
        List<Holdout> flag4Holdouts = config.getHoldoutForFlag("flag4");
        assertEquals(2, flag4Holdouts.size());
        assertTrue(flag4Holdouts.contains(globalHoldout));
        assertTrue(flag4Holdouts.contains(excludedHoldout));
    }

    @Test
    public void testCachingBehavior() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout, includedHoldout);
        HoldoutConfig config = new HoldoutConfig(holdouts);
        
        // First call
        List<Holdout> firstCall = config.getHoldoutForFlag("flag1");
        // Second call should return cached result (same object reference)
        List<Holdout> secondCall = config.getHoldoutForFlag("flag1");
        
        assertSame(firstCall, secondCall);
        assertEquals(2, firstCall.size());
    }

    @Test
    public void testGetAllHoldoutsIsUnmodifiable() {
        List<Holdout> holdouts = Arrays.asList(globalHoldout, includedHoldout);
        HoldoutConfig config = new HoldoutConfig(holdouts);
        
        List<Holdout> allHoldouts = config.getAllHoldouts();
        
        try {
            allHoldouts.add(mixedHoldout);
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
        
        // Should return same empty list for subsequent calls (caching)
        List<Holdout> secondCall = config.getHoldoutForFlag("any_flag");
        assertSame(flagHoldouts, secondCall);
    }

    @Test
    public void testHoldoutWithBothIncludedAndExcluded() {
        // Create a holdout with both included and excluded flags (included takes precedence)
        Holdout bothHoldout = new Holdout("both1", "both_holdout", null,
                Collections.emptyList(), null, Collections.emptyList(),
                Collections.emptyList(), Arrays.asList("flag1"), Arrays.asList("flag2"), "");
        
        List<Holdout> holdouts = Arrays.asList(globalHoldout, bothHoldout);
        HoldoutConfig config = new HoldoutConfig(holdouts);
        
        // flag1 should include bothHoldout (included takes precedence)
        List<Holdout> flag1Holdouts = config.getHoldoutForFlag("flag1");
        assertEquals(2, flag1Holdouts.size());
        assertTrue(flag1Holdouts.contains(globalHoldout));
        assertTrue(flag1Holdouts.contains(bothHoldout));
        
        // flag2 should not include bothHoldout (not in included list)
        List<Holdout> flag2Holdouts = config.getHoldoutForFlag("flag2");
        assertEquals(1, flag2Holdouts.size());
        assertTrue(flag2Holdouts.contains(globalHoldout));
        assertFalse(flag2Holdouts.contains(bothHoldout));
    }

}
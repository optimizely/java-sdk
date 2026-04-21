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

    @Before
    public void setUp() {
        // All holdouts are now global (apply to all flags)
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

}
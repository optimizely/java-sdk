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
package com.optimizely.ab.config.audience.match;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class NumberComparatorTest {

    private static final List<Object> INVALIDS = Collections.unmodifiableList(Arrays.asList(null, "test", "", true));

    @Test
    public void testLessThan() throws UnknownValueTypeException {
        assertTrue(NumberComparator.compare(0,1) < 0);
        assertTrue(NumberComparator.compare(0,1.0) < 0);
        assertTrue(NumberComparator.compare(0,1L) < 0);
    }

    @Test
    public void testGreaterThan() throws UnknownValueTypeException {
        assertTrue(NumberComparator.compare(1,0) > 0);
        assertTrue(NumberComparator.compare(1.0,0) > 0);
        assertTrue(NumberComparator.compare(1L,0) > 0);
    }

    @Test
    public void testEquals() throws UnknownValueTypeException {
        assertEquals(0, NumberComparator.compare(1, 1));
        assertEquals(0, NumberComparator.compare(1, 1.0));
        assertEquals(0, NumberComparator.compare(1L, 1));
    }

    @Test
    public void testInvalidRight() {
        for (Object invalid: INVALIDS) {
            try {
                NumberComparator.compare(0, invalid);
                fail("should have failed for invalid object");
            } catch (UnknownValueTypeException e) {
                // pass
            }
        }
    }

    @Test
    public void testInvalidLeft() {
        for (Object invalid: INVALIDS) {
            try {
                NumberComparator.compare(invalid, 0);
                fail("should have failed for invalid object");
            } catch (UnknownValueTypeException e) {
                // pass
            }
        }
    }
}

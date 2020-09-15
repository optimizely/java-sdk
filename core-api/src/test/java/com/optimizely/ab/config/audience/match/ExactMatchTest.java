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

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ExactMatchTest {

    private ExactMatch match;
    private static final List<Object> INVALIDS = Collections.unmodifiableList(Arrays.asList(new byte[0], new Object(), null));

    @Before
    public void setUp() {
        match = new ExactMatch();
    }

    @Test
    public void testInvalidConditionValues() {
        for (Object invalid : INVALIDS) {
            try {
                match.eval(invalid, "valid");
                fail("should have raised exception");
            } catch (UnexpectedValueTypeException e) {
                //pass
            }
        }
    }

    @Test
    public void testMismatchClasses() throws Exception {
        assertNull(match.eval(false, "false"));
        assertNull(match.eval("false", null));
    }

    @Test
    public void testStringMatch() throws Exception {
        assertEquals(Boolean.TRUE, match.eval("", ""));
        assertEquals(Boolean.TRUE, match.eval("true", "true"));
        assertEquals(Boolean.FALSE, match.eval("true", "false"));
    }

    @Test
    public void testBooleanMatch() throws Exception {
        assertEquals(Boolean.TRUE, match.eval(true, true));
        assertEquals(Boolean.TRUE, match.eval(false, false));
        assertEquals(Boolean.FALSE, match.eval(true, false));
    }

    @Test
    public void testNumberMatch() throws UnexpectedValueTypeException {
        assertEquals(Boolean.TRUE, match.eval(1, 1));
        assertEquals(Boolean.TRUE, match.eval(1L, 1L));
        assertEquals(Boolean.TRUE, match.eval(1.0, 1.0));
        assertEquals(Boolean.TRUE, match.eval(1, 1.0));
        assertEquals(Boolean.TRUE, match.eval(1L, 1.0));

        assertEquals(Boolean.FALSE, match.eval(1, 2));
        assertEquals(Boolean.FALSE, match.eval(1L, 2L));
        assertEquals(Boolean.FALSE, match.eval(1.0, 2.0));
        assertEquals(Boolean.FALSE, match.eval(1, 1.1));
        assertEquals(Boolean.FALSE, match.eval(1L, 1.1));
    }
}

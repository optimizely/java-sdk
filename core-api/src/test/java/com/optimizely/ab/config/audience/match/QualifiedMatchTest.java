/**
 *
 *    Copyright 2022, Optimizely and contributors
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

public class QualifiedMatchTest {

    private QualifiedMatch match;
    private static final List<Object> INVALIDS = Collections.unmodifiableList(Arrays.asList(new byte[0], new Object(), null));

    @Before
    public void setUp() {
        match = new QualifiedMatch();
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
        assertNull(match.eval("odp-segment-1", "false"));
        assertNull(match.eval("odp-segment-1", null));
    }

    @Test
    public void testValueContains() throws Exception {
        assertTrue(match.eval("odp-segment-1", Collections.singletonList("odp-segment-1")));
        assertFalse(match.eval("odp-segment-1", Collections.singletonList("odp-segment-2")));
    }

    @Test(expected = UnexpectedValueTypeException.class)
    public void testUnknownValueType() throws UnexpectedValueTypeException {
        match.eval(1, Collections.singletonList("odp-segment-1"));
    }

}

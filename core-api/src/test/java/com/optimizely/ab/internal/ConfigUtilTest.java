/**
 *
 *    Copyright 2019, Optimizely
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
package com.optimizely.ab.internal;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigUtilTest {

    private static final String SHARED_KEY = "test.prop";
    private static final String EXPECTED = "bar";

    @After
    public void tearDown() {
        System.setProperty(SHARED_KEY, "INVALID");
    }

    @Test
    public void testSystemPropBeforeOptimizelyProp() {
        String expected = "foo";
        System.setProperty("optimizely." + SHARED_KEY, expected);
        assertEquals(expected, ConfigUtil.get(SHARED_KEY));
    }

    @Test
    public void getFromOptimizelyProp() {
        assertEquals(EXPECTED, ConfigUtil.get("file.only"));
    }

    @Test
    public void getFromSystemProp() {
        System.setProperty("optimizely.sys.only", EXPECTED);
        assertEquals(EXPECTED, ConfigUtil.get("sys.only"));
    }
}
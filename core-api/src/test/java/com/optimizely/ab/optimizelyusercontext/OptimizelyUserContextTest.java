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
package com.optimizely.ab.optimizelyusercontext;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.optimizely.ab.Optimizely;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static com.optimizely.ab.config.ValidProjectConfigV4.ATTRIBUTE_HOUSE_KEY;
import static com.optimizely.ab.config.ValidProjectConfigV4.AUDIENCE_GRYFFINDOR_VALUE;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class OptimizelyUserContextTest {

    public Optimizely optimizely;
    public String userId = "tester";

    @Before
    public void setUp() throws Exception {
        String datafile = Resources.toString(Resources.getResource("config/decide-project-config.json"), Charsets.UTF_8);

        optimizely = new Optimizely.Builder()
            .withDatafile(datafile)
            .build();
    }

    @Test
    public void testOptimizelyUserContext_withAttributes() {
        Map<String, Object> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, userId, attributes);

        assertEquals(user.getOptimizely(), optimizely);
        assertEquals(user.getUserId(), userId);
        assertEquals(user.getAttributes(), attributes);
    }

    @Test
    public void testOptimizelyUserContext_noAttributes() {
        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, userId);

        assertEquals(user.getOptimizely(), optimizely);
        assertEquals(user.getUserId(), userId);
        assertTrue(user.getAttributes().isEmpty());
    }

    @Test
    public void testSetAttribute() {
        Map<String, Object> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, userId, attributes);

        user.setAttribute("k1", "v1");
        user.setAttribute("k2", true);
        user.setAttribute("k3", 100);
        user.setAttribute("k4", 3.5);

        assertEquals(user.getOptimizely(), optimizely);
        assertEquals(user.getUserId(), userId);
        Map<String, Object> newAttributes = user.getAttributes();
        assertEquals(newAttributes.get(ATTRIBUTE_HOUSE_KEY), AUDIENCE_GRYFFINDOR_VALUE);
        assertEquals(newAttributes.get("k1"), "v1");
        assertEquals(newAttributes.get("k2"), true);
        assertEquals(newAttributes.get("k3"), 100);
        assertEquals(newAttributes.get("k4"), 3.5);
    }

    @Test
    public void testSetAttribute_override() {
        Map<String, Object> attributes = Collections.singletonMap(ATTRIBUTE_HOUSE_KEY, AUDIENCE_GRYFFINDOR_VALUE);
        OptimizelyUserContext user = new OptimizelyUserContext(optimizely, userId, attributes);

        user.setAttribute("k1", "v1");
        user.setAttribute(ATTRIBUTE_HOUSE_KEY, "v2");

        assertEquals(user.getOptimizely(), optimizely);
        assertEquals(user.getUserId(), userId);
        Map<String, Object> newAttributes = user.getAttributes();
        assertEquals(newAttributes.get("k1"), "v1");
        assertEquals(newAttributes.get(ATTRIBUTE_HOUSE_KEY), "v2");
    }

}

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
package com.optimizely.ab.optimizelyjson;

import com.optimizely.ab.config.parser.ConfigParser;
import com.optimizely.ab.config.parser.JacksonConfigParser;
import com.optimizely.ab.config.parser.UnsupportedOperationException;
import com.optimizely.ab.optimizelyjson.types.MDN1;
import com.optimizely.ab.optimizelyjson.types.NotMatchingType;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OptimizelyJSONWithJacksonParserTest extends OptimizelyJSONExtendedTest {
    @Override
    protected ConfigParser getParser() {
        return new JacksonConfigParser();
    }

    // Tests for Jackson only

    @Test
    public void testGetValueWithNotMatchingType() throws UnsupportedOperationException {
        OptimizelyJSON oj1 = new OptimizelyJSON(orgJson, getParser());

        // Jackson returns null object when variables not matching (while GSON returns an object with null variables
        NotMatchingType md = oj1.getValue(null, NotMatchingType.class);
        assertNull(md);
    }

    // Tests for integer/double processing

    @Test
    public void testIntegerProcessing() throws UnsupportedOperationException {

        // Jackson parser toMap() keeps ".0" in double

        String json = "{\"k1\":1,\"k2\":2.5,\"k3\":{\"kk1\":3,\"kk2\":4.0}}";

        Map<String,Object> m2 = new HashMap<String,Object>();
        m2.put("kk1", 3);
        m2.put("kk2", 4.0);

        Map<String,Object> m1 = new HashMap<String,Object>();
        m1.put("k1", 1);
        m1.put("k2", 2.5);
        m1.put("k3", m2);

        OptimizelyJSON oj1 = new OptimizelyJSON(json, getParser());
        assertEquals(oj1.toMap(), m1);
    }

    @Test
    public void testIntegerProcessing2() throws UnsupportedOperationException {

        // Jackson parser toString() keeps ".0" in double

        String json = "{\"k1\":1,\"k2\":2.5,\"k3\":{\"kk1\":3,\"kk2\":4.0}}";

        Map<String,Object> m2 = new HashMap<String,Object>();
        m2.put("kk1", 3);
        m2.put("kk2", 4.0);

        Map<String,Object> m1 = new HashMap<String,Object>();
        m1.put("k1", 1);
        m1.put("k2", 2.5);
        m1.put("k3", m2);

        OptimizelyJSON oj1 = new OptimizelyJSON(m1, getParser());
        assertEquals(compact(oj1.toString()), compact(json));
    }

    @Test
    public void testIntegerProcessing3() throws UnsupportedOperationException {
        String json = "{\"k1\":1,\"k2\":2.5,\"k3\":{\"kk1\":3,\"kk2\":4.0}}";

        OptimizelyJSON oj1 = new OptimizelyJSON(json, getParser());
        MDN1 obj = oj1.getValue(null, MDN1.class);

        assertEquals(obj.k1, 1);
    }

}

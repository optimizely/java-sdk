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
package com.optimizely.ab.config.parser;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for {@link JsonHelpers}.
 */
public class JsonHelpersTest {
    private Map<String, Object> map;
    private JSONArray jsonArray;
    private JSONObject jsonObject;

    @Before
    public void setUp() throws Exception {
        List<Object> list = new ArrayList<Object>();
        list.add("vv1");
        list.add(true);

        map = new HashMap<String, Object>();
        map.put("k1", "v1");
        map.put("k2", 3.5);
        map.put("k3", list);

        jsonArray = new JSONArray();
        jsonArray.put("vv1");
        jsonArray.put(true);

        jsonObject = new JSONObject();
        jsonObject.put("k1", "v1");
        jsonObject.put("k2", 3.5);
        jsonObject.put("k3", jsonArray);
    }
        @Test
    public void testConvertToJsonObject() {
        JSONObject value = (JSONObject) JsonHelpers.convertToJsonObject(map);

        assertEquals(value.getString("k1"), "v1");
        assertEquals(value.getDouble("k2"), 3.5, 0.01);
        JSONArray array = value.getJSONArray("k3");
        assertEquals(array.get(0), "vv1");
        assertEquals(array.get(1), true);
    }

    @Test
    public void testJsonObjectToMap() {
        Map<String, Object> value = JsonHelpers.jsonObjectToMap(jsonObject);

        assertEquals(value.get("k1"), "v1");
        assertEquals((Double) value.get("k2"), 3.5, 0.01);
        ArrayList array = (ArrayList) value.get("k3");
        assertEquals(array.get(0), "vv1");
        assertEquals(array.get(1), true);
    }

    @Test
    public void testJsonArrayToList() {
        List<Object> value = JsonHelpers.jsonArrayToList(jsonArray);

        assertEquals(value.get(0), "vv1");
        assertEquals(value.get(1), true);
    }

}

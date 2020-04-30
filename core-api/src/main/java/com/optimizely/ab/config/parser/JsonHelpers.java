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

import java.util.*;

final class JsonHelpers {

    static Object convertToJsonObject(Object obj) {
        if (obj instanceof Map) {
            Map map = (Map)obj;
            JSONObject jObj = new JSONObject();
            for (Object key : map.keySet()) {
                jObj.put(key.toString(), convertToJsonObject(map.get(key)));
            }
            return jObj;
        } else if (obj instanceof List) {
            List list = (List)obj;
            JSONArray jArray = new JSONArray();
            for (Object value : list) {
                jArray.put(convertToJsonObject(value));
            }
            return jArray;
        } else {
            return obj;
        }
    }

    static Map<String, Object> jsonObjectToMap(JSONObject jObj) {
        Map<String, Object> map = new HashMap<>();

        Iterator<String> keys = jObj.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            Object value = jObj.get(key);

            if (value instanceof JSONArray) {
                value = jsonArrayToList((JSONArray)value);
            } else if (value instanceof JSONObject) {
                value = jsonObjectToMap((JSONObject)value);
            }

            map.put(key, value);
        }

        return map;
    }

    static List<Object> jsonArrayToList(JSONArray array) {
        List<Object> list = new ArrayList<>();
        for(Object value : array) {
            if (value instanceof JSONArray) {
                value = jsonArrayToList((JSONArray)value);
            } else if (value instanceof JSONObject) {
                value = jsonObjectToMap((JSONObject)value);
            }

            list.add(value);
        }

        return list;
    }

}

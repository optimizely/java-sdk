/**
 *
 *    Copyright 2018, Optimizely and contributors
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

import com.google.gson.internal.LinkedTreeMap;
import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConditionUtils {
    static public Condition parseConditions(List<Object> rawObjectList) {
        List<Condition> conditions = new ArrayList<Condition>();
        String operand = (String)rawObjectList.get(0);

        for (int i = 1; i < rawObjectList.size(); i++) {
            Object obj = rawObjectList.get(i);
            if (obj instanceof List) {
                List<Object> objectList = (List<Object>)rawObjectList.get(i);
                conditions.add(parseConditions(objectList));
            }
            else if (obj instanceof String) {
                conditions.add(new AudienceIdCondition((String)obj));
            }
            else if (obj instanceof LinkedTreeMap) { // gson
                LinkedTreeMap<String, ?> conditionMap = (LinkedTreeMap<String, ?>)rawObjectList.get(i);
                conditions.add(new UserAttribute((String)conditionMap.get("name"), (String)conditionMap.get("type"),
                        (String)conditionMap.get("match"), conditionMap.get("value")));
            }
            else if (obj instanceof JSONObject) {
                JSONObject conditionMap = (JSONObject)obj;
                conditions.add(new UserAttribute((String)conditionMap.get("name"), (String)conditionMap.get("type"),
                        (String)conditionMap.get("match"), conditionMap.get("value")));
            }
            else {
                Map<String, ?> conditionMap = (Map<String, ?>)rawObjectList.get(i);
                conditions.add(new UserAttribute((String)conditionMap.get("name"), (String)conditionMap.get("type"),
                        (String)conditionMap.get("match"), conditionMap.get("value")));
            }
        }

        Condition condition;
        switch (operand) {
            case "and":
                condition = new AndCondition(conditions);
                break;
            case "or":
                condition = new OrCondition(conditions);
                break;
            case "not":
                condition = new NotCondition(conditions.get(0));
                break;
            default:
                condition = new OrCondition(conditions);
                break;
        }

        return condition;
    }

    static public Condition parseConditions(org.json.JSONArray conditionJson) {
        List<Condition> conditions = new ArrayList<Condition>();
        String operand = (String)conditionJson.get(0);

        for (int i = 1; i < conditionJson.length(); i++) {
            Object obj = conditionJson.get(i);
            if (obj instanceof org.json.JSONArray) {
                conditions.add(parseConditions((org.json.JSONArray) conditionJson.get(i)));
            } else if (obj instanceof String) {
                conditions.add(new AudienceIdCondition((String)obj));
            } else {
                org.json.JSONObject conditionMap = (org.json.JSONObject)obj;
                String match = null;
                Object value = null;
                if (conditionMap.has("match")) {
                    match = (String) conditionMap.get("match");
                }
                if (conditionMap.has("value")) {
                    value = conditionMap.get("value");
                }
                conditions.add(new UserAttribute((String)conditionMap.get("name"), (String)conditionMap.get("type"),
                        match, value));
            }
        }

        Condition condition;
        switch (operand) {
            case "and":
                condition = new AndCondition(conditions);
                break;
            case "or":
                condition = new OrCondition(conditions);
                break;
            case "not":
                condition = new NotCondition(conditions.get(0));
                break;
            default:
                condition = new OrCondition(conditions);
                break;
        }

        return condition;
    }


}

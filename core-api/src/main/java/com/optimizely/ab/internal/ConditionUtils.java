/**
 *
 *    Copyright 2018-2019, 2021, Optimizely and contributors
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
import com.optimizely.ab.config.audience.EmptyCondition;
import com.optimizely.ab.config.audience.NullCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConditionUtils {

    static Logger logger = LoggerFactory.getLogger(ConditionUtils.class);

    static public <T> Condition parseConditions(Class<T> clazz, Object object) throws InvalidAudienceCondition {

        if (object instanceof List) {
            List<Object> objectList = (List<Object>) object;
            return ConditionUtils.<T>parseConditions(clazz, objectList);
        } else if (object instanceof String) { // looking for audience conditions in experiment
            AudienceIdCondition audienceIdCondition = new AudienceIdCondition<T>((String) object);
            if (clazz.isInstance(audienceIdCondition)) {
                return audienceIdCondition;
            } else {
                throw new InvalidAudienceCondition(String.format("Expected AudienceIdCondition got %s", clazz.getCanonicalName()));
            }
        } else {
            try {
                if (object instanceof LinkedTreeMap) { // gson
                    if (clazz != UserAttribute.class) {
                        throw new InvalidAudienceCondition(String.format("Expected UserAttributes got %s", clazz.getCanonicalName()));

                    }

                    LinkedTreeMap<String, ?> conditionMap = (LinkedTreeMap<String, ?>) object;
                    return new UserAttribute((String) conditionMap.get("name"), (String) conditionMap.get("type"),
                        (String) conditionMap.get("match"), conditionMap.get("value"));
                }
            }
            catch (NoClassDefFoundError ex) {
                // no gson loaded... not sure we need to log this if they don't use gson.
                logger.debug("parser: gson library not loaded");
            }

            try {
                if (object instanceof JSONObject) { // simple json
                    if (clazz != UserAttribute.class) {
                        throw new InvalidAudienceCondition(String.format("Expected UserAttributes got %s", clazz.getCanonicalName()));

                    }

                    JSONObject conditionMap = (JSONObject) object;
                    return new UserAttribute((String) conditionMap.get("name"), (String) conditionMap.get("type"),
                        (String) conditionMap.get("match"), conditionMap.get("value"));
                }
            }
            catch (NoClassDefFoundError ex) {
                logger.debug("parser: simple json not found");
            }

            try {
                if (object instanceof org.json.JSONArray) { // json
                    return ConditionUtils.<T>parseConditions(clazz, (org.json.JSONArray) object);
                } else if (object instanceof org.json.JSONObject){ //json
                    if (clazz != UserAttribute.class) {
                        throw new InvalidAudienceCondition(String.format("Expected UserAttributes got %s", clazz.getCanonicalName()));

                    }
                    org.json.JSONObject conditionMap = (org.json.JSONObject) object;
                    String match = null;
                    Object value = null;
                    if (conditionMap.has("match")) {
                        match = (String) conditionMap.get("match");
                    }
                    if (conditionMap.has("value")) {
                        value = conditionMap.get("value");
                    }
                    return new UserAttribute((String) conditionMap.get("name"), (String) conditionMap.get("type"),
                        match, value);
                }
            }
            catch (NoClassDefFoundError ex) {
                logger.debug("parser: json package not found.");
            }
            if (clazz != UserAttribute.class) {
                throw new InvalidAudienceCondition(String.format("Expected UserAttributes got %s", clazz.getCanonicalName()));

            }

            Map<String, ?> conditionMap = (Map<String, ?>) object;
            return new UserAttribute((String) conditionMap.get("name"), (String) conditionMap.get("type"),
                (String) conditionMap.get("match"), conditionMap.get("value"));
        }

    }

    /**
     * parse conditions using List and Map
     *
     * @param clazz the class of parsed condition
     * @param rawObjectList list of conditions
     * @param <T> This is the type parameter
     * @return audienceCondition
     */
    static public <T> Condition parseConditions(Class<T> clazz, List<Object> rawObjectList) throws InvalidAudienceCondition {

        if (rawObjectList.size() == 0) {
            return new EmptyCondition();
        }

        List<Condition> conditions = new ArrayList<Condition>();
        int startingParseIndex = 0;
        String operand = operand(rawObjectList.get(startingParseIndex));
        if (operand != null) {
            startingParseIndex = 1;
        } else {
            operand = "or";
        }

        for (int i = startingParseIndex; i < rawObjectList.size(); i++) {
            Object obj = rawObjectList.get(i);
            conditions.add(parseConditions(clazz, obj));
        }

        return buildCondition(operand, conditions);
    }

    static public String operand(Object object) {
        if (object != null && object instanceof String) {
            String operand = (String) object;
            switch (operand) {
                case "or":
                case "and":
                case "not":
                    return operand;
                default:
                    break;
            }
        }

        return null;
    }

    /**
     * Parse conditions from org.json.JsonArray
     *
     * @param clazz the class of parsed condition
     * @param conditionJson jsonArray to parse
     * @param <T> This is the type parameter
     * @return condition parsed from conditionJson.
     */
    static public <T> Condition parseConditions(Class<T> clazz, org.json.JSONArray conditionJson) throws InvalidAudienceCondition {

        if (conditionJson.length() == 0) {
            return new EmptyCondition();
        }

        List<Condition> conditions = new ArrayList<Condition>();
        int startingParseIndex = 0;
        String operand = operand(conditionJson.get(startingParseIndex));
        if (operand != null) {
            startingParseIndex = 1;
        } else {
            operand = "or";
        }

        for (int i = startingParseIndex; i < conditionJson.length(); i++) {
            Object obj = conditionJson.get(i);
            conditions.add(parseConditions(clazz, obj));
        }

        return buildCondition(operand, conditions);
    }

    private static Condition buildCondition(String operand, List<Condition> conditions) {
        Condition condition;
        switch (operand) {
            case "and":
                condition = new AndCondition(conditions);
                break;
            case "not":
                condition = new NotCondition(conditions.isEmpty() ? new NullCondition() : conditions.get(0));
                break;
            default:
                condition = new OrCondition(conditions);
                break;
        }

        return condition;
    }
}

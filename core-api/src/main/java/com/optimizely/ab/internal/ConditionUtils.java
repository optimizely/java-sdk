/**
 *
 *    Copyright 2018-2019, Optimizely and contributors
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
import com.optimizely.ab.config.audience.*;
import com.optimizely.ab.config.audience.match.*;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConditionUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConditionUtils.class);

    private static final String CUSTOM_ATTRIBUTE = "custom_attribute";
    private static final String MATCH_EXISTS = "exists";
    private static final String MATCH_EXACT = "exact";
    private static final String MATCH_SUBSTRING = "substring";
    private static final String MATCH_GREATER_THAN = "gt";
    private static final String MATCH_LESS_THAN = "lt";

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
            // TODO(llinn) this and subsequent logic that checks for for JSONObject can be combined because both are types of Map
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
     * TODO(llinn) consolidate with {@link #parseConditions(Class, org.json.JSONArray)} by generalizing to {@link Iterable}
     *
     * @param rawObjectList list of conditions
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

        Condition condition;
        switch (operand) {
            case "and":
                condition = new AndCondition(conditions);
                break;
            case "or":
                condition = new OrCondition(conditions);
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

    static public String operand(Object object) {
        if (object instanceof CharSequence) {
            String operand = object.toString();
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
     * TODO(llinn) consolidate with {@link #parseConditions(Class, List)} by generalizing to {@link Iterable}
     *
     * @param conditionJson jsonArray to parse
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

        Condition condition;
        switch (operand) {
            case "and":
                condition = new AndCondition(conditions);
                break;
            case "or":
                condition = new OrCondition(conditions);
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

    /**
     * @return a {@link Condition} that always evaluates to UNKNOWN, i.e. {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> Condition<T> voidCondition() {
        return (Condition<T>) VoidCondition.INSTANCE;
    }

    /**
     * Creates a {@link Condition} instance for a leaf node of audience condition AST.
     *
     * A leaf node is an expression in audience condition that is represented by an object, while
     * an internal node is an expression in audience condition that is represented by array (boolean operators).
     *
     * Only supports "custom_attribute" for {@code type}.
     *
     * Supported values for {@code match}:
     * <ul>
     *   <li>{@code null}</li>
     *   <li>{@code "exists"}</li>
     *   <li>{@code "exact"}</li>
     *   <li>{@code "substring"}</li>
     *   <li>{@code "gt"}</li>
     *   <li>{@code "lt"}</li>
     * </ul>
     *
     * TODO(llinn) move this to an abstract factory object that contains registry of concrete factories based on type/match fields
     *
     * @param match specifies the matching operation
     * @param name key in attributes map to extract input when condition evaluated
     * @param value the fixed operand to a matching operator; required type depends on match
     * @throws InvalidAudienceCondition if type is not equal to "custom_attribute"
     * @throws InvalidAudienceCondition if name is null
     * @throws InvalidAudienceCondition if match is not a supported
     * @throws InvalidAudienceCondition if type is not supported by implementation for {@code match}
     */
    public static Condition<?> leafCondition(
        String type,
        String match,
        String name,
        @Nullable Object value
    ) throws InvalidAudienceCondition {
        if (!CUSTOM_ATTRIBUTE.equals(type)) {
            throw new InvalidAudienceCondition("unknown type");
        }

        if (name == null) {
            throw new InvalidAudienceCondition("name is required");
        }

        // create condition for valid match + value, otherwise falls through
        if (match == null) {
            // legacy conditions do not have match specified and require string value
            if (value instanceof CharSequence) {
                return new EqualsAttributeCondition<>(
                    "legacy_custom_attribute",
                    name,
                    value.toString(),
                    Object.class,
                    true);
            }
        } else {
            switch (match) {
                case MATCH_EXISTS:
                    // TODO(llinn) log if value != null?
                    return new ExistsAttributeCondition(match, name);
                case MATCH_EXACT:
                    if (value instanceof CharSequence) {
                        return new StringAttributeCondition(match, name, value.toString());
                    } else if (value instanceof Boolean) {
                        return new EqualsAttributeCondition<>(match, name, (Boolean) value, Boolean.class, false);
                    } else if (value instanceof Number) {
                        return new NumericAttributeCondition.EqualTo(match, name, (Number) value);
                    }
                    // TODO(llinn) fallback? new EqualsAttributeCondition<>(name, value, Object.class)?
                case MATCH_SUBSTRING:
                    if (value instanceof CharSequence) {
                        return new SubstringAttributeCondition(match, name, (CharSequence) value);
                    }
                    // TODO(llinn) convert to String?
                case MATCH_GREATER_THAN:
                    if (value instanceof Number) {
                        return new NumericAttributeCondition.GreaterThan(match, name, (Number) value);
                    }
                    // TODO(llinn) convert to Number?
                    break;
                case MATCH_LESS_THAN:
                    if (value instanceof Number) {
                        return new NumericAttributeCondition.LessThan(match, name, (Number) value);
                    }
                    // TODO(llinn) convert to Number?
                    break;
                default:
                    throw new InvalidAudienceCondition("unknown match type");
            }
        }

        throw new InvalidAudienceCondition(String.format("unexpected value for %s match",
            match != null ? String.format("'%s'", match) : "legacy"));
    }

}

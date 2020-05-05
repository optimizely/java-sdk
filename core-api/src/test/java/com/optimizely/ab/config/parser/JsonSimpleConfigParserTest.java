/**
 *
 *    Copyright 2016-2017, 2019-2020, Optimizely and contributors
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

import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.FeatureVariable;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.UserAttribute;
import com.optimizely.ab.internal.ConditionUtils;
import com.optimizely.ab.internal.InvalidAudienceCondition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.nullFeatureEnabledConfigJsonV4;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigJsonV2;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigJsonV4;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigJsonV3;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV3;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV4;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.verifyProjectConfig;
import static org.junit.Assert.*;

/**
 * Tests for {@link JsonSimpleConfigParser}.
 */
public class JsonSimpleConfigParserTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void parseProjectConfigV2() throws Exception {
        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV2());
        ProjectConfig expected = validProjectConfigV2();

        verifyProjectConfig(actual, expected);
    }

    @Test
    public void parseProjectConfigV3() throws Exception {
        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV3());
        ProjectConfig expected = validProjectConfigV3();

        verifyProjectConfig(actual, expected);
    }

    @Test
    public void parseProjectConfigV4() throws Exception {
        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV4());
        ProjectConfig expected = validProjectConfigV4();

        verifyProjectConfig(actual, expected);
    }

    @Test
    public void parseNullFeatureEnabledProjectConfigV4() throws Exception {
        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(nullFeatureEnabledConfigJsonV4());

        assertNotNull(actual);

        assertNotNull(actual.getExperiments());

        assertNotNull(actual.getFeatureFlags());

    }

    @Test
    public void parseFeatureVariablesWithJsonPatched() throws Exception {
        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV4());

        // "string" type + "json" subType

        FeatureFlag featureFlag = actual.getFeatureKeyMapping().get("multi_variate_feature");
        FeatureVariable variable = featureFlag.getVariableKeyToFeatureVariableMap().get("json_patched");

        assertEquals(variable.getType(), "json");
    }

    @Test
    public void parseFeatureVariablesWithJsonNative() throws Exception {
        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV4());

        // native "json" type

        FeatureFlag featureFlag = actual.getFeatureKeyMapping().get("multi_variate_future_feature");        FeatureVariable variable = featureFlag.getVariableKeyToFeatureVariableMap().get("json_native");

        assertEquals(variable.getType(), "json");
    }

    @Test
    public void parseFeatureVariablesWithFutureType() throws Exception {
        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV4());

        // unknown type

        FeatureFlag featureFlag = actual.getFeatureKeyMapping().get("multi_variate_future_feature");
        FeatureVariable variable = featureFlag.getVariableKeyToFeatureVariableMap().get("future_variable");

        assertEquals(variable.getType(), "future_type");
    }

    @Test
    public void parseAudience() throws Exception {
        JSONObject jsonObject = new JSONObject();

        jsonObject.append("id", "123");
        jsonObject.append("name", "blah");
        jsonObject.append("conditions",
            "[\"and\", [\"or\", [\"or\", {\"name\": \"doubleKey\", \"type\": \"custom_attribute\", \"match\":\"lt\", \"value\":100.0}]]]");

        Condition<UserAttribute> condition = ConditionUtils.parseConditions(UserAttribute.class, new JSONArray("[\"and\", [\"or\", [\"or\", {\"name\": \"doubleKey\", \"type\": \"custom_attribute\", \"match\":\"lt\", \"value\":100.0}]]]"));

        assertNotNull(condition);
    }

    @Test
    public void parseAudienceLeaf() throws Exception {
        JSONObject jsonObject = new JSONObject();

        jsonObject.append("id", "123");
        jsonObject.append("name", "blah");
        jsonObject.append("conditions",
            "{\"name\": \"doubleKey\", \"type\": \"custom_attribute\", \"match\":\"lt\", \"value\":100.0}");

        Condition<UserAttribute> condition = ConditionUtils.parseConditions(UserAttribute.class, new JSONObject("{\"name\": \"doubleKey\", \"type\": \"custom_attribute\", \"match\":\"lt\", \"value\":100.0}"));

        assertNotNull(condition);
    }

    @Test
    public void parseInvalidAudience() throws Exception {
        thrown.expect(InvalidAudienceCondition.class);
        JSONObject jsonObject = new JSONObject();
        jsonObject.append("id", "123");
        jsonObject.append("name", "blah");
        jsonObject.append("conditions",
            "[\"and\", [\"or\", [\"or\", \"123\"]]]");

        ConditionUtils.parseConditions(UserAttribute.class, new JSONArray("[\"and\", [\"or\", [\"or\", \"123\"]]]"));
    }

    @Test
    public void parseAudienceConditions() throws Exception {
        JSONArray conditions = new JSONArray();
        conditions.put("and");
        conditions.put("1");
        conditions.put("2");
        conditions.put("3");

        Condition condition = ConditionUtils.parseConditions(AudienceIdCondition.class, conditions);
        assertNotNull(condition);
    }

    @Test
    public void parseAudienceCondition() throws Exception {
        String conditions = "1";

        Condition condition = ConditionUtils.parseConditions(AudienceIdCondition.class, conditions);
        assertNotNull(condition);
    }

    @Test
    public void parseInvalidAudienceConditions() throws Exception {
        thrown.expect(InvalidAudienceCondition.class);

        JSONArray conditions = new JSONArray();
        conditions.put("and");
        conditions.put("1");
        conditions.put("2");
        JSONObject userAttribute = new JSONObject();
        userAttribute.append("match", "exact");
        userAttribute.append("type", "custom_attribute");
        userAttribute.append("value", "string");
        userAttribute.append("name", "StringCondition");
        conditions.put(userAttribute);

        ConditionUtils.parseConditions(AudienceIdCondition.class, conditions);
    }

    /**
     * Verify that invalid JSON results in a {@link ConfigParseException} being thrown.
     */
    @Test
    public void invalidJsonExceptionWrapping() throws Exception {
        thrown.expect(ConfigParseException.class);

        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        parser.parseProjectConfig("invalid config");
    }

    /**
     * Verify that valid JSON without a required field results in a {@link ConfigParseException} being thrown.
     */
    @Test
    public void validJsonRequiredFieldMissingExceptionWrapping() throws Exception {
        thrown.expect(ConfigParseException.class);

        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        parser.parseProjectConfig("{\"valid\": \"json\"}");
    }

    /**
     * Verify that empty string JSON results in a {@link ConfigParseException} being thrown.
     */
    @Test
    public void emptyJsonExceptionWrapping() throws Exception {
        thrown.expect(ConfigParseException.class);

        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        parser.parseProjectConfig("");
    }

    /**
     * Verify that null JSON results in a {@link ConfigParseException} being thrown.
     */
    @Test
    @SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "Testing nullness contract violation")
    public void nullJsonExceptionWrapping() throws Exception {
        thrown.expect(ConfigParseException.class);

        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        parser.parseProjectConfig(null);
    }

    @Test
    public void testToJson() {
        Map<String, Object> map = new HashMap<>();
        map.put("k1", "v1");
        map.put("k2", 3.5);
        map.put("k3", true);

        String expectedString = "{\"k1\":\"v1\",\"k2\":3.5,\"k3\":true}";

        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        String json = parser.toJson(map);
        assertEquals(json, expectedString);
    }

    @Test
    public void testFromJson() {
        String json = "{\"k1\":\"v1\",\"k2\":3.5,\"k3\":true}";

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("k1", "v1");
        expectedMap.put("k2", 3.5);
        expectedMap.put("k3", true);

        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();

        Map map = null;
        try {
            map = parser.fromJson(json, Map.class);
            assertEquals(map, expectedMap);
        } catch (JsonParseException e) {
            fail("Parse to map failed: " + e.getMessage());
        }

        // not-supported parse type

        try {
            List value = parser.fromJson(json, List.class);
            fail("Unsupported parse target type: " + value.toString());
        } catch (JsonParseException e) {
            assertEquals(e.getMessage(), "Parsing fails with a unsupported type");
        }
    }

}

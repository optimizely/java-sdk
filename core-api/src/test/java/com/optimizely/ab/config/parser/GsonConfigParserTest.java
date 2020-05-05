/**
 *
 *    Copyright 2016-2017, 2020, Optimizely and contributors
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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.TypedAudience;
import com.optimizely.ab.internal.InvalidAudienceCondition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.nullFeatureEnabledConfigJsonV4;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigJsonV2;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigJsonV3;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigJsonV4;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV3;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV4;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.verifyProjectConfig;
import static org.junit.Assert.*;

/**
 * Tests for {@link GsonConfigParser}.
 */
public class GsonConfigParserTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void parseProjectConfigV2() throws Exception {
        GsonConfigParser parser = new GsonConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV2());
        ProjectConfig expected = validProjectConfigV2();

        verifyProjectConfig(actual, expected);
    }

    @Test
    public void parseProjectConfigV3() throws Exception {
        GsonConfigParser parser = new GsonConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV3());
        ProjectConfig expected = validProjectConfigV3();

        verifyProjectConfig(actual, expected);
    }

    @Test
    public void parseProjectConfigV4() throws Exception {
        GsonConfigParser parser = new GsonConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV4());
        ProjectConfig expected = validProjectConfigV4();

        verifyProjectConfig(actual, expected);
    }

    @Test
    public void parseNullFeatureEnabledProjectConfigV4() throws Exception {
        GsonConfigParser parser = new GsonConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(nullFeatureEnabledConfigJsonV4());

        assertNotNull(actual);

        assertNotNull(actual.getExperiments());

        assertNotNull(actual.getFeatureFlags());

    }

    @Test
    public void parseAudience() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "123");
        jsonObject.addProperty("name", "blah");
        jsonObject.addProperty("conditions",
            "[\"and\", [\"or\", [\"or\", {\"name\": \"doubleKey\", \"type\": \"custom_attribute\", \"match\":\"exact\", \"value\":100.0}]]]");

        AudienceGsonDeserializer deserializer = new AudienceGsonDeserializer();
        Type audienceType = new TypeToken<List<Audience>>() {
        }.getType();

        Audience audience = deserializer.deserialize(jsonObject, audienceType, null);

        assertNotNull(audience);
        assertNotNull(audience.getConditions());
    }

    @Test
    public void parseAudienceLeaf() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "123");
        jsonObject.addProperty("name", "blah");
        jsonObject.addProperty("conditions",
            "{\"name\": \"doubleKey\", \"type\": \"custom_attribute\", \"match\":\"exact\", \"value\":100.0}");

        AudienceGsonDeserializer deserializer = new AudienceGsonDeserializer();
        Type audienceType = new TypeToken<List<Audience>>() {
        }.getType();

        Audience audience = deserializer.deserialize(jsonObject, audienceType, null);

        assertNotNull(audience);
        assertNotNull(audience.getConditions());
    }

    @Test
    public void parseTypedAudienceLeaf() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "123");
        jsonObject.addProperty("name", "blah");

        JsonObject userAttribute = new JsonObject();
        userAttribute.addProperty("name", "doubleKey");
        userAttribute.addProperty("type", "custom_attribute");
        userAttribute.addProperty("match", "lt");
        userAttribute.addProperty("value", 100.0);

        jsonObject.add("conditions", userAttribute);

        AudienceGsonDeserializer deserializer = new AudienceGsonDeserializer();
        Type audienceType = new TypeToken<List<TypedAudience>>() {
        }.getType();

        Audience audience = deserializer.deserialize(jsonObject, audienceType, null);

        assertNotNull(audience);
        assertNotNull(audience.getConditions());
    }

    @Test
    public void parseInvalidAudience() throws Exception {
        thrown.expect(InvalidAudienceCondition.class);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "123");
        jsonObject.addProperty("name", "blah");
        jsonObject.addProperty("conditions",
            "[\"and\", [\"or\", [\"or\", \"123\"]]]");

        AudienceGsonDeserializer deserializer = new AudienceGsonDeserializer();
        Type audienceType = new TypeToken<List<Audience>>() {
        }.getType();

        Audience audience = deserializer.deserialize(jsonObject, audienceType, null);

        assertNotNull(audience);
        assertNotNull(audience.getConditions());
    }

    @Test
    public void parseAudienceConditions() throws Exception {
        JsonObject jsonObject = new JsonObject();
        JsonArray conditions = new JsonArray();
        conditions.add("and");
        conditions.add("1");
        conditions.add("2");
        conditions.add("3");

        jsonObject.add("audienceConditions", conditions);
        Condition condition = GsonHelpers.parseAudienceConditions(jsonObject);

        assertNotNull(condition);
    }

    @Test
    public void parseAudienceCondition() throws Exception {
        JsonObject jsonObject = new JsonObject();

        Gson gson = new Gson();


        JsonElement leaf = gson.toJsonTree("1");

        jsonObject.add("audienceConditions", leaf);
        Condition condition = GsonHelpers.parseAudienceConditions(jsonObject);

        assertNotNull(condition);
    }

    @Test
    public void parseInvalidAudienceConditions() throws Exception {
        thrown.expect(InvalidAudienceCondition.class);

        JsonObject jsonObject = new JsonObject();
        JsonArray conditions = new JsonArray();
        conditions.add("and");
        conditions.add("1");
        conditions.add("2");
        JsonObject userAttribute = new JsonObject();
        userAttribute.addProperty("match", "exact");
        userAttribute.addProperty("type", "custom_attribute");
        userAttribute.addProperty("value", "string");
        userAttribute.addProperty("name", "StringCondition");
        conditions.add(userAttribute);

        jsonObject.add("audienceConditions", conditions);
        GsonHelpers.parseAudienceConditions(jsonObject);

    }

    /**
     * Verify that invalid JSON results in a {@link ConfigParseException} being thrown.
     */
    @Test
    public void invalidJsonExceptionWrapping() throws Exception {
        thrown.expect(ConfigParseException.class);

        GsonConfigParser parser = new GsonConfigParser();
        parser.parseProjectConfig("invalid config");
    }

    /**
     * Verify that valid JSON without a required field results in a {@link ConfigParseException} being thrown.
     */
    @Test
    public void validJsonRequiredFieldMissingExceptionWrapping() throws Exception {
        thrown.expect(ConfigParseException.class);

        GsonConfigParser parser = new GsonConfigParser();
        parser.parseProjectConfig("{\"valid\": \"json\"}");
    }

    /**
     * Verify that empty string JSON results in a {@link ConfigParseException} being thrown.
     */
    @Test
    public void emptyJsonExceptionWrapping() throws Exception {
        thrown.expect(ConfigParseException.class);

        GsonConfigParser parser = new GsonConfigParser();
        parser.parseProjectConfig("");
    }

    /**
     * Verify that null JSON results in a {@link ConfigParseException} being thrown.
     */
    @Test
    @SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "Testing nullness contract violation")
    public void nullJsonExceptionWrapping() throws Exception {
        thrown.expect(ConfigParseException.class);

        GsonConfigParser parser = new GsonConfigParser();
        parser.parseProjectConfig(null);
    }

    @Test
    public void testToJson() {
        Map<String, Object> map = new HashMap<>();
        map.put("k1", "v1");
        map.put("k2", 3.5);
        map.put("k3", true);

        String expectedString = "{\"k1\":\"v1\",\"k2\":3.5,\"k3\":true}";

        GsonConfigParser parser = new GsonConfigParser();
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

        GsonConfigParser parser = new GsonConfigParser();

        Map map = null;
        try {
            map = parser.fromJson(json, Map.class);
            assertEquals(map, expectedMap);
        } catch (JsonParseException e) {
            fail("Parse to map failed: " + e.getMessage());
        }

        // invalid JSON string

        String invalidJson = "'k1':'v1','k2':3.5";
        try {
            map = parser.fromJson(invalidJson, Map.class);
            fail("Expected failure for parsing: " + map.toString());
        } catch (JsonParseException e) {
            assertTrue(true);
        }

    }

}

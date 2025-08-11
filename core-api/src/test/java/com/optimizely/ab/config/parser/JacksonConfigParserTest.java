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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.nullFeatureEnabledConfigJsonV4;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigHoldoutJsonV4;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigJsonV2;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigJsonV3;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigJsonV4;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV3;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV4;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV4_holdout;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.verifyProjectConfig;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.FeatureVariable;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.TypedAudience;
import com.optimizely.ab.internal.InvalidAudienceCondition;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link JacksonConfigParser}.
 */
public class JacksonConfigParserTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void parseProjectConfigV2() throws Exception {
        JacksonConfigParser parser = new JacksonConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV2());
        ProjectConfig expected = validProjectConfigV2();

        verifyProjectConfig(actual, expected);
    }

    @Test
    public void parseProjectConfigV3() throws Exception {
        JacksonConfigParser parser = new JacksonConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV3());
        ProjectConfig expected = validProjectConfigV3();

        verifyProjectConfig(actual, expected);
    }

    @SuppressFBWarnings("NP_NULL_PARAM_DEREF")
    @Test
    public void parseProjectConfigV4() throws Exception {
        JacksonConfigParser parser = new JacksonConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV4());
        ProjectConfig expected = validProjectConfigV4();

        verifyProjectConfig(actual, expected);
    }

    @SuppressFBWarnings("NP_NULL_PARAM_DEREF")
    @Test
    public void parseProjectConfigHoldoutV4() throws Exception {
        JacksonConfigParser parser = new JacksonConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigHoldoutJsonV4());
        ProjectConfig expected = validProjectConfigV4_holdout();

        verifyProjectConfig(actual, expected);
    }

    @Test
    public void parseNullFeatureEnabledProjectConfigV4() throws Exception {
        JacksonConfigParser parser = new JacksonConfigParser();
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

        FeatureFlag featureFlag = actual.getFeatureKeyMapping().get("multi_variate_future_feature");
        FeatureVariable variable = featureFlag.getVariableKeyToFeatureVariableMap().get("json_native");

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
        String audienceString =
            "{" +
                "\"id\": \"3468206645\"," +
                "\"name\": \"DOUBLE\"," +
                "\"conditions\": \"[\\\"and\\\", [\\\"or\\\", [\\\"or\\\", {\\\"name\\\": \\\"doubleKey\\\", \\\"type\\\": \\\"custom_attribute\\\", \\\"match\\\":\\\"lt\\\", \\\"value\\\":100.0}]]]\"" +
                "},";

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Audience.class, new AudienceJacksonDeserializer(objectMapper));
        module.addDeserializer(Condition.class, new ConditionJacksonDeserializer(objectMapper));
        objectMapper.registerModule(module);

        Audience audience = objectMapper.readValue(audienceString, Audience.class);
        assertNotNull(audience);
        assertNotNull(audience.getConditions());
    }

    @Test
    public void parseAudienceLeaf() throws Exception {
        String audienceString =
            "{" +
                "\"id\": \"3468206645\"," +
                "\"name\": \"DOUBLE\"," +
                "\"conditions\": \"{\\\"name\\\": \\\"doubleKey\\\", \\\"type\\\": \\\"custom_attribute\\\", \\\"match\\\":\\\"lt\\\", \\\"value\\\":100.0}\"" +
                "},";

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Audience.class, new AudienceJacksonDeserializer(objectMapper));
        module.addDeserializer(Condition.class, new ConditionJacksonDeserializer(objectMapper));
        objectMapper.registerModule(module);

        Audience audience = objectMapper.readValue(audienceString, Audience.class);
        assertNotNull(audience);
        assertNotNull(audience.getConditions());
    }

    @Test
    public void parseTypedAudienceLeaf() throws Exception {
        String audienceString =
            "{" +
                "\"id\": \"3468206645\"," +
                "\"name\": \"DOUBLE\"," +
                "\"conditions\": {\"name\": \"doubleKey\", \"type\": \"custom_attribute\", \"match\":\"lt\", \"value\":100.0}" +
                "},";

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TypedAudience.class, new TypedAudienceJacksonDeserializer(objectMapper));
        module.addDeserializer(Condition.class, new ConditionJacksonDeserializer(objectMapper));
        objectMapper.registerModule(module);

        Audience audience = objectMapper.readValue(audienceString, TypedAudience.class);
        assertNotNull(audience);
        assertNotNull(audience.getConditions());
    }

    @Test
    public void parseInvalidAudience() throws Exception {
        thrown.expect(InvalidAudienceCondition.class);
        String audienceString =
            "{" +
                "\"id\": \"123\"," +
                "\"name\":\"blah\"," +
                "\"conditions\":" +
                "\"[\\\"and\\\", [\\\"or\\\", [\\\"or\\\", \\\"123\\\"]]]\"" +
                "}";

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Audience.class, new AudienceJacksonDeserializer(objectMapper));
        module.addDeserializer(Condition.class, new ConditionJacksonDeserializer(objectMapper));
        objectMapper.registerModule(module);

        Audience audience = objectMapper.readValue(audienceString, Audience.class);
        assertNotNull(audience);
        assertNotNull(audience.getConditions());
    }

    @Test
    public void parseAudienceCondition() throws Exception {
        String conditionString = "\"123\"";

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Audience.class, new AudienceJacksonDeserializer(objectMapper));
        module.addDeserializer(Condition.class, new ConditionJacksonDeserializer(objectMapper));
        objectMapper.registerModule(module);

        Condition condition = objectMapper.readValue(conditionString, Condition.class);
        assertNotNull(condition);
    }

    @Test
    public void parseAudienceConditions() throws Exception {
        String conditionString =
            "[\"and\", \"12\", \"123\"]";

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Audience.class, new AudienceJacksonDeserializer(objectMapper));
        module.addDeserializer(Condition.class, new ConditionJacksonDeserializer(objectMapper));
        objectMapper.registerModule(module);

        Condition condition = objectMapper.readValue(conditionString, Condition.class);
        assertNotNull(condition);
    }

    @Test
    public void parseInvalidAudienceConditions() throws Exception {
        thrown.expect(InvalidAudienceCondition.class);

        String jsonString = "[\"and\", [\"or\", [\"or\", {\"name\": \"doubleKey\", \"type\": \"custom_attribute\", \"match\":\"lt\", \"value\":100.0}]]]";

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Audience.class, new AudienceJacksonDeserializer(objectMapper));
        module.addDeserializer(Condition.class, new ConditionJacksonDeserializer(objectMapper));
        objectMapper.registerModule(module);

        Condition condition = objectMapper.readValue(jsonString, Condition.class);
        assertNotNull(condition);

    }

    /**
     * Verify that invalid JSON results in a {@link ConfigParseException} being thrown.
     */
    @Test
    public void invalidJsonExceptionWrapping() throws Exception {
        thrown.expect(ConfigParseException.class);

        JacksonConfigParser parser = new JacksonConfigParser();
        parser.parseProjectConfig("invalid config");
    }

    /**
     * Verify that valid JSON without a required field results in a {@link ConfigParseException} being thrown.
     */
    @Test
    public void validJsonRequiredFieldMissingExceptionWrapping() throws Exception {
        thrown.expect(ConfigParseException.class);

        JacksonConfigParser parser = new JacksonConfigParser();
        parser.parseProjectConfig("{\"valid\": \"json\"}");
    }

    /**
     * Verify that empty string JSON results in a {@link ConfigParseException} being thrown.
     */
    @Test
    public void emptyJsonExceptionWrapping() throws Exception {
        thrown.expect(ConfigParseException.class);

        JacksonConfigParser parser = new JacksonConfigParser();
        parser.parseProjectConfig("");
    }

    /**
     * Verify that null JSON results in a {@link ConfigParseException} being thrown.
     */
    @Test
    @SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "Testing nullness contract violation")
    public void nullJsonExceptionWrapping() throws Exception {
        thrown.expect(ConfigParseException.class);

        JacksonConfigParser parser = new JacksonConfigParser();
        parser.parseProjectConfig(null);
    }

    @Test
    public void integrationsArrayAbsent() throws Exception {
        JacksonConfigParser parser = new JacksonConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(nullFeatureEnabledConfigJsonV4());
        assertEquals(actual.getHostForODP(), "");
        assertEquals(actual.getPublicKeyForODP(), "");
    }

    @Test
    public void integrationsArrayHasODP() throws Exception {
        JacksonConfigParser parser = new JacksonConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV4());
        assertEquals(actual.getHostForODP(), "https://example.com");
        assertEquals(actual.getPublicKeyForODP(), "test-key");
    }

    @Test
    public void integrationsArrayHasOtherIntegration() throws Exception {
        JacksonConfigParser parser = new JacksonConfigParser();
        String integrationsObject = ", \"integrations\": [" +
            "{ \"key\": \"not-odp\", " +
            "\"host\": \"https://example.com\", " +
            "\"publicKey\": \"test-key\" }" +
            "]}";
        String datafile = nullFeatureEnabledConfigJsonV4();
        datafile = datafile.substring(0, datafile.lastIndexOf("}")) + integrationsObject;
        ProjectConfig actual = parser.parseProjectConfig(datafile);
        assertEquals(actual.getIntegrations().size(), 1);
        assertEquals(actual.getHostForODP(), "");
        assertEquals(actual.getPublicKeyForODP(), "");
    }

    @Test
    public void integrationsArrayHasMissingHost() throws Exception {
        JacksonConfigParser parser = new JacksonConfigParser();
        String integrationsObject = ", \"integrations\": [" +
            "{ \"key\": \"odp\", " +
            "\"publicKey\": \"test-key\" }" +
            "]}";
        String datafile = nullFeatureEnabledConfigJsonV4();
        datafile = datafile.substring(0, datafile.lastIndexOf("}")) + integrationsObject;
        ProjectConfig actual = parser.parseProjectConfig(datafile);
        assertEquals(actual.getHostForODP(), null);
        assertEquals(actual.getPublicKeyForODP(), "test-key");
    }

    @Test
    public void integrationsArrayHasOtherKeys() throws Exception {
        JacksonConfigParser parser = new JacksonConfigParser();
        String integrationsObject = ", \"integrations\": [" +
            "{ \"key\": \"odp\", " +
            "\"host\": \"https://example.com\", " +
            "\"publicKey\": \"test-key\", " +
            "\"new-key\": \"new-value\" }" +
            "]}";
        String datafile = nullFeatureEnabledConfigJsonV4();
        datafile = datafile.substring(0, datafile.lastIndexOf("}")) + integrationsObject;
        ProjectConfig actual = parser.parseProjectConfig(datafile);
        assertEquals(actual.getIntegrations().size(), 1);
        assertEquals(actual.getHostForODP(), "https://example.com");
        assertEquals(actual.getPublicKeyForODP(), "test-key");
    }

    @Test
    public void testToJson() {
        Map<String, Object> map = new HashMap<>();
        map.put("k1", "v1");
        map.put("k2", 3.5);
        map.put("k3", true);

        String expectedString = "{\"k1\":\"v1\",\"k2\":3.5,\"k3\":true}";

        JacksonConfigParser parser = new JacksonConfigParser();
        String json = null;
        try {
            json = parser.toJson(map);
            assertEquals(json, expectedString);
        } catch (JsonParseException e) {
            fail("Parse to serialize to a JSON string: " + e.getMessage());
        }
    }

    @Test
    public void testFromJson() {
        String json = "{\"k1\":\"v1\",\"k2\":3.5,\"k3\":true}";

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("k1", "v1");
        expectedMap.put("k2", 3.5);
        expectedMap.put("k3", true);

        JacksonConfigParser parser = new JacksonConfigParser();

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

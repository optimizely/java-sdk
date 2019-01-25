/**
 *
 *    Copyright 2016-2017, 2019, Optimizely and contributors
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

import static com.optimizely.ab.config.ProjectConfigTestUtils.nullFeatureEnabledConfigJsonV4;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV4;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV4;
import static com.optimizely.ab.config.ProjectConfigTestUtils.verifyProjectConfig;
import static org.junit.Assert.assertNotNull;

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
}

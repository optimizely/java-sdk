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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.TypedAudience;
import com.optimizely.ab.internal.InvalidAudienceCondition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV4;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV4;
import static com.optimizely.ab.config.ProjectConfigTestUtils.verifyProjectConfig;
import static org.junit.Assert.assertNotNull;

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

    @Test
    public void parseProjectConfigV4() throws Exception {
        JacksonConfigParser parser = new JacksonConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV4());
        ProjectConfig expected = validProjectConfigV4();

        verifyProjectConfig(actual, expected);
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
}

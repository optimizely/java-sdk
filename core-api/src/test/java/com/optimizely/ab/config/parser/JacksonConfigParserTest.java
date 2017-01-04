/**
 *
 *    Copyright 2016, Optimizely and contributors
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV1;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV1;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.verifyProjectConfig;

/**
 * Tests for {@link JacksonConfigParser}.
 */
public class JacksonConfigParserTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void parseProjectConfigV1() throws Exception {
        JacksonConfigParser parser = new JacksonConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJsonV1());
        ProjectConfig expected = validProjectConfigV1();

        verifyProjectConfig(actual, expected);
    }

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
}
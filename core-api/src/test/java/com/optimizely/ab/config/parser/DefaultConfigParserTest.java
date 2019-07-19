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
import com.optimizely.ab.internal.PropertyUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.*;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigJsonV4;
import static junit.framework.TestCase.fail;

/**
 * Tests for {@link DefaultConfigParser}.
 */
@RunWith(Parameterized.class)
public class DefaultConfigParserTest {

    @Parameterized.Parameters(name = "{index}")
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{
            {
                validConfigJsonV2(),
                validProjectConfigV2()
            },
            {
                validConfigJsonV3(),
                validProjectConfigV3()
            },
            {
                validConfigJsonV4(),
                validProjectConfigV4()
            }
        });
    }

    @Parameterized.Parameter(0)
    public String validDatafile;

    @Parameterized.Parameter(1)
    public ProjectConfig validProjectConfig;

    /**
     * This method is to test DefaultConfigParser when different default_parser gets set.
     * For example: when optimizely_default_parser environment variable will be set to "GSON_CONFIG_PARSER" than
     * "DefaultConfigParser.getInstance()" returns "GsonConfigParser" and parse ProjectConfig using it. Also
     * this test will assertThat "configParser" (Provided in env variable) is instance of "GsonConfigParser.class"
     *
     * @throws Exception
     */
    @Test
    public void testPropertyDefaultParser() throws Exception {
        String defaultParser = PropertyUtils.get("default_parser");
        ConfigParser configParser = DefaultConfigParser.getInstance();
        ProjectConfig actual = configParser.parseProjectConfig(validDatafile);
        ProjectConfig expected = validProjectConfig;
        verifyProjectConfig(actual, expected);
        Class expectedParser = GsonConfigParser.class;

        if(defaultParser != null) {
            DefaultConfigParser.ConfigParserSupplier defaultParserSupplier = DefaultConfigParser.ConfigParserSupplier.valueOf(defaultParser);
            switch (defaultParserSupplier) {
                case GSON_CONFIG_PARSER:
                    expectedParser = GsonConfigParser.class;
                    break;
                case JACKSON_CONFIG_PARSER:
                    expectedParser = JacksonConfigParser.class;
                    break;
                case JSON_CONFIG_PARSER:
                    expectedParser = JsonConfigParser.class;
                    break;
                case JSON_SIMPLE_CONFIG_PARSER:
                    expectedParser = JsonSimpleConfigParser.class;
                    break;
                default:
                    fail("Not a valid config parser");
            }
        }

        Assert.assertThat(configParser, CoreMatchers.instanceOf(expectedParser));
    }
}

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
package com.optimizely.ab.config;

import com.optimizely.ab.config.parser.ConfigParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.optimizely.ab.config.ProjectConfigTestUtils.invalidProjectConfigV5;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV4;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link com.optimizely.ab.config.ProjectConfig.Builder}.
 */
public class ProjectConfigBuilderTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void withNullDatafile() throws Exception {
        thrown.expect(ConfigParseException.class);
        new ProjectConfig.Builder()
            .withDatafile(null)
            .build();
    }

    @Test
    public void withEmptyDatafile() throws Exception {
        thrown.expect(ConfigParseException.class);
        new ProjectConfig.Builder()
            .withDatafile("")
            .build();
    }

    @Test
    public void withValidDatafile() throws Exception {
        ProjectConfig projectConfig = new ProjectConfig.Builder()
            .withDatafile(validConfigJsonV4())
            .build();
        assertNotNull(projectConfig);
        assertEquals("4", projectConfig.getVersion());
    }

    @Test
    public void withUnsupportedDatafile() throws Exception {
        thrown.expect(ConfigParseException.class);
        new ProjectConfig.Builder()
            .withDatafile(invalidProjectConfigV5())
            .build();
    }
}

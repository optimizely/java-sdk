/**
 *
 *    Copyright 2019, Optimizely and contributors
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

import org.junit.Before;
import org.junit.Test;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigJsonV4;
import static org.junit.Assert.*;

public class AtomicProjectConfigManagerTest {

    private AtomicProjectConfigManager projectConfigManager;

    @Before
    public void setUp() {
        projectConfigManager = new AtomicProjectConfigManager();
    }

    @Test
    public void testGetAndSetConfig() throws Exception {
        assertNull(projectConfigManager.getConfig());

        ProjectConfig projectConfig = new DatafileProjectConfig.Builder().withDatafile(validConfigJsonV4()).build();
        projectConfigManager.setConfig(projectConfig);
        assertEquals(projectConfig, projectConfigManager.getConfig());
    }
}

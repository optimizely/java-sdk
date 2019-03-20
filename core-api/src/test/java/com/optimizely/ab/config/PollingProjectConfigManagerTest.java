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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class PollingProjectConfigManagerTest {

    private static final int POLLING_INTERVAL_MS = 1;
    private static final int MAX_WAITING_STEPS = 100;

    private PollingProjectConfigManager pollingProjectConfigManager;
    private TestProjectConfigManager projectConfigManager;

    @Before
    public void setUp() throws Exception {
        projectConfigManager = new TestProjectConfigManager();
        pollingProjectConfigManager = PollingProjectConfigManager.builder()
            .withPollingIntervalMs(POLLING_INTERVAL_MS)
            .withConfigManager(projectConfigManager)
            .build();
    }

    @After
    public void tearDown() throws Exception {
        pollingProjectConfigManager.close();
    }

    @Test
    public void testPollingUpdates() throws InterruptedException {
        int maxAttempts = 100;
        int desiredCount = 50;

        blockForStarted(true);

        for (int i = 0; i < maxAttempts; i++) {
            Thread.sleep(POLLING_INTERVAL_MS);
            if (desiredCount <= projectConfigManager.getCount()) {
                return;
            }
        }

        fail(String.format("Max number of attempts exceeded: %s", maxAttempts));
    }

    @Test
    public void testPause() throws InterruptedException {
        int maxAttempts = 100;

        blockForStarted(true);
        pollingProjectConfigManager.pause();
        blockForStarted(false);

        int desiredCount = projectConfigManager.getCount();

        for (int i = 0; i < maxAttempts; i++) {
            Thread.sleep(POLLING_INTERVAL_MS);
            if (desiredCount <= projectConfigManager.getCount()) {
                assertEquals(desiredCount, projectConfigManager.getCount());
            }
        }
    }

    /**
     * This should be replaced by an actual future from the PollingProjectConfigManager
     */
    private void blockForStarted(boolean waitFor) throws InterruptedException {
        // Wait for started replace with future.
        for (int i = 0; i < MAX_WAITING_STEPS; i++) {
            if (pollingProjectConfigManager.isStarted() == waitFor) {
                return;
            }
        }

        fail(String.format("Max attempts waiting for ProjectConfigManager to be started: %s", waitFor));
    }

    private static class TestProjectConfigManager implements ProjectConfigManager {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public ProjectConfig getConfig() {
            counter.incrementAndGet();
            return null;
        }

        public int getCount() {
            return counter.get();
        }
    }
}

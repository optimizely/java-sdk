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

import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.notification.UpdateConfigNotification;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigJsonV4;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PollingProjectConfigManagerTest {

    private static final long POLLING_PERIOD = 10;
    private static final TimeUnit POLLING_UNIT = TimeUnit.MILLISECONDS;
    private static final int PROJECT_CONFIG_DELAY = 100;

    private TestProjectConfigManager testProjectConfigManager;
    private ProjectConfig projectConfig;

    @Before
    public void setUp() throws Exception {
        projectConfig = new DatafileProjectConfig.Builder().withDatafile(validConfigJsonV4()).build();
        testProjectConfigManager = new TestProjectConfigManager(projectConfig);
        testProjectConfigManager.start();
    }

    @After
    public void tearDown() throws Exception {
        testProjectConfigManager.close();
    }

    @Test
    public void testPollingUpdates() throws Exception {
        int maxAttempts = 100;
        int desiredCount = 10;

        testProjectConfigManager.release();

        for (int i = 0; i < maxAttempts; i++) {
            Thread.sleep(PROJECT_CONFIG_DELAY);
            if (desiredCount <= testProjectConfigManager.getCount()) {
                return;
            }
        }

        fail(String.format("Max number of attempts exceeded: %s", maxAttempts));
    }

    @Test
    public void testStop() throws Exception {
        int maxAttempts = 10;

        testProjectConfigManager.release();
        testProjectConfigManager.stop();
        assertFalse(testProjectConfigManager.isRunning());

        int desiredCount = testProjectConfigManager.getCount();

        for (int i = 0; i < maxAttempts; i++) {
            Thread.sleep(PROJECT_CONFIG_DELAY);
            if (desiredCount <= testProjectConfigManager.getCount()) {
                assertEquals(desiredCount, testProjectConfigManager.getCount());
            }
        }
    }

    @Test
    public void testBlockingGetConfig() throws Exception {
        testProjectConfigManager.release();
        TimeUnit.MILLISECONDS.sleep(PROJECT_CONFIG_DELAY);
        assertEquals(projectConfig, testProjectConfigManager.getConfig());
    }

    @Test
    public void testBlockingGetConfigWithDefault() throws Exception {
        testProjectConfigManager.setConfig(projectConfig);
        assertEquals(projectConfig, testProjectConfigManager.getConfig());
    }

    @Test
    public void testBlockingGetConfigWithTimeout() throws Exception {
        testProjectConfigManager.start();
        assertNull(testProjectConfigManager.getConfig());
    }

    @Test
    public void testGetConfigNotStarted() throws Exception {
        testProjectConfigManager.release();
        testProjectConfigManager.close();
        assertFalse(testProjectConfigManager.isRunning());
        assertEquals(projectConfig, testProjectConfigManager.getConfig());
    }

    @Test
    public void testGetConfigNotStartedDefault() throws Exception {
        testProjectConfigManager.setConfig(projectConfig);
        testProjectConfigManager.close();
        assertFalse(testProjectConfigManager.isRunning());
        assertEquals(projectConfig, testProjectConfigManager.getConfig());
    }

    @Test
    public void testSetConfig() {
        testProjectConfigManager = new TestProjectConfigManager() {
            @Override
            public ProjectConfig poll() {
                return null;
            }
        };

        assertNull(testProjectConfigManager.getConfig());

        testProjectConfigManager.setConfig(projectConfig);
        assertEquals(projectConfig, testProjectConfigManager.getConfig());

        testProjectConfigManager.setConfig(null);
        assertEquals(projectConfig, testProjectConfigManager.getConfig());

        ProjectConfig newerProjectConfig = mock(ProjectConfig.class);
        when(newerProjectConfig.getRevision()).thenReturn("new");

        testProjectConfigManager.setConfig(newerProjectConfig);
        assertEquals(newerProjectConfig, testProjectConfigManager.getConfig());
    }

    @Test
    public void testErroringProjectConfigManagerWithTimeout() throws Exception {
        testProjectConfigManager = new TestProjectConfigManager() {
            @Override
            public ProjectConfig poll() {
                throw new RuntimeException();
            }
        };

        testProjectConfigManager.start();
        assertNull(testProjectConfigManager.getConfig());
    }

    @Test
    public void testRecoveringProjectConfigManagerWithTimeout() throws Exception {
        AtomicBoolean throwError = new AtomicBoolean(true);

        testProjectConfigManager = new TestProjectConfigManager() {
                @Override
                public ProjectConfig poll() {
                    if (throwError.get()) {
                        throw new RuntimeException("Test class, expected failure");
                    }

                    return projectConfig;
                }
            };

        testProjectConfigManager.start();
        assertNull(testProjectConfigManager.getConfig());

        throwError.set(false);
        Thread.sleep(2 * PROJECT_CONFIG_DELAY);
        assertEquals(projectConfig, testProjectConfigManager.getConfig());
    }

    @Test
    public void testUpdateConfigNotificationGetsTriggered() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        testProjectConfigManager.getNotificationCenter()
            .<UpdateConfigNotification>getNotificationManager(UpdateConfigNotification.class)
            .addHandler(message -> {countDownLatch.countDown();});

        assertTrue(countDownLatch.await(500, TimeUnit.MILLISECONDS));
    }

    private static class TestProjectConfigManager extends PollingProjectConfigManager {
        private final AtomicInteger counter = new AtomicInteger();

        private final CountDownLatch countDownLatch = new CountDownLatch(1);
        private final ProjectConfig projectConfig;

        private TestProjectConfigManager() {
            this(null);
        }

        private TestProjectConfigManager(ProjectConfig projectConfig) {
            super(POLLING_PERIOD, POLLING_UNIT, POLLING_PERIOD / 2, POLLING_UNIT, new NotificationCenter());
            this.projectConfig = projectConfig;
        }

        @Override
        public ProjectConfig poll() {
            try {
                countDownLatch.await(10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }

            counter.incrementAndGet();
            return projectConfig;
        }

        public int getCount() {
            return counter.get();
        }

        public void release() {
            countDownLatch.countDown();
        }
    }
}

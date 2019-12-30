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

import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.notification.UpdateConfigNotification;
import com.optimizely.ab.optimizelyconfig.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validConfigJsonV4;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
    @Ignore("flaky")
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
    public void testSetOptimizelyConfig(){
        assertNull(testProjectConfigManager.getOptimizelyConfig());

        testProjectConfigManager.setConfig(generateOptimizelyConfig(false));
        OptimizelyConfig initialOptimizelyConfig = testProjectConfigManager.getOptimizelyConfig();
        assertEquals(getExpectedConfig(false), initialOptimizelyConfig);

        testProjectConfigManager.setConfig(null);
        OptimizelyConfig oldOptimizelyConfig = testProjectConfigManager.getOptimizelyConfig();
        assertEquals(oldOptimizelyConfig, getExpectedConfig(false));

        testProjectConfigManager.setConfig(generateOptimizelyConfig(true));
        OptimizelyConfig updatedOptimizelyConfig = testProjectConfigManager.getOptimizelyConfig();
        assertEquals(updatedOptimizelyConfig, getExpectedConfig(true));
    }

    private ProjectConfig generateOptimizelyConfig(boolean updated) {
        return new DatafileProjectConfig(
            "2360254204",
            true,
            true,
            "3918735994",
            updated ? "1480511547" : "1480511548",
            "4",
            asList(
                new Attribute(
                    "553339214",
                    "house"
                ),
                new Attribute(
                    "58339410",
                    "nationality"
                )
            ),
            Collections.<Audience>emptyList(),
            Collections.<Audience>emptyList(),
            asList(
                new EventType(
                    "3785620495",
                    "basic_event",
                    asList("1323241596", "2738374745", "3042640549", "3262035800", "3072915611")
                ),
                new EventType(
                    "3195631717",
                    "event_with_paused_experiment",
                    asList("2667098701")
                )
            ),
            asList(
                new Experiment(
                    "1323241596",
                    "basic_experiment",
                    "Running",
                    "1630555626",
                    Collections.<String>emptyList(),
                    null,
                    asList(
                        new Variation(
                            "1423767502",
                            "A",
                            Collections.<FeatureVariableUsageInstance>emptyList()
                        ),
                        new Variation(
                            "3433458314",
                            "B",
                            Collections.<FeatureVariableUsageInstance>emptyList()
                        )
                    ),
                    Collections.singletonMap("Harry Potter", "A"),
                    asList(
                        new TrafficAllocation(
                            "1423767502",
                            5000
                        ),
                        new TrafficAllocation(
                            "3433458314",
                            10000
                        )
                    )
                ),
                new Experiment(
                    "3262035800",
                    "multivariate_experiment",
                    "Running",
                    "3262035800",
                    asList("3468206642"),
                    null,
                    asList(
                        new Variation(
                            "1880281238",
                            "Fred",
                            true,
                            asList(
                                new FeatureVariableUsageInstance(
                                    "675244127",
                                    "F"
                                ),
                                new FeatureVariableUsageInstance(
                                    "4052219963",
                                    "red"
                                )
                            )
                        ),
                        new Variation(
                            "3631049532",
                            "Feorge",
                            true,
                            asList(
                                new FeatureVariableUsageInstance(
                                    "675244127",
                                    "F"
                                ),
                                new FeatureVariableUsageInstance(
                                    "4052219963",
                                    "eorge"
                                )
                            )
                        )
                    ),
                    Collections.singletonMap("Fred", "Fred"),
                    asList(
                        new TrafficAllocation(
                            "1880281238",
                            2500
                        ),
                        new TrafficAllocation(
                            "3631049532",
                            5000
                        ),
                        new TrafficAllocation(
                            "4204375027",
                            7500
                        ),
                        new TrafficAllocation(
                            "2099211198",
                            10000
                        )
                    )
                )
            ),
            asList(
                new FeatureFlag(
                    "4195505407",
                    "boolean_feature",
                    "",
                    Collections.<String>emptyList(),
                    Collections.<FeatureVariable>emptyList()
                ),
                new FeatureFlag(
                    "3263342226",
                    "multi_variate_feature",
                    "813411034",
                    asList("3262035800"),
                    asList(
                        new FeatureVariable(
                            "675244127",
                            "first_letter",
                            "H",
                            FeatureVariable.VariableStatus.ACTIVE,
                            FeatureVariable.VariableType.STRING
                        ),
                        new FeatureVariable(
                            "4052219963",
                            "rest_of_name",
                            "arry",
                            FeatureVariable.VariableStatus.ACTIVE,
                            FeatureVariable.VariableType.STRING
                        )
                    )
                )
            ),
            Collections.<Group>emptyList(),
            Collections.<Rollout>emptyList()
        );
    }

    private OptimizelyConfig getExpectedConfig(boolean updated) {
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = new HashMap<>();
        optimizelyExperimentMap.put(
            "multivariate_experiment",
            new OptimizelyExperiment(
                "3262035800",
                "multivariate_experiment",
                new HashMap<String, OptimizelyVariation>() {{
                    put(
                        "Feorge",
                        new OptimizelyVariation(
                            "3631049532",
                            "Feorge",
                            true,
                            new HashMap<String, OptimizelyVariable>() {{
                                put(
                                    "first_letter",
                                    new OptimizelyVariable(
                                        "675244127",
                                        "first_letter",
                                        "string",
                                        "F"
                                    )
                                );
                                put(
                                    "rest_of_name",
                                    new OptimizelyVariable(
                                        "4052219963",
                                        "rest_of_name",
                                        "string",
                                        "eorge"
                                    )
                                );
                            }}
                        )
                    );
                    put(
                        "Fred",
                        new OptimizelyVariation(
                            "1880281238",
                            "Fred",
                            true,
                            new HashMap<String, OptimizelyVariable>() {{
                                put(
                                    "first_letter",
                                    new OptimizelyVariable(
                                        "675244127",
                                        "first_letter",
                                        "string",
                                        "F"
                                    )
                                );
                                put(
                                    "rest_of_name",
                                    new OptimizelyVariable(
                                        "4052219963",
                                        "rest_of_name",
                                        "string",
                                        "red"
                                    )
                                );
                            }}
                        )
                    );
                }}
            )
        );
        optimizelyExperimentMap.put(
            "basic_experiment",
            new OptimizelyExperiment(
                "1323241596",
                "basic_experiment",
                new HashMap<String, OptimizelyVariation>() {{
                    put(
                        "A",
                        new OptimizelyVariation(
                            "1423767502",
                            "A",
                            null,
                            Collections.emptyMap()
                        )
                    );
                    put(
                        "B",
                        new OptimizelyVariation(
                            "3433458314",
                            "B",
                            null,
                            Collections.emptyMap()
                        )
                    );
                }}
            )
        );

        Map<String, OptimizelyFeature> optimizelyFeatureMap = new HashMap<>();
        optimizelyFeatureMap.put(
            "multi_variate_feature",
            new OptimizelyFeature(
                "3263342226",
                "multi_variate_feature",
                new HashMap<String, OptimizelyExperiment>() {{
                    put(
                        "multivariate_experiment",
                        new OptimizelyExperiment(
                            "3262035800",
                            "multivariate_experiment",
                            new HashMap<String, OptimizelyVariation>() {{
                                put(
                                    "Feorge",
                                    new OptimizelyVariation(
                                        "3631049532",
                                        "Feorge",
                                        true,
                                        new HashMap<String, OptimizelyVariable>() {{
                                            put(
                                                "first_letter",
                                                new OptimizelyVariable(
                                                    "675244127",
                                                    "first_letter",
                                                    "string",
                                                    "F"
                                                )
                                            );
                                            put(
                                                "rest_of_name",
                                                new OptimizelyVariable(
                                                    "4052219963",
                                                    "rest_of_name",
                                                    "string",
                                                    "eorge"
                                                )
                                            );
                                        }}
                                    )
                                );
                                put(
                                    "Fred",
                                    new OptimizelyVariation(
                                        "1880281238",
                                        "Fred",
                                        true,
                                        new HashMap<String, OptimizelyVariable>() {{
                                            put(
                                                "first_letter",
                                                new OptimizelyVariable(
                                                    "675244127",
                                                    "first_letter",
                                                    "string",
                                                    "F"
                                                )
                                            );
                                            put(
                                                "rest_of_name",
                                                new OptimizelyVariable(
                                                    "4052219963",
                                                    "rest_of_name",
                                                    "string",
                                                    "red"
                                                )
                                            );
                                        }}
                                    )
                                );
                            }}
                        )
                    );
                }},
                new HashMap<String, OptimizelyVariable>() {{
                    put(
                        "first_letter",
                        new OptimizelyVariable(
                            "675244127",
                            "first_letter",
                            "string",
                            "H"
                        )
                    );
                    put(
                        "rest_of_name",
                        new OptimizelyVariable(
                            "4052219963",
                            "rest_of_name",
                            "string",
                            "arry"
                        )
                    );
                }}
            )
        );
        optimizelyFeatureMap.put(
            "boolean_feature",
            new OptimizelyFeature(
                "4195505407",
                "boolean_feature",
                Collections.emptyMap(),
                Collections.emptyMap()
            )
        );

        return new OptimizelyConfig(
            optimizelyExperimentMap,
            optimizelyFeatureMap,
            updated ? "1480511547" : "1480511548"
        );
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

    @Test
    public void testUpdateConfigNotificationDoesNotResultInDeadlock() throws Exception {
        NotificationCenter notificationCenter = new NotificationCenter();

        TestProjectConfigManager testProjectConfigManager = new TestProjectConfigManager(projectConfig, TimeUnit.SECONDS.toMillis(10), notificationCenter);
        notificationCenter.getNotificationManager(UpdateConfigNotification.class)
            .addHandler(message -> {
                assertNotNull(testProjectConfigManager.getConfig());
            });

        testProjectConfigManager.start();
        CompletableFuture.runAsync(testProjectConfigManager::getConfig).get(5, TimeUnit.SECONDS);
    }

    private static class TestProjectConfigManager extends PollingProjectConfigManager {
        private final AtomicInteger counter = new AtomicInteger();

        private final CountDownLatch countDownLatch = new CountDownLatch(1);
        private final ProjectConfig projectConfig;

        private TestProjectConfigManager() {
            this(null);
        }

        private TestProjectConfigManager(ProjectConfig projectConfig) {
            this(projectConfig, POLLING_PERIOD / 2, new NotificationCenter());
        }

        private TestProjectConfigManager(ProjectConfig projectConfig, long blockPeriod, NotificationCenter notificationCenter) {
            super(POLLING_PERIOD, POLLING_UNIT, blockPeriod, POLLING_UNIT, notificationCenter);
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

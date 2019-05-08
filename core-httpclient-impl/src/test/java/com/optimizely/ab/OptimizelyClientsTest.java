/**
 *
 *    Copyright 2019, Optimizely
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
package com.optimizely.ab;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.optimizely.ab.config.HttpProjectConfigManager;
import com.optimizely.ab.event.AsyncEventHandler;
import com.optimizely.ab.internal.PropertyUtils;
import com.optimizely.ab.notification.NotificationCenter;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class OptimizelyClientsTest {

    @Before
    public void setUp() {
        PropertyUtils.clear(AsyncEventHandler.CONFIG_QUEUE_CAPACITY);
        PropertyUtils.clear(AsyncEventHandler.CONFIG_NUM_WORKERS);
        PropertyUtils.clear(HttpProjectConfigManager.CONFIG_POLLING_DURATION);
        PropertyUtils.clear(HttpProjectConfigManager.CONFIG_POLLING_UNIT);
        PropertyUtils.clear(HttpProjectConfigManager.CONFIG_BLOCKING_DURATION);
        PropertyUtils.clear(HttpProjectConfigManager.CONFIG_BLOCKING_UNIT);
        PropertyUtils.clear(HttpProjectConfigManager.CONFIG_SDK_KEY);
    }

    @Test
    public void setEventQueueParams() {
        Integer capacity = 10;
        Integer workers = 5;
        OptimizelyClients.setEventQueueParams(capacity, workers);

        assertEquals(capacity, PropertyUtils.getInteger(AsyncEventHandler.CONFIG_QUEUE_CAPACITY));
        assertEquals(workers, PropertyUtils.getInteger(AsyncEventHandler.CONFIG_NUM_WORKERS));
    }

    @Test
    public void setPollingInterval() {
        Long duration = 10L;
        TimeUnit timeUnit = TimeUnit.MICROSECONDS;
        OptimizelyClients.setPollingInterval(duration, timeUnit);

        assertEquals(duration, PropertyUtils.getLong(HttpProjectConfigManager.CONFIG_POLLING_DURATION));
        assertEquals(timeUnit, PropertyUtils.getEnum(HttpProjectConfigManager.CONFIG_POLLING_UNIT, TimeUnit.class));
    }

    @Test
    public void setBlockingTimeout() {
        Long duration = 20L;
        TimeUnit timeUnit = TimeUnit.NANOSECONDS;
        OptimizelyClients.setBlockingTimeout(duration, timeUnit);

        assertEquals(duration, PropertyUtils.getLong(HttpProjectConfigManager.CONFIG_BLOCKING_DURATION));
        assertEquals(timeUnit, PropertyUtils.getEnum(HttpProjectConfigManager.CONFIG_BLOCKING_UNIT, TimeUnit.class));
    }

    @Test
    public void setSdkKey() {
        String expected = "sdk-key";
        OptimizelyClients.setSdkKey(expected);

        assertEquals(expected, PropertyUtils.get(HttpProjectConfigManager.CONFIG_SDK_KEY));
    }

    @Test
    public void newDefaultInstanceInvalid() {
        Optimizely optimizely = OptimizelyClients.newDefaultInstance();
        assertFalse(optimizely.isValid());
    }

    @Test
    public void newDefaultInstanceWithSdkKey() throws Exception {
        // Set a blocking timeout so we don't block for too long.
        OptimizelyClients.setBlockingTimeout(5, TimeUnit.MICROSECONDS);
        Optimizely optimizely = OptimizelyClients.newDefaultInstance("sdk-key");
        assertFalse(optimizely.isValid());
    }

    @Test
    public void newDefaultInstanceWithFallback() throws Exception {
        String datafileString = Resources.toString(Resources.getResource("valid-project-config-v4.json"), Charsets.UTF_8);
        Optimizely optimizely = OptimizelyClients.newDefaultInstance("sdk-key", datafileString);
        assertTrue(optimizely.isValid());
    }

    @Test
    public void newDefaultInstanceWithProjectConfig() throws Exception {
        Optimizely optimizely = OptimizelyClients.newDefaultInstance(() -> null);
        assertFalse(optimizely.isValid());
    }

    @Test
    public void newDefaultInstanceWithProjectConfigAndNotificationCenter() throws Exception {
        NotificationCenter notificationCenter = new NotificationCenter();
        Optimizely optimizely = OptimizelyClients.newDefaultInstance(() -> null, notificationCenter);
        assertFalse(optimizely.isValid());
        assertEquals(notificationCenter, optimizely.getNotificationCenter());
    }

    @Test
    public void newDefaultInstanceWithProjectConfigAndNotificationCenterAndEventHandler() {
        NotificationCenter notificationCenter = new NotificationCenter();
        Optimizely optimizely = OptimizelyClients.newDefaultInstance(() -> null, notificationCenter, logEvent -> {});
        assertFalse(optimizely.isValid());
        assertEquals(notificationCenter, optimizely.getNotificationCenter());
    }
}
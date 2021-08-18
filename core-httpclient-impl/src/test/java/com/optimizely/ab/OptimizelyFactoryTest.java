/**
 *
 *    Copyright 2019-2020, Optimizely
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
import com.optimizely.ab.event.BatchEventProcessor;
import com.optimizely.ab.internal.PropertyUtils;
import com.optimizely.ab.notification.NotificationCenter;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class OptimizelyFactoryTest {

    private Optimizely optimizely;

    @Before
    public void setUp() {
        PropertyUtils.clear(BatchEventProcessor.CONFIG_BATCH_SIZE);
        PropertyUtils.clear(BatchEventProcessor.CONFIG_BATCH_INTERVAL);
        PropertyUtils.clear(AsyncEventHandler.CONFIG_QUEUE_CAPACITY);
        PropertyUtils.clear(AsyncEventHandler.CONFIG_NUM_WORKERS);
        PropertyUtils.clear(HttpProjectConfigManager.CONFIG_POLLING_DURATION);
        PropertyUtils.clear(HttpProjectConfigManager.CONFIG_POLLING_UNIT);
        PropertyUtils.clear(HttpProjectConfigManager.CONFIG_BLOCKING_DURATION);
        PropertyUtils.clear(HttpProjectConfigManager.CONFIG_BLOCKING_UNIT);
        PropertyUtils.clear(HttpProjectConfigManager.CONFIG_EVICT_DURATION);
        PropertyUtils.clear(HttpProjectConfigManager.CONFIG_EVICT_UNIT);
        PropertyUtils.clear(HttpProjectConfigManager.CONFIG_SDK_KEY);
    }

    @After
    public void tearDown() {
        if (optimizely == null) {
            return;
        }

        optimizely.close();
    }

    @Test
    public void setMaxEventBatchSize() {
        Integer batchSize = 10;
        OptimizelyFactory.setMaxEventBatchSize(batchSize);

        assertEquals(batchSize, PropertyUtils.getInteger(BatchEventProcessor.CONFIG_BATCH_SIZE));
    }

    @Test
    public void setInvalidMaxEventBatchSize() {
        Integer batchSize = 0;
        OptimizelyFactory.setMaxEventBatchSize(batchSize);

        assertNull(PropertyUtils.getInteger(BatchEventProcessor.CONFIG_BATCH_SIZE));
    }

    @Test
    public void setMaxEventBatchInterval() {
        Long batchInterval = 100L;
        OptimizelyFactory.setMaxEventBatchInterval(batchInterval);

        assertEquals(batchInterval, PropertyUtils.getLong(BatchEventProcessor.CONFIG_BATCH_INTERVAL));
    }

    @Test
    public void setInvalidMaxEventBatchInterval() {
        Long batchInterval = 0L;
        OptimizelyFactory.setMaxEventBatchInterval(batchInterval);

        assertNull(PropertyUtils.getLong(BatchEventProcessor.CONFIG_BATCH_INTERVAL));
    }

    @Test
    public void setEventQueueParams() {
        Integer capacity = 10;
        Integer workers = 5;
        OptimizelyFactory.setEventQueueParams(capacity, workers);

        assertEquals(capacity, PropertyUtils.getInteger(AsyncEventHandler.CONFIG_QUEUE_CAPACITY));
        assertEquals(workers, PropertyUtils.getInteger(AsyncEventHandler.CONFIG_NUM_WORKERS));
    }

    @Test
    public void setInvalidEventQueueParams() {
        OptimizelyFactory.setEventQueueParams(-1, 1);
        assertNull(PropertyUtils.getInteger(AsyncEventHandler.CONFIG_QUEUE_CAPACITY));
        assertNull(PropertyUtils.getInteger(AsyncEventHandler.CONFIG_NUM_WORKERS));

        OptimizelyFactory.setEventQueueParams(1, -1);
        assertNull(PropertyUtils.getInteger(AsyncEventHandler.CONFIG_QUEUE_CAPACITY));
        assertNull(PropertyUtils.getInteger(AsyncEventHandler.CONFIG_NUM_WORKERS));
    }

    @Test
    public void setPollingInterval() {
        Long duration = 10L;
        TimeUnit timeUnit = TimeUnit.MICROSECONDS;
        OptimizelyFactory.setPollingInterval(duration, timeUnit);

        assertEquals(duration, PropertyUtils.getLong(HttpProjectConfigManager.CONFIG_POLLING_DURATION));
        assertEquals(timeUnit, PropertyUtils.getEnum(HttpProjectConfigManager.CONFIG_POLLING_UNIT, TimeUnit.class));
    }

    @Test
    public void setInvalidPollingInterval() {
        OptimizelyFactory.setPollingInterval(-1, TimeUnit.MICROSECONDS);
        assertNull(PropertyUtils.getLong(HttpProjectConfigManager.CONFIG_POLLING_DURATION));
        assertNull(PropertyUtils.getEnum(HttpProjectConfigManager.CONFIG_POLLING_UNIT, TimeUnit.class));

        OptimizelyFactory.setPollingInterval(10, null);
        assertNull(PropertyUtils.getLong(HttpProjectConfigManager.CONFIG_POLLING_DURATION));
        assertNull(PropertyUtils.getEnum(HttpProjectConfigManager.CONFIG_POLLING_UNIT, TimeUnit.class));
    }

    @Test
    public void setBlockingTimeout() {
        Long duration = 20L;
        TimeUnit timeUnit = TimeUnit.NANOSECONDS;
        OptimizelyFactory.setBlockingTimeout(duration, timeUnit);

        assertEquals(duration, PropertyUtils.getLong(HttpProjectConfigManager.CONFIG_BLOCKING_DURATION));
        assertEquals(timeUnit, PropertyUtils.getEnum(HttpProjectConfigManager.CONFIG_BLOCKING_UNIT, TimeUnit.class));
    }

    @Test
    public void setInvalidBlockingTimeout() {
        OptimizelyFactory.setBlockingTimeout(-1, TimeUnit.MICROSECONDS);
        assertNull(PropertyUtils.getLong(HttpProjectConfigManager.CONFIG_BLOCKING_DURATION));
        assertNull(PropertyUtils.getEnum(HttpProjectConfigManager.CONFIG_BLOCKING_UNIT, TimeUnit.class));

        OptimizelyFactory.setBlockingTimeout(10, null);
        assertNull(PropertyUtils.getLong(HttpProjectConfigManager.CONFIG_BLOCKING_DURATION));
        assertNull(PropertyUtils.getEnum(HttpProjectConfigManager.CONFIG_POLLING_UNIT, TimeUnit.class));
    }

    @Test
    public void setEvictIdleConnections() {
        Long duration = 2000L;
        TimeUnit timeUnit = TimeUnit.SECONDS;
        OptimizelyFactory.setEvictIdleConnections(duration, timeUnit);

        assertEquals(duration, PropertyUtils.getLong(HttpProjectConfigManager.CONFIG_EVICT_DURATION));
        assertEquals(timeUnit, PropertyUtils.getEnum(HttpProjectConfigManager.CONFIG_EVICT_UNIT, TimeUnit.class));
    }

    @Test
    public void setInvalidEvictIdleConnections() {
        OptimizelyFactory.setEvictIdleConnections(-1, TimeUnit.MICROSECONDS);
        assertNull(PropertyUtils.getLong(HttpProjectConfigManager.CONFIG_EVICT_DURATION));
        assertNull(PropertyUtils.getEnum(HttpProjectConfigManager.CONFIG_EVICT_UNIT, TimeUnit.class));

        OptimizelyFactory.setEvictIdleConnections(10, null);
        assertNull(PropertyUtils.getLong(HttpProjectConfigManager.CONFIG_EVICT_DURATION));
        assertNull(PropertyUtils.getEnum(HttpProjectConfigManager.CONFIG_EVICT_UNIT, TimeUnit.class));
    }

    @Test
    public void setSdkKey() {
        String expected = "sdk-key";
        OptimizelyFactory.setSdkKey(expected);

        assertEquals(expected, PropertyUtils.get(HttpProjectConfigManager.CONFIG_SDK_KEY));
    }

    @Test
    public void setInvalidSdkKey() {
        String expected = "sdk-key";
        OptimizelyFactory.setSdkKey(expected);
        assertEquals(expected, PropertyUtils.get(HttpProjectConfigManager.CONFIG_SDK_KEY));

        OptimizelyFactory.setSdkKey(null);
        assertEquals(expected, PropertyUtils.get(HttpProjectConfigManager.CONFIG_SDK_KEY));
    }

    @Test
    public void setDatafileAccessToken() {
        String expected = "datafile-access-token";
        OptimizelyFactory.setDatafileAccessToken(expected);

        assertEquals(expected, PropertyUtils.get(HttpProjectConfigManager.CONFIG_DATAFILE_AUTH_TOKEN));
    }

    @Test
    public void setInvalidDatafileAccessToken() {
        String expected = "datafile-access-token";
        OptimizelyFactory.setDatafileAccessToken(expected);
        OptimizelyFactory.setDatafileAccessToken(null);
        assertEquals(expected, PropertyUtils.get(HttpProjectConfigManager.CONFIG_DATAFILE_AUTH_TOKEN));
    }

    @Test
    public void newDefaultInstanceInvalid() {
        optimizely = OptimizelyFactory.newDefaultInstance();
        assertFalse(optimizely.isValid());
    }

    @Test
    public void newDefaultInstanceWithSdkKey() throws Exception {
        // Set a blocking timeout so we don't block for too long.
        OptimizelyFactory.setBlockingTimeout(5, TimeUnit.MICROSECONDS);
        optimizely = OptimizelyFactory.newDefaultInstance("sdk-key");
        assertFalse(optimizely.isValid());
    }

    @Test
    public void newDefaultInstanceWithSdkKeyAndCustomHttpClient() throws Exception {
        // Set a blocking timeout so we don't block for too long.
        OptimizelyFactory.setBlockingTimeout(5, TimeUnit.MICROSECONDS);

        // Add custom Proxy and Port here
        int port = 443;
        String proxyHostName = "someProxy.com";
        HttpHost proxyHost = new HttpHost(proxyHostName, port);

        HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxyHost);

        HttpClientBuilder clientBuilder = HttpClients.custom();
        clientBuilder = clientBuilder.setRoutePlanner(routePlanner);

        CloseableHttpClient httpClient = clientBuilder.build();

        optimizely = OptimizelyFactory.newDefaultInstance("sdk-key", httpClient);
        assertFalse(optimizely.isValid());
    }

    @Test
    public void newDefaultInstanceWithFallback() throws Exception {
        String datafileString = Resources.toString(Resources.getResource("valid-project-config-v4.json"), Charsets.UTF_8);
        optimizely = OptimizelyFactory.newDefaultInstance("sdk-key", datafileString);
        assertTrue(optimizely.isValid());
    }

    @Test
    public void newDefaultInstanceWithDatafileAccessToken() throws Exception {
        String datafileString = Resources.toString(Resources.getResource("valid-project-config-v4.json"), Charsets.UTF_8);
        optimizely = OptimizelyFactory.newDefaultInstance("sdk-key", datafileString, "auth-token");
        assertTrue(optimizely.isValid());
    }

    @Test
    public void newDefaultInstanceWithDatafileAccessTokenAndCustomHttpClient() throws Exception {
        // Add custom Proxy and Port here
        int port = 443;
        String proxyHostName = "someProxy.com";
        HttpHost proxyHost = new HttpHost(proxyHostName, port);

        HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxyHost);

        HttpClientBuilder clientBuilder = HttpClients.custom();
        clientBuilder = clientBuilder.setRoutePlanner(routePlanner);

        CloseableHttpClient httpClient = clientBuilder.build();
        OptimizelyFactory.setCustomHttpClient(httpClient);
        String datafileString = Resources.toString(Resources.getResource("valid-project-config-v4.json"), Charsets.UTF_8);
        optimizely = OptimizelyFactory.newDefaultInstance("sdk-key", datafileString, "auth-token");
        assertTrue(optimizely.isValid());
    }

    @Test
    public void newDefaultInstanceWithProjectConfig() throws Exception {
        optimizely = OptimizelyFactory.newDefaultInstance(() -> null);
        assertFalse(optimizely.isValid());
    }

    @Test
    public void newDefaultInstanceWithProjectConfigAndNotificationCenter() throws Exception {
        NotificationCenter notificationCenter = new NotificationCenter();
        optimizely = OptimizelyFactory.newDefaultInstance(() -> null, notificationCenter);
        assertFalse(optimizely.isValid());
        assertEquals(notificationCenter, optimizely.getNotificationCenter());
    }

    @Test
    public void newDefaultInstanceWithProjectConfigAndNotificationCenterAndEventHandler() {
        NotificationCenter notificationCenter = new NotificationCenter();
        optimizely = OptimizelyFactory.newDefaultInstance(() -> null, notificationCenter, logEvent -> {});
        assertFalse(optimizely.isValid());
        assertEquals(notificationCenter, optimizely.getNotificationCenter());
    }
}
/**
 *
 *    Copyright 2019-2021, Optimizely
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

import com.optimizely.ab.config.HttpProjectConfigManager;
import com.optimizely.ab.config.ProjectConfigManager;
import com.optimizely.ab.event.AsyncEventHandler;
import com.optimizely.ab.event.BatchEventProcessor;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.internal.PropertyUtils;
import com.optimizely.ab.notification.NotificationCenter;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * OptimizelyFactory is a utility class to instantiate an {@link Optimizely} client with a minimal
 * number of configuration options. Basic default parameters can be configured via system properties
 * or through the use of an optimizely.properties file. System properties takes precedence over
 * the properties file and are managed via the {@link PropertyUtils} class.
 *
 * OptimizelyFactory also provides setter methods to override the system properties at runtime.
 * <ul>
 *  <li>{@link OptimizelyFactory#setMaxEventBatchSize}</li>
 *  <li>{@link OptimizelyFactory#setMaxEventBatchInterval}</li>
 *  <li>{@link OptimizelyFactory#setEventQueueParams}</li>
 *  <li>{@link OptimizelyFactory#setBlockingTimeout}</li>
 *  <li>{@link OptimizelyFactory#setPollingInterval}</li>
 *  <li>{@link OptimizelyFactory#setSdkKey}</li>
 *  <li>{@link OptimizelyFactory#setDatafileAccessToken}</li>
 * </ul>
 *
 */
public final class OptimizelyFactory {
    private static final Logger logger = LoggerFactory.getLogger(OptimizelyFactory.class);
    private static CloseableHttpClient customHttpClient;

    /**
     * Convenience method for setting the maximum number of events contained within a batch.
     * {@link AsyncEventHandler}
     *
     * @param batchSize The max number of events for batching
     */
    public static void setMaxEventBatchSize(int batchSize) {
        if (batchSize <= 0) {
            logger.warn("Batch size cannot be <= 0. Reverting to default configuration.");
            return;
        }

        PropertyUtils.set(BatchEventProcessor.CONFIG_BATCH_SIZE, Integer.toString(batchSize));
    }

    /**
     * Convenience method for setting the maximum time interval in milliseconds between event dispatches.
     * {@link AsyncEventHandler}
     *
     * @param batchInterval The max time interval for event batching
     */
    public static void setMaxEventBatchInterval(long batchInterval) {
        if (batchInterval <= 0) {
            logger.warn("Batch interval cannot be <= 0. Reverting to default configuration.");
            return;
        }

        PropertyUtils.set(BatchEventProcessor.CONFIG_BATCH_INTERVAL, Long.toString(batchInterval));
    }

    /**
     * Convenience method for setting the required queueing parameters for event dispatching.
     * {@link AsyncEventHandler}
     *
     * @param queueCapacity  A depth of the event queue
     * @param numberWorkers  The number of workers
     */
    public static void setEventQueueParams(int queueCapacity, int numberWorkers) {
        if (queueCapacity <= 0) {
            logger.warn("Queue capacity cannot be <= 0. Reverting to default configuration.");
            return;
        }

        if (numberWorkers <= 0) {
            logger.warn("Number of workers cannot be <= 0. Reverting to default configuration.");
            return;
        }

        PropertyUtils.set(AsyncEventHandler.CONFIG_QUEUE_CAPACITY, Integer.toString(queueCapacity));
        PropertyUtils.set(AsyncEventHandler.CONFIG_NUM_WORKERS, Integer.toString(numberWorkers));
    }

    /**
     * Convenience method for setting the blocking timeout.
     * {@link HttpProjectConfigManager.Builder#withBlockingTimeout(Long, TimeUnit)}
     *
     * @param blockingDuration The blocking time duration
     * @param blockingTimeout The blocking time unit
     */
    public static void setBlockingTimeout(long blockingDuration, TimeUnit blockingTimeout) {
        if (blockingTimeout == null) {
            logger.warn("TimeUnit cannot be null. Reverting to default configuration.");
            return;
        }

        if (blockingDuration <= 0) {
            logger.warn("Timeout cannot be <= 0. Reverting to default configuration.");
            return;
        }

        PropertyUtils.set(HttpProjectConfigManager.CONFIG_BLOCKING_DURATION, Long.toString(blockingDuration));
        PropertyUtils.set(HttpProjectConfigManager.CONFIG_BLOCKING_UNIT, blockingTimeout.toString());
    }

    /**
     * Convenience method for setting the evict idle connections.
     * {@link HttpProjectConfigManager.Builder#withEvictIdleConnections(long, TimeUnit)}
     *
     * @param maxIdleTime The connection idle time duration (0 to disable eviction)
     * @param maxIdleTimeUnit The connection idle time unit
     */
    public static void setEvictIdleConnections(long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        if (maxIdleTimeUnit == null) {
            logger.warn("TimeUnit cannot be null. Reverting to default configuration.");
            return;
        }

        if (maxIdleTime < 0) {
            logger.warn("Timeout cannot be < 0. Reverting to default configuration.");
            return;
        }

        PropertyUtils.set(HttpProjectConfigManager.CONFIG_EVICT_DURATION, Long.toString(maxIdleTime));
        PropertyUtils.set(HttpProjectConfigManager.CONFIG_EVICT_UNIT, maxIdleTimeUnit.toString());
    }

    /**
     * Convenience method for setting the polling interval on System properties.
     * {@link HttpProjectConfigManager.Builder#withPollingInterval(Long, TimeUnit)}
     *
     * @param pollingDuration The polling interval
     * @param pollingTimeout The polling time unit
     */
    public static void setPollingInterval(long pollingDuration, TimeUnit pollingTimeout) {
        if (pollingTimeout == null) {
            logger.warn("TimeUnit cannot be null. Reverting to default configuration.");
            return;
        }

        if (pollingDuration <= 0) {
            logger.warn("Interval cannot be <= 0. Reverting to default configuration.");
            return;
        }

        PropertyUtils.set(HttpProjectConfigManager.CONFIG_POLLING_DURATION, Long.toString(pollingDuration));
        PropertyUtils.set(HttpProjectConfigManager.CONFIG_POLLING_UNIT, pollingTimeout.toString());
    }

    /**
     * Convenience method for setting the sdk key on System properties.
     * {@link HttpProjectConfigManager.Builder#withSdkKey(String)}
     *
     * @param sdkKey The sdk key
     */
    public static void setSdkKey(String sdkKey) {
        if (sdkKey == null) {
            logger.warn("SDK key cannot be null. Reverting to default configuration.");
            return;
        }

        PropertyUtils.set(HttpProjectConfigManager.CONFIG_SDK_KEY, sdkKey);
    }

    /**
     * Convenience method for setting the Datafile Access Token on System properties.
     * {@link HttpProjectConfigManager.Builder#withDatafileAccessToken(String)}
     *
     * @param datafileAccessToken The datafile access token
     */
    public static void setDatafileAccessToken(String datafileAccessToken) {
        if (datafileAccessToken == null) {
            logger.warn("Datafile Access Token cannot be null. Reverting to default configuration.");
            return;
        }

        PropertyUtils.set(HttpProjectConfigManager.CONFIG_DATAFILE_AUTH_TOKEN, datafileAccessToken);
    }

    /**
     * Returns a new Optimizely instance based on preset configuration.
     *
     * @return A new Optimizely instance
     */
    public static Optimizely newDefaultInstance() {
        String sdkKey = PropertyUtils.get(HttpProjectConfigManager.CONFIG_SDK_KEY);
        return newDefaultInstance(sdkKey);
    }

    /**
     * Returns a new Optimizely instance based on preset configuration.
     * EventHandler - {@link AsyncEventHandler}
     * ProjectConfigManager - {@link HttpProjectConfigManager}
     *
     * @param sdkKey SDK key used to build the ProjectConfigManager.
     * @return A new Optimizely instance
     */
    public static Optimizely newDefaultInstance(String sdkKey) {
        if (sdkKey == null) {
            logger.error("Must provide an sdkKey, returning non-op Optimizely client");
            return newDefaultInstance(() -> null);
        }

        return newDefaultInstance(sdkKey, "");
    }

    public static Optimizely newDefaultInstance(String sdkKey, CloseableHttpClient httpClient) {
        if (sdkKey == null || httpClient == null) {
            logger.error("Must provide an sdkKey and httpClient, returning non-op Optimizely client");
            return newDefaultInstance(() -> null);
        }
        customHttpClient = httpClient;
        return newDefaultInstance(sdkKey);
    }

    /**
     * Returns a new Optimizely instance based on preset configuration.
     * EventHandler - {@link AsyncEventHandler}
     * ProjectConfigManager - {@link HttpProjectConfigManager}
     *
     * @param sdkKey   SDK key used to build the ProjectConfigManager.
     * @param fallback Fallback datafile string used by the ProjectConfigManager to be immediately available.
     * @return A new Optimizely instance
     */
    public static Optimizely newDefaultInstance(String sdkKey, String fallback) {
        if (fallback.equals("")) { fallback = null; }
        String datafileAccessToken = PropertyUtils.get(HttpProjectConfigManager.CONFIG_DATAFILE_AUTH_TOKEN);
        return newDefaultInstance(sdkKey, fallback, datafileAccessToken);
    }

    /**
     * Returns a new Optimizely instance with authenticated datafile support.
     *
     * @param sdkKey   SDK key used to build the ProjectConfigManager.
     * @param fallback Fallback datafile string used by the ProjectConfigManager to be immediately available.
     * @param datafileAccessToken  Token for authenticated datafile access.
     * @return A new Optimizely instance
     */
    public static Optimizely newDefaultInstance(String sdkKey, String fallback, String datafileAccessToken) {
        NotificationCenter notificationCenter = new NotificationCenter();
        HttpProjectConfigManager.Builder builder;
        if (customHttpClient != null) {
            OptimizelyHttpClient optimizelyHttpClient = new OptimizelyHttpClient(customHttpClient);

            builder = HttpProjectConfigManager.builder()
                .withDatafile(fallback)
                .withNotificationCenter(notificationCenter)
                .withOptimizelyHttpClient(optimizelyHttpClient)
                .withSdkKey(sdkKey);
        } else {
            builder = HttpProjectConfigManager.builder()
                .withDatafile(fallback)
                .withNotificationCenter(notificationCenter)
                .withSdkKey(sdkKey);
        }
        if (datafileAccessToken != null) {
            builder.withDatafileAccessToken(datafileAccessToken);
        }

        return newDefaultInstance(builder.build(), notificationCenter);
    }

    /**
     * Returns a new Optimizely instance based on preset configuration.
     * EventHandler - {@link AsyncEventHandler}
     *
     * @param configManager The {@link ProjectConfigManager} supplied to Optimizely instance.
     * @return A new Optimizely instance
     */
    public static Optimizely newDefaultInstance(ProjectConfigManager configManager) {
        return newDefaultInstance(configManager, null);
    }

    /**
     * Returns a new Optimizely instance based on preset configuration.
     * EventHandler - {@link AsyncEventHandler}
     *
     * @param configManager      The {@link ProjectConfigManager} supplied to Optimizely instance.
     * @param notificationCenter The {@link NotificationCenter} supplied to Optimizely instance.
     * @return A new Optimizely instance
     */
    public static Optimizely newDefaultInstance(ProjectConfigManager configManager, NotificationCenter notificationCenter) {
        EventHandler eventHandler = AsyncEventHandler.builder().build();
        return newDefaultInstance(configManager, notificationCenter, eventHandler);
    }

    /**
     * Returns a new Optimizely instance based on preset configuration.
     *
     * @param configManager      The {@link ProjectConfigManager} supplied to Optimizely instance.
     * @param notificationCenter The {@link ProjectConfigManager} supplied to Optimizely instance.
     * @param eventHandler       The {@link EventHandler} supplied to Optimizely instance.
     * @return A new Optimizely instance
     * */
    public static Optimizely newDefaultInstance(ProjectConfigManager configManager, NotificationCenter notificationCenter, EventHandler eventHandler) {
        if (notificationCenter == null) {
            notificationCenter = new NotificationCenter();
        }

        BatchEventProcessor eventProcessor = BatchEventProcessor.builder()
            .withEventHandler(eventHandler)
            .withNotificationCenter(notificationCenter)
            .build();

        return Optimizely.builder()
            .withEventProcessor(eventProcessor)
            .withConfigManager(configManager)
            .withNotificationCenter(notificationCenter)
            .build();
    }
}

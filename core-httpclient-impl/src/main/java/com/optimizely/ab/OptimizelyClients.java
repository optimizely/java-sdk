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

import com.optimizely.ab.config.HttpProjectConfigManager;
import com.optimizely.ab.config.ProjectConfigManager;
import com.optimizely.ab.event.AsyncEventHandler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.internal.PropertyUtils;
import com.optimizely.ab.notification.NotificationCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * OptimizelyClients is a utility class to instantiate an {@link Optimizely} client with a minimal
 * number of configuration options. Basic default parameters can be configured via system properties
 * or through the use of an optimizely.properties file. System properties takes precedence over
 * the properties file.
 *
 * OptimizelyClients also provides setter methods to override the system properties at runtime.
 * <ul>
 *  <li></li>
 * </ul>
 * * {@link OptimizelyClients#setEventQueueParams}
 * {@link OptimizelyClients#setEventQueueParams}
 */
public final class OptimizelyClients {
    private static final Logger logger = LoggerFactory.getLogger(OptimizelyClients.class);

    public static final String EVENT_QUEUE_CAPACITY    = "event.queue.capacity";
    public static final String EVENT_NUM_WORKERS       = "event.num.workers";

    private static final int DEFAULT_QUEUE_CAPACITY     = 5000;
    private static final int DEFAULT_NUM_WORKERS        = 2;

    /**
     * Convenience method for setting the required queueing parameters.
     * {@link AsyncEventHandler(int, int)}
     */
    public static void setEventQueueParams(int queueCapacity, int numberWorkers) {
        PropertyUtils.set(EVENT_QUEUE_CAPACITY, Integer.toString(queueCapacity));
        PropertyUtils.set(EVENT_NUM_WORKERS, Integer.toString(numberWorkers));
    }

    /**
     * Convenience method for setting the blocking timeout.
     * {@link HttpProjectConfigManager.Builder#withBlockingTimeout(long, TimeUnit)}
     */
    public static void setBlockingTimeout(long blockingDuration, TimeUnit blockingTimeout) {
        PropertyUtils.set(HttpProjectConfigManager.CONFIG_BLOCKING_DURATION, Long.toString(blockingDuration));
        PropertyUtils.set(HttpProjectConfigManager.CONFIG_BLOCKING_UNIT, blockingTimeout.toString());
    }

    /**
     * Convenience method for setting the polling interval on System properties.
     * {@link HttpProjectConfigManager.Builder#withPollingInterval(long, TimeUnit)}
     */
    public static void setPollingInterval(long pollingDuration, TimeUnit pollingTimeout) {
        PropertyUtils.set(HttpProjectConfigManager.CONFIG_POLLING_DURATION, Long.toString(pollingDuration));
        PropertyUtils.set(HttpProjectConfigManager.CONFIG_POLLING_UNIT, pollingTimeout.toString());
    }

    /**
     * Convenience method for setting the sdk key on System properties.
     * {@link HttpProjectConfigManager.Builder#withSdkKey(String)}
     */
    public static void setSdkKey(String sdkKey) {
        PropertyUtils.set(HttpProjectConfigManager.CONFIG_SDK_KEY, sdkKey);
    }

    /**
     * Returns a new Optimizely instance based on preset configuration.
     * EventHandler - {@link AsyncEventHandler}
     * ProjectConfigManager - {@link HttpProjectConfigManager}
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
     */
    public static Optimizely newDefaultInstance(String sdkKey) {
        if (sdkKey == null) {
            logger.error("Must provide an sdkKey, returning non-op Optimizely client");
            return newDefaultInstance(() -> null);
        }

        return newDefaultInstance(sdkKey, null);
    }

    /**
     * Returns a new Optimizely instance based on preset configuration.
     * EventHandler - {@link AsyncEventHandler}
     * ProjectConfigManager - {@link HttpProjectConfigManager}
     *
     * @param sdkKey   SDK key used to build the ProjectConfigManager.
     * @param fallback Fallback datafile string used by the ProjectConfigManager to be immediately available.
     */
    public static Optimizely newDefaultInstance(String sdkKey, String fallback) {
        NotificationCenter notificationCenter = new NotificationCenter();

        HttpProjectConfigManager.Builder builder = HttpProjectConfigManager.builder()
            .withDatafile(fallback)
            .withNotificationCenter(notificationCenter)
            .withSdkKey(sdkKey);

        return newDefaultInstance(builder.build(), notificationCenter);
    }

    /**
     * Returns a new Optimizely instance based on preset configuration.
     * EventHandler - {@link AsyncEventHandler}
     *
     * @param configManager The {@link ProjectConfigManager} supplied to Optimizely instance.
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
     */
    public static Optimizely newDefaultInstance(ProjectConfigManager configManager, NotificationCenter notificationCenter) {
        if (notificationCenter == null) {
            notificationCenter = new NotificationCenter();
        }

        Integer queueCapacity = PropertyUtils.getInteger(EVENT_QUEUE_CAPACITY, DEFAULT_QUEUE_CAPACITY);
        Integer numWorkers = PropertyUtils.getInteger(EVENT_NUM_WORKERS, DEFAULT_NUM_WORKERS);

        EventHandler eventHandler = new AsyncEventHandler(queueCapacity, numWorkers);
        return newDefaultInstance(configManager, notificationCenter, eventHandler);
    }

    /**
     * Returns a new Optimizely instance based on preset configuration.
     *
     * @param configManager      The {@link ProjectConfigManager} supplied to Optimizely instance.
     * @param notificationCenter The {@link ProjectConfigManager} supplied to Optimizely instance.
     * @param eventHandler       The {@link EventHandler} supplied to Optimizely instance.
     */
    public static Optimizely newDefaultInstance(ProjectConfigManager configManager, NotificationCenter notificationCenter, EventHandler eventHandler) {
        return Optimizely.builder()
            .withEventHandler(eventHandler)
            .withConfigManager(configManager)
            .withNotificationCenter(notificationCenter)
            .build();
    }
}

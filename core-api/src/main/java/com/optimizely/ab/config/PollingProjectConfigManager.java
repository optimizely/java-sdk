/**
 *
 *    Copyright 2019-2020, 2023, Optimizely and contributors
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

import com.optimizely.ab.internal.NotificationRegistry;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.notification.UpdateConfigNotification;
import com.optimizely.ab.optimizelyconfig.OptimizelyConfig;
import com.optimizely.ab.optimizelyconfig.OptimizelyConfigManager;
import com.optimizely.ab.optimizelyconfig.OptimizelyConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PollingProjectConfigManager is an abstract class that provides basic scheduling and caching.
 *
 * Instances of this class, must implement the {@link PollingProjectConfigManager#poll()} method
 * which is responsible for fetching a given ProjectConfig.
 *
 * If this class is never started then calls will be made directly to {@link PollingProjectConfigManager#poll()}
 * since no scheduled execution is being performed.
 *
 * Calling {@link PollingProjectConfigManager#getConfig()} should block until the ProjectConfig
 * is initially set. A default ProjectConfig can be provided to bootstrap the initial ProjectConfig
 * return value and prevent blocking.
 */
public abstract class PollingProjectConfigManager implements ProjectConfigManager, AutoCloseable, OptimizelyConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(PollingProjectConfigManager.class);
    private static final UpdateConfigNotification SIGNAL = new UpdateConfigNotification();

    private final AtomicReference<ProjectConfig> currentProjectConfig = new AtomicReference<>();
    private final AtomicReference<OptimizelyConfig> currentOptimizelyConfig = new AtomicReference<>();
    private final ScheduledExecutorService scheduledExecutorService;
    private final long period;
    private final TimeUnit timeUnit;
    private final long blockingTimeoutPeriod;
    private final TimeUnit blockingTimeoutUnit;
    private final NotificationCenter notificationCenter;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private volatile String sdkKey;
    private volatile boolean started;
    private ScheduledFuture<?> scheduledFuture;

    public PollingProjectConfigManager(long period, TimeUnit timeUnit)  {
        this(period, timeUnit, Long.MAX_VALUE, TimeUnit.MILLISECONDS, new NotificationCenter());
    }

    public PollingProjectConfigManager(long period, TimeUnit timeUnit, NotificationCenter notificationCenter)  {
        this(period, timeUnit, Long.MAX_VALUE, TimeUnit.MILLISECONDS, notificationCenter);
    }

    public PollingProjectConfigManager(long period, TimeUnit timeUnit, long blockingTimeoutPeriod, TimeUnit blockingTimeoutUnit, NotificationCenter notificationCenter)  {
        this.period = period;
        this.timeUnit = timeUnit;
        this.blockingTimeoutPeriod = blockingTimeoutPeriod;
        this.blockingTimeoutUnit = blockingTimeoutUnit;
        this.notificationCenter = notificationCenter;

        final ThreadFactory threadFactory = Executors.defaultThreadFactory();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = threadFactory.newThread(runnable);
            thread.setDaemon(true);
            return thread;
        });
    }

    protected abstract ProjectConfig poll();

    /**
     * Access to current cached project configuration, This is to make sure that config returns without any wait, even if it is null.
     *
     * @return {@link ProjectConfig}
     */
    @Override
    public ProjectConfig getCachedConfig() {
        return currentProjectConfig.get();
    }

    /**
     * Only allow the ProjectConfig to be set to a non-null value, if and only if the value has not already been set.
     * @param projectConfig
     */
    void setConfig(ProjectConfig projectConfig) {
        if (projectConfig == null) {
            return;
        }

        ProjectConfig oldProjectConfig = currentProjectConfig.get();
        String previousRevision = oldProjectConfig == null ? "null" : oldProjectConfig.getRevision();

        if (projectConfig.getRevision().equals(previousRevision)) {
            return;
        }

        if (oldProjectConfig == null) {
            logger.info("New datafile set with revision: {}.", projectConfig.getRevision());
        } else {
            logger.info("New datafile set with revision: {}. Old revision: {}", projectConfig.getRevision(), previousRevision);
        }

        currentProjectConfig.set(projectConfig);
        currentOptimizelyConfig.set(new OptimizelyConfigService(projectConfig).getConfig());
        countDownLatch.countDown();

        if (sdkKey == null) {
            sdkKey = projectConfig.getSdkKey();
        }
        if (sdkKey != null) {
            NotificationRegistry.getInternalNotificationCenter(sdkKey).send(SIGNAL);
        }
        notificationCenter.send(SIGNAL);
    }

    public NotificationCenter getNotificationCenter() {
        return notificationCenter;
    }

    /**
     * If the instance was never started, then call getConfig() directly from the inner ProjectConfigManager.
     * else, wait until the ProjectConfig is set or the timeout expires.
     */
    @Override
    public ProjectConfig getConfig() {
        if (started) {
            try {
                boolean acquired = countDownLatch.await(blockingTimeoutPeriod, blockingTimeoutUnit);
                if (!acquired) {
                    logger.warn("Timeout exceeded waiting for ProjectConfig to be set, returning null.");
                    countDownLatch.countDown();
                }

            } catch (InterruptedException e) {
                logger.warn("Interrupted waiting for valid ProjectConfig, returning null.");
            }

            return currentProjectConfig.get();
        }

        ProjectConfig projectConfig = poll();
        return projectConfig == null ? currentProjectConfig.get() : projectConfig;
    }

    /**
     * Returns the cached {@link OptimizelyConfig}
     * @return {@link OptimizelyConfig}
     */
    @Override
    public OptimizelyConfig getOptimizelyConfig() {
        return currentOptimizelyConfig.get();
    }

    @Override
    public String getSDKKey() {
        return this.sdkKey;
    }

    public synchronized void start() {
        if (started) {
            logger.warn("Manager already started.");
            return;
        }

        if (scheduledExecutorService.isShutdown()) {
            logger.warn("Not starting. Already in shutdown.");
            return;
        }

        Runnable runnable = new ProjectConfigFetcher();
        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(runnable, 0, period, timeUnit);
        started = true;
    }

    public synchronized void stop() {
        if (!started) {
            logger.warn("Not pausing. Manager has not been started.");
            return;
        }

        if (scheduledExecutorService.isShutdown()) {
            logger.warn("Not pausing. Already in shutdown.");
            return;
        }

        logger.info("pausing project watcher");
        scheduledFuture.cancel(true);
        started = false;
    }

    @Override
    public synchronized void close() {
        stop();
        scheduledExecutorService.shutdownNow();
        started = false;
    }

    protected void setSdkKey(String sdkKey) {
        this.sdkKey = sdkKey;
    }

    public boolean isRunning() {
        return started;
    }

    private class ProjectConfigFetcher implements Runnable {
        @Override
        public void run() {
            try {
                ProjectConfig projectConfig = poll();
                setConfig(projectConfig);
            } catch (Exception e) {
                logger.error("Uncaught exception polling for ProjectConfig.", e);
            }
        }
    }
}

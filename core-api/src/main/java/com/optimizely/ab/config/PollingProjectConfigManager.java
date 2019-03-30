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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PollingProjectConfigManager is a composite class that's meant to wrap another ProjectConfigManager
 * and calls the inner ConfigManager at some configured interval. This can also be thought of as a
 * scheduler, but defining yet another interface might not be that advantageous.
 *
 * This is a useful abstraction since it allows the developer to define a stateless ProjectConfigManager,
 * via Http, etc, and this class will provide the state management and scheduling semantics.
 */
public class PollingProjectConfigManager implements ProjectConfigManager, Closeable {

    private static final Logger logger = LoggerFactory.getLogger(PollingProjectConfigManager.class);

    private final AtomicReference<ProjectConfig> currentProjectConfig = new AtomicReference<>();
    private final ScheduledExecutorService scheduledExecutorService;
    private final long pollingIntervalMs;
    private final ProjectConfigManager projectConfigManager;

    private volatile boolean started;
    private ScheduledFuture<?> future;

    public ProjectConfig getConfig() {
        return currentProjectConfig.get();
    }

    private PollingProjectConfigManager(long pollingIntervalMs, ProjectConfigManager projectConfigManager)  {
        logger.info("Created ProjectConfigManager with polling interval: {}", pollingIntervalMs);

        this.pollingIntervalMs = pollingIntervalMs;
        this.projectConfigManager = projectConfigManager;

        final ThreadFactory threadFactory = Executors.defaultThreadFactory();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = threadFactory.newThread(runnable);
            thread.setDaemon(true);
            return thread;
        });

        start();
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

        logger.info("Start polling for SDK key: {}, interval: {} ms", pollingIntervalMs);

        Runnable runnable = new ProjectConfigFetcher();
        future = scheduledExecutorService.scheduleAtFixedRate(runnable, 0, pollingIntervalMs, TimeUnit.MILLISECONDS);
        started = true;
    }

    public synchronized void pause() {
        if (!started) {
            logger.warn("Not pausing. Manager has not been started.");
            return;
        }

        if (scheduledExecutorService.isShutdown()) {
            logger.warn("Not pausing. Already in shutdown.");
            return;
        }

        logger.info("pausing project watcher");
        future.cancel(true);
        started = false;
    }

    @Override
    public synchronized void close() {
        scheduledExecutorService.shutdown();
        started = false;
    }

    public boolean isStarted() {
        return started;
    }

    private class ProjectConfigFetcher implements Runnable {

        @Override
        public void run() {
            ProjectConfig projectConfig = projectConfigManager.getConfig();
            if (projectConfig == null) {
                logger.warn("Null projectConfig fetched");
                return;
            }

            String previousVersion = currentProjectConfig.get() == null ? "null" : currentProjectConfig.get().getRevision();

            if (projectConfig.getRevision().equals(previousVersion)) {
                logger.info("ProjectConfig not changed.");
                return;
            }

            logger.info("New datafile fetched with revision: {}. Old version: {}", projectConfig.getRevision(), previousVersion);

            currentProjectConfig.set(projectConfig);
        }
    }

    public static PollingProjectConfigManager.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long pollingIntervalMs = TimeUnit.MINUTES.toMillis(5);
        private ProjectConfigManager projectConfigManager;

        public Builder withPollingIntervalMs(long pollingIntervalMs) {
            this.pollingIntervalMs = pollingIntervalMs;
            return this;
        }

        public Builder withConfigManager(ProjectConfigManager projectConfigManager) {
            this.projectConfigManager = projectConfigManager;
            return this;
        }

        public PollingProjectConfigManager build() {
            return new PollingProjectConfigManager(pollingIntervalMs, projectConfigManager);
        }
    }
}

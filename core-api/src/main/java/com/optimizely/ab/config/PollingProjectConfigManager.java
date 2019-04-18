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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PollingProjectConfigManager is a composite class that's meant to wrap another ProjectConfigManager
 * and make calls the inner ConfigManager at a fixed interval. This can also be thought of as a
 * scheduler, but defining yet another interface might not be that advantageous.
 *
 * This is a useful abstraction since it allows the developer to define a stateless ProjectConfigManager,
 * via Http, etc, and this class will provide the state management and scheduling semantics.
 *
 * If this instance is never started then calls will be routed directly to the inner ProjectConfigManager
 * since no scheduled execution is being performed.
 *
 * A default ProjectConfig can also be provided to prevent ever returning a null ProjectConfig value.
 *
 * Calling {@link PollingProjectConfigManager#getConfig()} will block until the ProjectConfig
 * is initially set.
 */
public abstract class PollingProjectConfigManager implements ProjectConfigManager, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(PollingProjectConfigManager.class);

    private final AtomicReference<ProjectConfig> currentProjectConfig = new AtomicReference<>();
    private final ScheduledExecutorService scheduledExecutorService;
    private final long period;
    private final TimeUnit timeUnit;

    private final CompletableFuture<ProjectConfigManager> completableFuture = new CompletableFuture<>();

    private volatile boolean started;
    private ScheduledFuture<?> scheduledFuture;

    public PollingProjectConfigManager(long period, TimeUnit timeUnit)  {
        this.period = period;
        this.timeUnit = timeUnit;

        final ThreadFactory threadFactory = Executors.defaultThreadFactory();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = threadFactory.newThread(runnable);
            thread.setDaemon(true);
            return thread;
        });
    }

    protected abstract ProjectConfig poll();

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

        logger.info("New datafile set with revision: {}. Old revision: {}", projectConfig.getRevision(), previousRevision);

        currentProjectConfig.set(projectConfig);
        completableFuture.complete(this);
    }

    /**
     * If the instance was never started, then call getConfig() directly from the inner ProjectConfigManager.
     * else, wait until the ProjectConfig is set or the timeout expires.
     * TODO add timeout to we don't hang indefinitely..
     */
    @Override
    public ProjectConfig getConfig() {
        if (started) {
            try {
                completableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.warn("Interrupted waiting for valid ProjectConfig");
            }

            return currentProjectConfig.get();
        }

        ProjectConfig projectConfig = poll();
        return projectConfig == null ? currentProjectConfig.get() : projectConfig;
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

        Runnable runnable = new ProjectConfigFetcher(this);
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

    public boolean isRunning() {
        return started;
    }

    private static class ProjectConfigFetcher implements Runnable {

        private final PollingProjectConfigManager pollingProjectConfigManager;

        private ProjectConfigFetcher(PollingProjectConfigManager pollingProjectConfigManager) {
            this.pollingProjectConfigManager = pollingProjectConfigManager;
        }

        @Override
        public void run() {
            ProjectConfig projectConfig = pollingProjectConfigManager.poll();
            pollingProjectConfigManager.setConfig(projectConfig);
        }
    }
}

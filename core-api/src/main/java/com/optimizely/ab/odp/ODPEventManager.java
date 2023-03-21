/**
 *
 *    Copyright 2022-2023, Optimizely
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
package com.optimizely.ab.odp;

import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.event.internal.BuildVersionInfo;
import com.optimizely.ab.event.internal.ClientEngineInfo;
import com.optimizely.ab.odp.serializer.ODPJsonSerializerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;

public class ODPEventManager {
    private static final Logger logger = LoggerFactory.getLogger(ODPEventManager.class);
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final int DEFAULT_QUEUE_SIZE = 10000;
    private static final int DEFAULT_FLUSH_INTERVAL = 1000;
    private static final int MAX_RETRIES = 3;
    private static final String EVENT_URL_PATH = "/v3/events";

    private final int queueSize;
    private final int batchSize;
    private final int flushInterval;
    @Nonnull private Map<String, Object> userCommonData = Collections.emptyMap();
    @Nonnull private Map<String, String> userCommonIdentifiers = Collections.emptyMap();

    private Boolean isRunning = false;

    // This needs to be volatile because it will be updated in the main thread and the event dispatcher thread
    //      needs to see the change immediately.
    private volatile ODPConfig odpConfig;
    private EventDispatcherThread eventDispatcherThread;

    private final ODPApiManager apiManager;

    // The eventQueue needs to be thread safe. We are not doing anything extra for thread safety here
    //      because `LinkedBlockingQueue` itself is thread safe.
    private final BlockingQueue<Object> eventQueue = new LinkedBlockingQueue<>();

    public ODPEventManager(@Nonnull ODPApiManager apiManager) {
        this(apiManager, null, null);
    }

    public ODPEventManager(@Nonnull ODPApiManager apiManager, @Nullable Integer queueSize, @Nullable Integer flushInterval) {
        this.apiManager = apiManager;
        this.queueSize = queueSize != null ? queueSize : DEFAULT_QUEUE_SIZE;
        this.flushInterval = (flushInterval != null && flushInterval > 0) ? flushInterval : DEFAULT_FLUSH_INTERVAL;
        this.batchSize = (flushInterval != null && flushInterval == 0) ? 1 : DEFAULT_BATCH_SIZE;
    }

    // these user-provided common data are included in all ODP events in addition to the SDK-generated common data.
    public void setUserCommonData(@Nullable Map<String, Object> commonData) {
        if (commonData != null) this.userCommonData = commonData;
    }

    // these user-provided common identifiers are included in all ODP events in addition to the SDK-generated identifiers.
    public void setUserCommonIdentifiers(@Nullable Map<String, String> commonIdentifiers) {
        if (commonIdentifiers != null) this.userCommonIdentifiers = commonIdentifiers;
    }

    public void start() {
        if (eventDispatcherThread == null) {
            eventDispatcherThread = new EventDispatcherThread();
        }
        if (!isRunning) {
            final ThreadFactory threadFactory = Executors.defaultThreadFactory();
            ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = threadFactory.newThread(runnable);
                thread.setDaemon(true);
                return thread;
            });
            executor.submit(eventDispatcherThread);
        }
        isRunning = true;
    }

    public void updateSettings(ODPConfig newConfig) {
        if (odpConfig == null || (!odpConfig.equals(newConfig) && eventQueue.offer(new FlushEvent(odpConfig)))) {
            odpConfig = newConfig;
        }
    }

    public void identifyUser(String userId) {
        identifyUser(null, userId);
    }

    public void identifyUser(@Nullable String vuid, @Nullable String userId) {
        Map<String, String> identifiers = new HashMap<>();
        if (vuid != null) {
            identifiers.put(ODPUserKey.VUID.getKeyString(), vuid);
        }
        if (userId != null) {
            if (ODPManager.isVuid(userId)) {
                identifiers.put(ODPUserKey.VUID.getKeyString(), userId);
            } else {
                identifiers.put(ODPUserKey.FS_USER_ID.getKeyString(), userId);
            }
        }
        ODPEvent event = new ODPEvent("fullstack", "identified", identifiers, null);
        sendEvent(event);
    }

    public void sendEvent(ODPEvent event) {
        event.setData(augmentCommonData(event.getData()));
        event.setIdentifiers(augmentCommonIdentifiers(event.getIdentifiers()));

        if (!event.isIdentifiersValid()) {
            logger.error("ODP event send failed (event identifiers must have at least one key-value pair)");
            return;
        }

        if (!event.isDataValid()) {
            logger.error("ODP event send failed (event data is not valid)");
            return;
        }

        processEvent(event);
    }

    @VisibleForTesting
    protected Map<String, Object> augmentCommonData(Map<String, Object> sourceData) {
        // priority: sourceData > userCommonData > sdkCommonData

        Map<String, Object> data = new HashMap<>();
        data.put("idempotence_id", UUID.randomUUID().toString());
        data.put("data_source_type", "sdk");
        data.put("data_source", ClientEngineInfo.getClientEngine().getClientEngineValue());
        data.put("data_source_version", BuildVersionInfo.getClientVersion());

        data.putAll(userCommonData);
        data.putAll(sourceData);
        return data;
    }

    @VisibleForTesting
    protected Map<String, String> augmentCommonIdentifiers(Map<String, String> sourceIdentifiers) {
        // priority: sourceIdentifiers > userCommonIdentifiers

        Map<String, String> identifiers = new HashMap<>();
        identifiers.putAll(userCommonIdentifiers);
        identifiers.putAll(sourceIdentifiers);
        return identifiers;
    }

    private void processEvent(ODPEvent event) {
        if (!isRunning) {
            logger.warn("Failed to Process ODP Event. ODPEventManager is not running");
            return;
        }

        if (odpConfig == null || !odpConfig.isReady()) {
            logger.debug("Unable to Process ODP Event. ODPConfig is not ready.");
            return;
        }

        if (eventQueue.size() >= queueSize) {
            logger.warn("Failed to Process ODP Event. Event Queue full. queueSize = " + queueSize);
            return;
        }

        if (!eventQueue.offer(event)) {
            logger.error("Failed to Process ODP Event. Event Queue is not accepting any more events");
        }
    }

    public void stop() {
        logger.debug("Sending stop signal to ODP Event Dispatcher Thread");
        eventDispatcherThread.signalStop();
    }

    private class EventDispatcherThread extends Thread {

        private volatile boolean shouldStop = false;

        private final List<ODPEvent> currentBatch = new ArrayList<>();

        private long nextFlushTime = new Date().getTime();

        @Override
        public void run() {
            while (true) {
                try {
                    Object nextEvent;

                    // If batch has events, set the timeout to remaining time for flush interval,
                    //      otherwise wait for the new event indefinitely
                    if (currentBatch.size() > 0) {
                        nextEvent = eventQueue.poll(nextFlushTime - new Date().getTime(), TimeUnit.MILLISECONDS);
                    } else {
                        nextEvent = eventQueue.take();
                    }

                    if (nextEvent == null) {
                        // null means no new events received and flush interval is over, dispatch whatever is in the batch.
                        if (!currentBatch.isEmpty()) {
                            flush();
                        }
                        if (shouldStop) {
                            break;
                        }
                        continue;
                    }

                    if (nextEvent instanceof FlushEvent) {
                        flush(((FlushEvent) nextEvent).getOdpConfig());
                        continue;
                    }

                    if (currentBatch.size() == 0) {
                        // Batch starting, create a new flush time
                        nextFlushTime = new Date().getTime() + flushInterval;
                    }

                    currentBatch.add((ODPEvent) nextEvent);

                    if (currentBatch.size() >= batchSize) {
                        flush();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            isRunning = false;
            logger.debug("Exiting ODP Event Dispatcher Thread.");
        }

        private void flush(ODPConfig odpConfig) {
            if (currentBatch.size() == 0) {
                return;
            }

            if (odpConfig.isReady()) {
                String payload = ODPJsonSerializerFactory.getSerializer().serializeEvents(currentBatch);
                String endpoint = odpConfig.getApiHost() + EVENT_URL_PATH;
                Integer statusCode;
                int numAttempts = 0;
                do {
                    statusCode = apiManager.sendEvents(odpConfig.getApiKey(), endpoint, payload);
                    numAttempts ++;
                } while (numAttempts < MAX_RETRIES && statusCode != null && (statusCode == 0 || statusCode >= 500));
            } else {
                logger.debug("ODPConfig not ready, discarding event batch");
            }
            currentBatch.clear();
        }

        private void flush() {
            flush(odpConfig);
        }

        public void signalStop() {
            shouldStop = true;
        }
    }

    private static class FlushEvent {
        private final ODPConfig odpConfig;
        public FlushEvent(ODPConfig odpConfig) {
            this.odpConfig = odpConfig.getClone();
        }

        public ODPConfig getOdpConfig() {
            return odpConfig;
        }
    }
}

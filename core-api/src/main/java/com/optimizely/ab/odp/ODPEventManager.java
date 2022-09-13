/**
 *
 *    Copyright 2022, Optimizely
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

import com.optimizely.ab.event.internal.BuildVersionInfo;
import com.optimizely.ab.event.internal.ClientEngineInfo;
import com.optimizely.ab.odp.serializer.ODPJsonSerializerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class ODPEventManager {
    private static final Logger logger = LoggerFactory.getLogger(ODPEventManager.class);
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final int DEFAULT_QUEUE_SIZE = 10000;
    private static final int FLUSH_INTERVAL = 1000;
    private static final int MAX_RETRIES = 3;
    private static final String EVENT_URL_PATH = "/v3/events";

    private int queueSize = DEFAULT_QUEUE_SIZE;
    private int batchSize = DEFAULT_BATCH_SIZE;

    private Boolean isRunning = false;
    private volatile ODPConfig odpConfig;
    private EventDispatcherThread eventDispatcherThread;

    private final ODPApiManager apiManager;
    private final BlockingQueue<ODPEvent> eventQueue = new LinkedBlockingQueue<>();

    public ODPEventManager(ODPConfig odpConfig, ODPApiManager apiManager) {
        this.apiManager = apiManager;
        this.odpConfig = odpConfig;
    }

    public ODPEventManager(ODPConfig odpConfig, ODPApiManager apiManager, int batchSize, int queueSize) {
        this(odpConfig, apiManager);
        this.batchSize = batchSize;
        this.queueSize = queueSize;
    }

    public void start() {
        isRunning = true;
        eventDispatcherThread = new EventDispatcherThread();
        eventDispatcherThread.start();
    }

    public void updateSettings(ODPConfig odpConfig) {
        this.odpConfig = odpConfig;
    }

    public void sendEvents(List<ODPEvent> events) {
        for (ODPEvent event: events) {
            sendEvent(event);
        }
    }

    public void sendEvent(ODPEvent event) {
        event.setData(augmentCommonData(event.getData()));
        processEvent(event);
    }

    private Map<String, Object> augmentCommonData(Map<String, Object> sourceData) {
        Map<String, Object> data = new HashMap<>();
        data.put("idempotence_id", UUID.randomUUID().toString());
        data.put("data_source_type", "sdk");
        data.put("data_source", ClientEngineInfo.getClientEngine().getClientEngineValue());
        data.put("data_source_version", BuildVersionInfo.getClientVersion());
        data.putAll(sourceData);
        return data;
    }

    private void processEvent(ODPEvent event) {
        if (!odpConfig.isReady()) {
            logger.debug("Unable to Process Event. ODPConfig is not ready.");
            return;
        }

        if (!isRunning) {
            logger.warn("Failed to Process ODP Event. ODPEventManager is not running");
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

        private long lastFlushTime = new Date().getTime();

        @Override
        public void run() {
            while (true) {
                try {
                    long nextFlushMillis = Math.max(0, FLUSH_INTERVAL - (new Date().getTime() - lastFlushTime));
                    ODPEvent nextEvent = eventQueue.poll(nextFlushMillis, TimeUnit.MILLISECONDS);

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

                    currentBatch.add(nextEvent);

                    if (currentBatch.size() >= batchSize) {
                        flush();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            logger.warn("Exiting ODP Event Dispatcher Thread");
        }

        private void flush() {
            lastFlushTime = new Date().getTime();

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

        public void signalStop() {
            shouldStop = true;
        }
    }
}

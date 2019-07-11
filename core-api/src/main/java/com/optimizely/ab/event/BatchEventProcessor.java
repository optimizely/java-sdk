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
package com.optimizely.ab.event;

import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.event.internal.EventFactory;
import com.optimizely.ab.event.internal.UserEvent;
import com.optimizely.ab.internal.PropertyUtils;
import com.optimizely.ab.notification.NotificationCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.*;

/**
 * BatchEventProcessor is a batched implementation of the {@link EventProcessor}
 *
 * Events passed to the BatchEventProcessor are immediately added to a BlockingQueue.
 *
 * The BatchEventProcessor maintains a single consumer thread that pulls events off of
 * the BlockingQueue and buffers them for either a configured batch size or for a
 * maximum duration before the resulting LogEvent is sent to the NotificationManager.
 */
public class BatchEventProcessor implements EventProcessor, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(BatchEventProcessor.class);

    public static final String CONFIG_BATCH_SIZE     = "event.processor.batch.size";
    public static final String CONFIG_BATCH_INTERVAL = "event.processor.batch.interval";

    public static final int DEFAULT_QUEUE_CAPACITY  = 1000;
    public static final int DEFAULT_BATCH_SIZE      = 50;
    public static final long DEFAULT_BATCH_INTERVAL = TimeUnit.MINUTES.toMillis(1);

    private static final Object SHUTDOWN_SIGNAL = new Object();

    private final BlockingQueue<Object> eventQueue;
    private final EventHandler eventHandler;

    private final int batchSize;
    private final long flushInterval;
    private final ExecutorService executor;
    private final NotificationCenter notificationCenter;

    private Future<?> future;
    private boolean isStarted = false;

    private BatchEventProcessor(BlockingQueue<Object> eventQueue, EventHandler eventHandler, Integer batchSize, Long flushInterval, ExecutorService executor, NotificationCenter notificationCenter) {
        this.eventHandler = eventHandler;
        this.eventQueue = eventQueue;
        this.batchSize = batchSize == null ? PropertyUtils.getInteger(CONFIG_BATCH_SIZE, DEFAULT_BATCH_SIZE) : batchSize;
        this.flushInterval = flushInterval == null ? PropertyUtils.getLong(CONFIG_BATCH_INTERVAL, DEFAULT_BATCH_INTERVAL) : flushInterval;
        this.notificationCenter = notificationCenter;

        if (executor == null) {
            final ThreadFactory threadFactory = Executors.defaultThreadFactory();
            this.executor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = threadFactory.newThread(runnable);
                thread.setDaemon(true);
                return thread;
            });
        } else {
            this.executor = executor;
        }

        start();
    }

    public synchronized void start() {
        if (isStarted) {
            logger.info("Executor already started.");
            return;
        }

        isStarted = true;
        EventConsumer runnable = new EventConsumer();
        future = executor.submit(runnable);
    }

    @Override
    public void close() throws Exception {
        logger.info("Start close");
        eventQueue.put(SHUTDOWN_SIGNAL);
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while awaiting termination.");
            Thread.currentThread().interrupt();
        }
    }

    public void process(UserEvent userEvent) {
        logger.debug("Received userEvent: {}", userEvent);

        if (executor.isShutdown()) {
            logger.warn("Executor shutdown, not accepting tasks.");
            return;
        }

        if (!eventQueue.offer(userEvent)) {
            logger.warn("Payload not accepted by the queue. Current size: {}", eventQueue.size());
        }
    }

    public class EventConsumer implements Runnable {
        private LinkedList<UserEvent> currentBatch = new LinkedList<>();
        private long deadline = System.currentTimeMillis() + flushInterval;

        @Override
        public void run() {
            try {
                while (true) {
                    if (System.currentTimeMillis() > deadline) {
                        logger.debug("Deadline exceeded flushing current batch.");
                        flush();
                    }

                    Object item = eventQueue.poll(50, TimeUnit.MILLISECONDS);
                    if (item == null) {
                        logger.debug("Empty item, sleeping for 50ms.");
                        Thread.sleep(50);
                        continue;
                    }

                    if (item == SHUTDOWN_SIGNAL) {
                        logger.info("Received shutdown signal.");
                        break;
                    }

                    addToBatch((UserEvent) item);
                }
            } catch (InterruptedException e) {
                logger.info("Interrupted while processing buffer.");
            } catch (Exception e) {
                logger.error("Uncaught exception processing buffer.", e);
            } finally {
                logger.info("Exiting processing loop. Attempting to flush pending events.");
                flush();
            }
        }

        private void addToBatch(UserEvent userEvent) {
            if (shouldSplit(userEvent)) {
                flush();
                currentBatch = new LinkedList<>();
            }

            // Reset the deadline if starting a new batch.
            if (currentBatch.isEmpty()) {
                deadline = System.currentTimeMillis() + flushInterval;
            }

            currentBatch.add(userEvent);
            if (currentBatch.size() >= batchSize) {
                flush();
            }
        }

        private boolean shouldSplit(UserEvent userEvent) {
            if (currentBatch.isEmpty()) {
                return false;
            }

            ProjectConfig currentConfig = currentBatch.peekLast().getUserContext().getProjectConfig();
            ProjectConfig newConfig = userEvent.getUserContext().getProjectConfig();

            // Revisions should match
            if (!currentConfig.getProjectId().equals(newConfig.getProjectId())) {
                return true;
            }

            // Projects should match
            if (!currentConfig.getRevision().equals(newConfig.getRevision())) {
                return true;
            }

            return false;
        }

        private void flush() {
            if (currentBatch.isEmpty()) {
                return;
            }

            LogEvent logEvent = EventFactory.createLogEvent(currentBatch);

            if (notificationCenter != null) {
                notificationCenter.send(logEvent);
            }

            try {
                eventHandler.dispatchEvent(logEvent);
            } catch (Exception e) {
                logger.error("Error dispatching event: {}", logEvent, e);
            }
            currentBatch = new LinkedList<>();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BlockingQueue<Object> eventQueue = new ArrayBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        private EventHandler eventHandler = null;
        private Integer batchSize = null;
        private Long flushInterval = null;
        private ExecutorService executor = null;
        private NotificationCenter notificationCenter = null;

        public Builder withEventHandler(EventHandler eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        public Builder withEventQueue(BlockingQueue<Object> eventQueue) {
            this.eventQueue = eventQueue;
            return this;
        }

        public Builder withBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder withFlushInterval(Long flushInterval) {
            this.flushInterval = flushInterval;
            return this;
        }

        public Builder withExecutor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public Builder withNotificationCenter(NotificationCenter notificationCenter) {
            this.notificationCenter = notificationCenter;
            return this;
        }

        public BatchEventProcessor build() {
            return new BatchEventProcessor(eventQueue, eventHandler, batchSize, flushInterval, executor, notificationCenter);
        }
    }

}

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

import static com.optimizely.ab.internal.SafetyUtils.tryClose;

/**
 * BatchEventProcessor is a batched implementation of the {@link EventProcessor}
 *
 * Events passed to the BatchEventProcessor are immediately added to a BlockingQueue.
 *
 * The BatchEventProcessor maintains a single consumer thread that pulls events off of
 * the BlockingQueue and buffers them for either a configured batch size or for a
 * maximum duration before the resulting LogEvent is sent to the EventHandler
 * and NotificationCenter.
 */
public class BatchEventProcessor implements EventProcessor, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(BatchEventProcessor.class);

    public static final String CONFIG_BATCH_SIZE     = "event.processor.batch.size";
    public static final String CONFIG_BATCH_INTERVAL = "event.processor.batch.interval";
    public static final String CONFIG_CLOSE_TIMEOUT  = "event.processor.close.timeout";

    public static final int DEFAULT_QUEUE_CAPACITY    = 1000;
    public static final int DEFAULT_EMPTY_COUNT = 2;
    public static final int DEFAULT_BATCH_SIZE        = 10;
    public static final long DEFAULT_BATCH_INTERVAL   = TimeUnit.SECONDS.toMillis(30);
    public static final long DEFAULT_TIMEOUT_INTERVAL = TimeUnit.SECONDS.toMillis(5);

    private static final Object SHUTDOWN_SIGNAL = new Object();
    private static final Object FLUSH_SIGNAL    = new Object();

    private final BlockingQueue<Object> eventQueue;
    private final EventHandler eventHandler;

    final int batchSize;
    final long flushInterval;
    final long timeoutMillis;
    private final ExecutorService executor;
    private final NotificationCenter notificationCenter;

    private Future<?> future;
    private boolean isStarted = false;

    private BatchEventProcessor(BlockingQueue<Object> eventQueue, EventHandler eventHandler, Integer batchSize, Long flushInterval, Long timeoutMillis, ExecutorService executor, NotificationCenter notificationCenter) {
        this.eventHandler = eventHandler;
        this.eventQueue = eventQueue;
        this.batchSize = batchSize;
        this.flushInterval = flushInterval;
        this.timeoutMillis = timeoutMillis;
        this.notificationCenter = notificationCenter;
        this.executor = executor;
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
            future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while awaiting termination.");
            Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
            logger.error("Timeout exceeded attempting to close for {} ms", timeoutMillis);
        } finally {
            isStarted = false;
            tryClose(eventHandler);
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

    public void flush() throws InterruptedException {
        eventQueue.put(FLUSH_SIGNAL);
    }

    public class EventConsumer implements Runnable {
        private LinkedList<UserEvent> currentBatch = new LinkedList<>();
        private long deadline = System.currentTimeMillis() + flushInterval;

        @Override
        public void run() {
            try {
                int emptyCount = 0;

                while (true) {
                    if (System.currentTimeMillis() > deadline) {
                        logger.debug("Deadline exceeded flushing current batch.");
                        flush();
                        deadline = System.currentTimeMillis() + flushInterval;
                    }

                    long timeout = deadline - System.currentTimeMillis();
                    Object item = emptyCount > DEFAULT_EMPTY_COUNT ? eventQueue.take() : eventQueue.poll(timeout, TimeUnit.MILLISECONDS);

                    if (item == null) {
                        logger.debug("Empty item after waiting flush interval. Flushing.");
                        emptyCount++;
                        continue;
                    }

                    emptyCount = 0;

                    if (item == SHUTDOWN_SIGNAL) {
                        logger.info("Received shutdown signal.");
                        break;
                    }

                    if (item == FLUSH_SIGNAL) {
                        logger.debug("Received flush signal.");
                        flush();
                        continue;
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

            // Projects should match
            if (!currentConfig.getProjectId().equals(newConfig.getProjectId())) {
                return true;
            }

            // Revisions should match
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
        private Integer batchSize = PropertyUtils.getInteger(CONFIG_BATCH_SIZE, DEFAULT_BATCH_SIZE);
        private Long flushInterval = PropertyUtils.getLong(CONFIG_BATCH_INTERVAL, DEFAULT_BATCH_INTERVAL);
        private Long timeoutMillis = PropertyUtils.getLong(CONFIG_CLOSE_TIMEOUT, DEFAULT_TIMEOUT_INTERVAL);
        private ExecutorService executor = null;
        private NotificationCenter notificationCenter = null;

        /**
         * {@link EventHandler} implementation used to dispatch events to Optimizely.
         */
        public Builder withEventHandler(EventHandler eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        /**
         * EventQueue is the underlying BlockingQueue used to buffer events before being added to the batch payload.
         */
        public Builder withEventQueue(BlockingQueue<Object> eventQueue) {
            this.eventQueue = eventQueue;
            return this;
        }

        /**
         * BatchSize is the maximum number of events contained within a single event batch.
         */
        public Builder withBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        /**
         * FlushInterval is the maximum duration, in milliseconds, that an event will remain in flight before
         * being flushed to the event dispatcher.
         */
        public Builder withFlushInterval(Long flushInterval) {
            this.flushInterval = flushInterval;
            return this;
        }

        /**
         * ExecutorService used to execute the {@link EventConsumer} thread.
         */
        public Builder withExecutor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Timeout is the maximum time to wait for the EventProcessor to close.
         */
        public Builder withTimeout(long duration, TimeUnit timeUnit) {
            this.timeoutMillis = timeUnit.toMillis(duration);
            return this;
        }

        /**
         * NotificationCenter used to notify when event batches are flushed.
         */
        public Builder withNotificationCenter(NotificationCenter notificationCenter) {
            this.notificationCenter = notificationCenter;
            return this;
        }

        public BatchEventProcessor build() {
            return build(true);
        }

        public BatchEventProcessor build(boolean shouldStart) {
            if (batchSize < 0) {
                logger.warn("Invalid batchSize of {}, Defaulting to {}", batchSize, DEFAULT_BATCH_SIZE);
                batchSize = DEFAULT_BATCH_SIZE;
            }

            if (flushInterval < 0) {
                logger.warn("Invalid flushInterval of {}, Defaulting to {}", flushInterval, DEFAULT_BATCH_INTERVAL);
                flushInterval = DEFAULT_BATCH_INTERVAL;
            }

            if (timeoutMillis < 0) {
                logger.warn("Invalid timeoutMillis of {}, Defaulting to {}", timeoutMillis, DEFAULT_TIMEOUT_INTERVAL);
                timeoutMillis = DEFAULT_TIMEOUT_INTERVAL;
            }

            if (eventHandler == null) {
                throw new IllegalArgumentException("EventHandler was not configured");
            }

            if (executor == null) {
                final ThreadFactory threadFactory = Executors.defaultThreadFactory();
                executor = Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = threadFactory.newThread(runnable);
                    thread.setDaemon(true);
                    return thread;
                });
            }

            BatchEventProcessor batchEventProcessor = new BatchEventProcessor(eventQueue, eventHandler, batchSize, flushInterval, timeoutMillis, executor, notificationCenter);

            if (shouldStart) {
                batchEventProcessor.start();
            }

            return batchEventProcessor;
        }
    }
}

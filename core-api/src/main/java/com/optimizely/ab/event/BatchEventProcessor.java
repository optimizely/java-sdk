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

import com.optimizely.ab.event.internal.EventFactory;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.event.internal.payload.Visitor;
import com.optimizely.ab.internal.PropertyUtils;
import com.optimizely.ab.notification.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.*;

/**
 * BatchEventProcessor is a batched implementation of the {@link EventProcessor}
 *
 * Events passed to the BatchEventProcessor are immediately added to a BlockingQueue.
 *
 * The BatchEventProcessor maintains a single consumer thread that pulls events off of
 * the BlockingQueue and buffers them for either a configured batch size or for a
 * maximum duration before they resulting LogEvent is sent to the NotificationManager.
 */
public class BatchEventProcessor implements EventProcessor, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(BatchEventProcessor.class);

    public static final String CONFIG_BATCH_SIZE     = "event.processor.batch.size";
    public static final String CONFIG_BATCH_DURATION = "event.processor.batch.duration.ms";

    public static final int  DEFAULT_BATCH_SIZE     = 50;
    public static final long DEFAULT_BATCH_DURATION = TimeUnit.MINUTES.toMillis(1);

    private static final EventBatch SHUTDOWN_SIGNAL = new EventBatch();

    private final NotificationManager<LogEvent> notificationManager = new NotificationManager<>();
    private final BlockingQueue<EventBatch> eventQueue;

    private final int batchSize;
    private final long batchDuration;
    private final ExecutorService executor;

    private Future<?> future;
    private boolean isStarted = false;

    // TODO use a builder.
    public BatchEventProcessor() {
        this(new ArrayBlockingQueue<>(1000));
    }

    public BatchEventProcessor(BlockingQueue<EventBatch> eventQueue) {
        this(eventQueue, null, null, Executors.newSingleThreadExecutor());
    }

    public BatchEventProcessor(BlockingQueue<EventBatch> eventQueue, Integer batchSize, Integer batchDuration, ExecutorService executor) {
        this.eventQueue = eventQueue;
        this.batchSize = batchSize == null ? PropertyUtils.getInteger(CONFIG_BATCH_SIZE, DEFAULT_BATCH_SIZE) : batchSize;
        this.batchDuration = batchDuration == null ? PropertyUtils.getLong(CONFIG_BATCH_DURATION, DEFAULT_BATCH_DURATION) : batchDuration;

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
        process(SHUTDOWN_SIGNAL);
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while awaiting termination.");
            Thread.currentThread().interrupt();
        }
    }

    public int addEventHandler(EventHandler handler) {
        return addHandler(handler::dispatchEvent);
    }

    public int addHandler(NotificationHandler<LogEvent> handler) {
        logger.debug("Adding notification handler: {}", handler);
        return notificationManager.addHandler(handler);
    }

    public void process(EventBatch eventBatch) {

        if (executor.isShutdown()) {
            logger.warn("Executor shutdown, not accepting tasks.");
            return;
        }

        if (!eventQueue.offer(eventBatch)) {
            logger.warn("Payload not accepted by the queue. Current size: {}", eventQueue.size());
        }
    }

    public class EventConsumer implements Runnable {
        private EventBatch currentBatch = null;
        private long lastFlushTimeMillis = System.currentTimeMillis();

        @Override
        public void run() {
            try {
                while(true) {
                    if (System.currentTimeMillis() - lastFlushTimeMillis > batchDuration) {
                        logger.debug("Deadline exceeded flushing current batch.");
                        flush();
                    }

                    EventBatch item = eventQueue.poll(50, TimeUnit.MILLISECONDS);
                    if (item == null) {
                        logger.info("Empty item, sleeping for 500ms.");
                        Thread.sleep(50);
                        continue;
                    }

                    if (item == SHUTDOWN_SIGNAL) {
                        logger.info("Received shutdown signal.");
                        break;
                    }

                    addToBatch(item);
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

        private void addToBatch(EventBatch eventBatch) {
            if (currentBatch == null) {
                currentBatch = eventBatch;
            } else {
                // Separate batches based on config revision.
                if (!currentBatch.getRevision().equals(eventBatch.getRevision())) {
                    flush();
                    currentBatch = eventBatch;
                    return;
                }

                for (Visitor visitor: eventBatch.getVisitors()) {
                    currentBatch.getVisitors().add(visitor);
                }
            }

            if (currentBatch.getVisitors().size() >= batchSize) {
                flush();
            }
        }

        private void flush() {
            lastFlushTimeMillis = System.currentTimeMillis();

            if (currentBatch == null) {
                return;
            }

            LogEvent logEvent = new LogEvent(LogEvent.RequestMethod.POST, EventFactory.EVENT_ENDPOINT,
                Collections.emptyMap(), currentBatch);

            notificationManager.send(logEvent);
            currentBatch = null;
        }
    }
}

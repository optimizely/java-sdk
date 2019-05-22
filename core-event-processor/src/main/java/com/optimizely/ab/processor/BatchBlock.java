/**
 *    Copyright 2019, Optimizely Inc. and contributors
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
package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Buffers elements to be asynchronously emitted in batches using a {@link BlockingQueue}.
 *
 * Starts a single {@link Thread} that consumes elements from the queue and manages a temporary buffer that is flushed
 * once it reaches the configured size or age, whichever comes first.
 *
 * When an element is received (via {@link #post}) it is inserted into the {@link BlockingQueue} or waits until space
 * becomes available.
 *
 * NOTE: this class could easily be generalized to any sort of queueing/buffering behavior using a BlockingQueue, as the
 * batching-specific aspects are fully encapsulated in the ConsumerTask.
 *
 * @param <T> the type of input elements and output element batches
 */
public class BatchBlock<T> extends Blocks.Source<T> implements ProcessorBlock<T, T> {
    private static final Logger logger = LoggerFactory.getLogger(BatchBlock.class);

    private final BlockingQueue<Object> queue;
    private final ThreadFactory threadFactory;
    private final ConsumerTask<T> consumerTask;

    private volatile boolean running = false;
    private Thread consumeThread;

    public static <T> BatchBlock<T> create(BatchOptions options, ThreadFactory threadFactory) {
        ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(1024);
        return new BatchBlock<>(queue, new BatchingConsumerTask<>(queue, options), threadFactory);
    }

    protected BatchBlock(BlockingQueue<Object> queue, ConsumerTask consumerTask, ThreadFactory threadFactory) {
        this.queue = Assert.notNull(queue, "queue");
        this.consumerTask = Assert.notNull(consumerTask, "consumerTask");
        this.threadFactory = (threadFactory != null) ? threadFactory : Executors.defaultThreadFactory();
    }

    @Override
    protected void afterStart() {
        if (running) {
            return;
        }

        logger.debug("Starting batching thread...");

        consumeThread = threadFactory.newThread(consumerTask);
        consumeThread.setDaemon(true);
        consumeThread.start();

        running = true;

        logger.debug("Started batching thread");
    }

    @Override
    protected void beforeStop() {
        if (!running) {
            return;
        }

        running = false; // disable queue inserts

        logger.debug("Stopping consumer thread...");
        consumerTask.shutdown();
        try {
            consumeThread.join(30_000L);
        } catch (InterruptedException e) {
            logger.warn("Consumer did not shutdown cleanly", e);
        }
        logger.debug("Stopped consumer thread");
    }

    @Override
    public synchronized void linkTo(TargetBlock<? super T> target, LinkOptions options) {
        consumerTask.flushTo(target);
        super.linkTo(target, options);
    }

    @Override
    public void post(@Nonnull T element) {
        if (!running) {
            logger.warn("Rejected event: {}", element);
            return;
        }

        try {
            queue.put(element);
        } catch (InterruptedException e) {
            logger.warn("Dropped event: {}", element, e);
        }
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void flush() {
        consumerTask.flush();
    }

    protected interface ConsumerTask<T> extends Runnable {
        /**
         * Initiates the shutdown of this consumer task.
         */
        void shutdown();

        /**
         * Initiates a flush of any pending elements and related state held in this consumer task.
         */
        void flush();

        /**
         * Sets the {@link TargetBlock} where consumed elements are to be emitted.
         */
        void flushTo(TargetBlock<? super T> sink);
    }

    /**
     * Continuously consumes from {@link BlockingQueue} to a temporary buffer.
     *
     * Buffer is flushed when the max size or age is reached, whichever comes first.
     */
    private static class BatchingConsumerTask<T> implements ConsumerTask<T> {
        private final BatchOptions options;

        // sentinel values queued to wake-up consumer (a.k.a poison pills)
        private final Object shutdownSignal = new Object();
        private final Object flushSignal = new Object();
        private List<T> buffer;
        private Long deadline;
        private BlockingQueue<Object> queue;
        private TargetBlock<? super T> sink;

        private BatchingConsumerTask(BlockingQueue<Object> queue, BatchOptions options) {
            this.options = Assert.notNull(options, "options");
            this.queue = queue;
        }

        @Override
        public void run() {
            logger.trace("Consumer started (options: {})", options);

            try {
                Long timeout = null;
                while (!Thread.currentThread().isInterrupted()) {
                    // if there's a deadline (non-empty buffer), block with timeout,
                    // otherwise, block until next element/signal
                    Object obj = (timeout != null) ?
                        queue.poll(timeout, TimeUnit.MILLISECONDS) :
                        queue.take();

                    if (obj == shutdownSignal) {
                        break;
                    }

                    if (obj == flushSignal) {
                        logger.trace("Flushing buffer (reason: external request)");
                        drainBuffer();
                        continue;
                    }

                    if (obj != null) {
                        try {
                            //noinspection unchecked
                            T element = (T) obj;

                            // Initialize the buffer/deadline when first element is received
                            if (buffer == null) {
                                initBuffer();
                            }

                            buffer.add(element);

                            if (buffer.size() >= options.getMaxSize()) {
                                logger.trace("Flushing buffer (reason: max size)");
                                drainBuffer();
                            }
                        } catch (ClassCastException e) {
                            logger.warn("Unexpected item in queue: {}", obj, e);
                        }
                    }

                    if (deadline != null) {
                        timeout = deadline - Blocks.clock().millis();
                        if (timeout < 0) {
                            logger.trace("Flushing buffer (reason: max age)");
                            drainBuffer();
                            timeout = null;
                        }
                    } else {
                        timeout = null;
                    }
                }
            } catch (InterruptedException e) {
                logger.debug("Consumer interrupted", e);
            }

            logger.trace("Consumer is shutting down...");
            if (options.shouldFlushOnShutdown()) {
                drainBuffer();
            }
            logger.trace("Consumer has been shutdown");
        }

        @Override
        public void shutdown() {
            try {
                queue.put(shutdownSignal);
            } catch (InterruptedException e) {
                logger.warn("Interrupted while shutting down", e);
            }
        }

        @Override
        public void flush() {
            try {
                queue.put(flushSignal);
            } catch (InterruptedException e) {
                logger.warn("Interrupted while flushing", e);
            }
        }

        @Override
        public void flushTo(TargetBlock<? super T> sink) {
            this.sink = sink;
        }

        /**
         * Initializes buffer-related state.
         */
        private void initBuffer() {
            buffer = BatchOptions.hasMaxSize(options) ?
                new ArrayList<>(options.getMaxSize()) :
                new LinkedList<>();

            deadline = BatchOptions.hasMaxAge(options) ?
                Blocks.clock().millis() + options.getMaxAge() :
                null;
        }

        /**
         * Emits current items in buffer and clears buffer-related state.
         *
         * @return items in buffer
         */
        private void drainBuffer() {
            if (sink != null && buffer != null && !buffer.isEmpty()) {
                logger.trace("Emitting batch of {} items", buffer.size());
                sink.postBatch(buffer);
            }

            buffer = null;
            deadline = null;
        }
    }
}

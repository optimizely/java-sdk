package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class QueueBlock<T> extends Blocks.Source<T> implements ProcessorBlock<T, T> {
    private static final Logger logger = LoggerFactory.getLogger(QueueBlock.class);

    private final BatchOptions options;
    private final BlockingQueue<Object> queue;
    private final ThreadFactory threadFactory;

    // sentinel values to send control signals to consumer (a.k.a poison pill)
    private final Object shutdownSignal = new Object();
    private final Object flushSignal = new Object();

    private Clock clock = Clock.systemUTC();

    private volatile boolean running = false;

    public QueueBlock(BatchOptions options, ThreadFactory threadFactory) {
        this(defaultQueue(), options, threadFactory);
    }

    public QueueBlock(BlockingQueue<Object> queue, BatchOptions options, ThreadFactory threadFactory) {
        this.queue = Assert.notNull(queue, "queue");
        this.options = Assert.notNull(options, "options");
        this.threadFactory = (threadFactory != null) ? threadFactory : Executors.defaultThreadFactory();
    }

    static BlockingQueue<Object> defaultQueue() {
        return new ArrayBlockingQueue<>(1024);
    }

    @Override
    protected synchronized void afterStart() {
        synchronized (this) {
            if (running) {
                return;
            }

            logger.debug("Starting batching thread...");
            ConsumeTask consumeTask = new ConsumeTask();
            Thread consumeThread = threadFactory.newThread(consumeTask);
            consumeThread.setDaemon(true);
            consumeThread.start();

            running = true;

            logger.debug("Started batching thread");
        }
    }

    @Override
    protected void beforeStop() {
        synchronized (this) {
            if (!running) {
                return;
            }

            synchronized (shutdownSignal) {
                logger.debug("Stopping consumer thread...");
                running = false; // stop producing further
                try {
                    queue.put(shutdownSignal);

                    // wait until consumer has acknowledged shutdown signal
                    shutdownSignal.wait(1000L);

                    logger.debug("Stopped consumer thread");
                } catch (InterruptedException e) {
                    logger.warn("Consumer did not shutdown cleanly", e);
                }
            }
        }
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

    void flush() {
        queue.offer(flushSignal);
    }

    int size() {
        return queue.size();
    }

    int remaininCapacity() {
        return queue.remainingCapacity();
    }

    public BatchOptions getOptions() {
        return options;
    }

    // for test-purposes only
    void setClock(Clock clock) {
        this.clock = clock;
    }

    private class ConsumeTask implements Runnable {
        private List<T> buffer;
        private Long deadline;

        @Override
        public void run() {
            try {
                Long timeout = null;
                while (running && !Thread.currentThread().isInterrupted()) {
                    // if there's a deadline (non-empty buffer), block with timeout,
                    // otherwise, block until next element/signal
                    Object obj = (timeout != null) ?
                        queue.poll(timeout, TimeUnit.MILLISECONDS) :
                        queue.take();

                    if (obj == shutdownSignal) {
                        break;
                    }

                    if (obj == flushSignal) {
                        drainBuffer();
                        continue;
                    }

                    if (obj != null) {
                        if (buffer == null) {
                            initBuffer();
                        }

                        //noinspection unchecked
                        buffer.add((T) obj);

                        if (buffer.size() >= options.getMaxSize()) {
                            logger.trace("Flushing - buffer full");
                            drainBuffer();
                        }
                    }

                    if (deadline != null) {
                        timeout = deadline - clock.millis();
                        if (timeout < 0) {
                            logger.trace("Flushing - buffer expired");
                            drainBuffer();
                            timeout = null;
                        }
                    } else {
                        timeout = null;
                    }
                }
            } catch (InterruptedException e) {
                logger.debug("Consumer interrupted", e);
            } finally {
                running = false;
            }

            logger.trace("Consumer is shutting down...");

            if (options.shouldFlushOnShutdown()) {
                drainBuffer();
            }

            // acknowledge the shutdown signal
            synchronized (shutdownSignal) {
                shutdownSignal.notify();
            }

            logger.trace("Consumer has been shutdown");
        }

        /**
         * Initializes buffer-related state.
         */
        private void initBuffer() {
            buffer = BatchOptions.hasMaxSize(options) ?
                new ArrayList<>(options.getMaxSize()) :
                new LinkedList<>();

            deadline = BatchOptions.hasMaxAge(options) ?
                clock.millis() + options.getMaxAge() :
                null;
        }

        /**
         * Emits current items in buffer and clears buffer-related state.
         *
         * @return items in buffer
         */
        private void drainBuffer() {
            if (buffer != null) {
                logger.trace("Emitting batch of {} items", buffer.size());
                emitBatch(buffer);
            }

            buffer = null;
            deadline = null;
        }
    }
}

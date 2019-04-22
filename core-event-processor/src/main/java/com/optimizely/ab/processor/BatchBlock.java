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
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

/**
 * Emits buffers in the form of a {@link Collection}, each which contains a maximum number of elements or when the
 * buffer reaches a maximum age.
 *
 * Buffers are maintained and flushed downstream on a separate, dedicated thread.
 *
 * Supports throttling the number of batches in-flight.
 *
 * @param <T> the type of input elements and output element batches
 * @see BatchTask
 */
public class BatchBlock<T> extends Blocks.Source<T> implements ProcessorBlock<T, T> {
    private static final Logger logger = LoggerFactory.getLogger(BatchBlock.class);

    private final BatchOptions options;
    private final Executor executor;
    private final Semaphore inFlightPermits;
    private final Supplier<BatchTask<T, ?>> batchTaskFactory;

    /**
     * Mutex to serialize modifications to buffer
     */
    private final Object mutex = new Object();

    /**
     * Task that holds & flushes the buffer for latest batch
     */
    private BatchTask<T, ?> currentTask;

    BatchBlock(BatchOptions options, ThreadFactory threadFactory) {
        this(options, createExecutor(threadFactory));
    }

    BatchBlock(BatchOptions options, Executor executor) {
        this.options = Assert.notNull(options, "options");
        this.executor = Assert.notNull(executor, "executor");
        this.inFlightPermits = createInFlightSemaphore(options);
        this.batchTaskFactory = createBatchTaskFactory(options);
    }

    private static Executor createExecutor(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            return Executors.newCachedThreadPool();
        }
        return Executors.newCachedThreadPool(threadFactory);
    }

    /**
     * @param element the element to push
     */
    @Override
    public void post(@Nonnull T element) {
        synchronized (mutex) {
            collect(element);
        }
    }

    /**
     * @param elements the elements to put
     */
    @Override
    public void postBatch(@Nonnull Collection<? extends T> elements) {
        synchronized (mutex) {
            for (T element : elements) {
                collect(element);
            }
        }
    }

    @Override
    public void beforeStop() {
        if (options.shouldFlushOnShutdown()) {
            try {
                flush();
            } catch (InterruptedException e) {
                logger.warn("Interrupted while flushing before shutdown");
            }
        }
    }

    /**
     * Forces in-flight work to be flushed synchronously.
     */
    public void flush() throws InterruptedException {
        synchronized (mutex) {
            // prevent new batches from being started
            if (currentTask != null) {
                currentTask.close();
            }

            logger.debug("Flush started");
            Integer n = options.getMaxBatchInFlight();
            if (inFlightPermits != null && n != null) {
                inFlightPermits.acquire(n);
                inFlightPermits.release(n);
            }
            logger.debug("Flush complete");
        }
    }

    public BatchOptions getOptions() {
        return options;
    }

    protected Supplier<BatchTask<T, ?>> createBatchTaskFactory(BatchOptions opts) {
        // TODO should this live on BatchOptions?
        final Supplier<Collection<T>> bufferSupplier = BatchOptions.hasMaxSize(opts) ?
            () -> new ArrayList<>(opts.getMaxSize()) :
            ArrayList::new;

        return () -> new BatchTask<>(
            bufferSupplier,
            this::onBufferClose,
            opts.getMaxSize(),
            opts.getMaxAge());
    }


    protected Semaphore createInFlightSemaphore(BatchOptions config) {
        Integer n = config.getMaxBatchInFlight();
        return (n != null) ? new Semaphore(n) : null;
    }

    /**
     * Callback that consumes elements from a closed {@link BatchTask}
     */
    protected void onBufferClose(Collection<? extends T> batch) {
        if (batch == null || batch.isEmpty()) {
            logger.debug("Batch completed without any elements"); // i.e. forced
            return;
        }

        logger.debug("Batch completed with {} elements: {}", batch.size(), batch);

        emitBatch(batch);
    }

    /**
     * Inserts the specified element into a batch, initializing a new batch if current
     * batch does not exist or does not accept it (max age/size), and blocking if
     * necessary to acquire in-flight batch permit.
     *
     * Assumes mutex for {@link #currentTask} is being held by caller.
     */
    protected void collect(T element) {
        // try to add to current buffer
        if (currentTask == null || !currentTask.offer(element)) {
            // otherwise, start new one
            BatchTask<T, ?> successorTask = batchTaskFactory.get();

            if (!successorTask.offer(element)) {
                // This can only happen if element itself is flawed and cannot be added
                // to batch (even a new one), so we must drop it.
                logger.warn("Discarding {} rejected by {}", element, successorTask);
                // could potentially return here; currently utilizes successorTask
            }

            currentTask = successorTask;

            // TODO consider executing (acquiring in-flight permit) after releasing task mutex
            executeBatch(successorTask);
        }
    }

    /**
     * Submits task to executor when permitted by the configured number of in-flight batches.
     */
    protected void executeBatch(final BatchTask<T, ?> task) {
        Runnable runnable = task;

        if (inFlightPermits != null) {
            try {
                inFlightPermits.acquire();

                logger.debug("Acquired in-flight permit (available: {}, queued: {})",
                    inFlightPermits.availablePermits(),
                    inFlightPermits.getQueueLength());
            } catch (InterruptedException e) {
                logger.debug("Interrupted while waiting for in-flight batch permit");
                Thread.currentThread().interrupt();
                throw new BlockException(e.getMessage(), e);
            }

            // release permit when run complete
            runnable = () -> {
                try {
                    task.run();
                } finally {
                    inFlightPermits.release();
                }
            };
        }

        try {
            executor.execute(runnable);
        } catch (RejectedExecutionException e) {
            // release permit if never ran
            if (inFlightPermits != null) {
                inFlightPermits.release();
            }
            throw new BlockException("Unable to start batching task", e);
        }
    }
}

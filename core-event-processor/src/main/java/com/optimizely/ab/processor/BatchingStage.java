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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * An asynchronous stage that buffers input elements and emits downstream in batches.
 *
 * @param <T> the type of input elements and output element batches
 * @see BatchingTask
 */
public class BatchingStage<T> implements Stage<T, T> {
    private final BatchingConfig config;

    public BatchingStage(BatchingConfig config) {
        this.config = Assert.notNull(config, "config");
    }

    @Nonnull
    @Override
    public Processor<T> getProcessor(@Nonnull Processor<? super T> sink) {
        return new BatchingProcessor<>(config, sink);
    }

    /**
     * A thread-safe processor that facilitates the execution of {@link BatchingTask}.
     *
     * Batches are flushed to the sink on thread assigned by {@link Executor}.
     *
     * Supports throttling the number of in-flight batches being flushed to the sink.
     *
     * @param <E>
     */
    public static class BatchingProcessor<E> extends AbstractProcessor<E, E> {
        public static final Logger logger = LoggerFactory.getLogger(BatchingProcessor.class);

        /**
         * Config settings for this batchTask
         */
        private final BatchingConfig config;

        /**
         * The executor service for the batching tasks.
         */
        private final Executor executor;

        /**
         * Permits controlling number of in-flight batches
         */
        private final Semaphore inFlightBatches;

        /**
         * Task that holds & flushes the buffer for latest batch
         */
        private BatchingTask<E, ?> batchTask;

        /**
         * Mutex to serialize modifications to buffer
         */
        private final Object mutex = new Object();

        public BatchingProcessor(
            BatchingConfig config,
            Processor<? super E> sink
        ) {
            super(Assert.notNull(sink, "sink"));

            this.config = Assert.notNull(config, "config");
            this.executor = Assert.notNull(config.getExecutor(), "executor");
            this.inFlightBatches = createInFlightSemaphore(config);
        }

        /**
         * @param element the element to push
         */
        @Override
        public void process(@Nonnull E element) {
            synchronized (mutex) {
                addToBuffer(element);
            }
        }

        /**
         * @param elements the elements to put
         */
        @Override
        public void processBatch(@Nonnull Collection<? extends E> elements) {
            synchronized (mutex) {
                for (E element : elements) {
                    addToBuffer(element);
                }
            }
        }

        @Override
        protected boolean beforeStop() {
            if (config.isFlushOnShutdown()) {
                try {
                    flush();
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while flushing before shutdown");
                }
            }
            return true;
        }

        /**
         * Forces in-flight work to be flushed synchronously.
         */
        public void flush() throws InterruptedException {
            synchronized (mutex) {
                // prevent new batches from being started
                if (batchTask != null) {
                    batchTask.close();
                }

                logger.debug("Flush started");
                Integer n = config.getMaxInFlightBatches();
                if (inFlightBatches != null && n != null) {
                    inFlightBatches.acquire(n);
                    inFlightBatches.release(n);
                }
                logger.debug("Flush complete");
            }
        }

        public BatchingConfig getConfig() {
            return config;
        }

        protected Collection<E> createBuffer() {
            return (config.getMaxBatchSize() != null) ?
                new ArrayList<>(config.getMaxBatchSize()) :
                new ArrayList<>();
        }

        protected Semaphore createInFlightSemaphore(BatchingConfig config) {
            Integer n = config.getMaxInFlightBatches();
            return (n != null) ? new Semaphore(n) : null;
        }

        protected void onBufferClose(Collection<E> elements) {
            if (elements == null || elements.isEmpty()) {
                logger.debug("Batch completed with empty buffer");
                return;
            }

            logger.debug("Batch completed with {} elements: {}", elements.size(), elements);
            emitBatch(elements);
        }

        // assumes lock is being held
        /**
         * This can only happen if element itself is flawed and
         * cannot be added to batch (even a new one), so we must drop it.
         * Can be overridden for different behavior.
         */
        protected void onElementDiscarded(E element) {
            logger.warn("Discarding element rejected by buffer", element);
        }

        private void addToBuffer(E element) {
            // try to add to current buffer
            if (batchTask == null || !batchTask.offer(element)) {
                // otherwise, start a new buffer
                BatchingTask<E, ?> next = new BatchingTask<>(
                    this::createBuffer,
                    this::onBufferClose,
                    config.getMaxBatchSize(),
                    config.getMaxBatchAge());

                if (!next.offer(element)) {
                    onElementDiscarded(element);
                }

                execute(next);

                batchTask = next;
            }
        }

        /**
         * Submits task to executor when permitted by the configured number of in-flight batches.
         */
        protected void execute(final BatchingTask<E, ?> task) {
            Runnable runnable = task;

            if (inFlightBatches != null) {
                try {
                    inFlightBatches.acquire();

                    logger.debug("Acquired in-flight permit (available: {}, queued: {})",
                        inFlightBatches.availablePermits(),
                        inFlightBatches.getQueueLength());
                } catch (InterruptedException e) {
                    logger.debug("Interrupted while waiting for in-flight batch permit");
                    Thread.currentThread().interrupt();
                    throw new ProcessorException(e.getMessage(), e);
                }

                // release permit when run complete
                runnable = () -> {
                    try {
                        task.run();
                    } finally {
                        inFlightBatches.release();
                    }
                };
            }

            try {
                executor.execute(runnable);
            } catch (RejectedExecutionException e) {
                // release permit if never ran
                if (inFlightBatches != null) {
                    inFlightBatches.release();
                }
                throw new ProcessorException("Unable to start batching task", e);
            }
        }
    }
}

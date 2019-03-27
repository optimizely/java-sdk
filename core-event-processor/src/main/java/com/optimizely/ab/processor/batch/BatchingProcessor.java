package com.optimizely.ab.processor.batch;

import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.processor.AbstractProcessor;
import com.optimizely.ab.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

public class BatchingProcessor<E> extends AbstractProcessor<E, E> {
    public static final Logger logger = LoggerFactory.getLogger(BatchingProcessor.class);

    /**
     * Config settings for this batchTask
     */
    private final BatchingProcessorConfig config;

    /**
     * The executor service for the batching tasks.
     */
    private final Executor executor;

    /**
     * Permits controlling number of in-flight batches
     */
    private final Semaphore inFlightBatches;

    /**
     * Reference to current batch
     */
    private BatchBufferTask<E, ?> batchTask;

    /**
     * Mutex to serialize modifying batchTask
     */
    private final Object mutex = new Object();

    public BatchingProcessor(
        BatchingProcessorConfig config,
        Processor<? super E> sink
    ) {
        super(Assert.notNull(sink, "sink"));

        Assert.isTrue(config.getMaxInFlightBatches() > 0, "maxInFlightBatches > 0");

        this.config = Assert.notNull(config, "config");
        this.executor = Assert.notNull(config.getExecutor(), "executor");
        this.inFlightBatches = new Semaphore(config.getMaxInFlightBatches(), true);
    }

    public BatchingProcessorConfig getConfig() {
        return config;
    }

    /**
     * @param element the element to push
     * @throws BatchBufferTask.BatchInterruptedException if interrupted while inserting into buffer
     */
    @Override
    public void process(@Nonnull E element) {
        synchronized (mutex) {
            add(element);
        }
    }

    /**
     * @param elements the elements to put
     * @throws BatchBufferTask.BatchInterruptedException if interrupted while inserting into buffer
     */
    @Override
    public void processBatch(@Nonnull Collection<? extends E> elements) {
        synchronized (mutex) {
            for (E element : elements) {
                add(element);
            }
        }
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
            inFlightBatches.acquire(config.getMaxInFlightBatches());
            inFlightBatches.release(config.getMaxInFlightBatches());
            logger.debug("Flush complete");
        }
    }

    private void add(E element) {
        if (batchTask != null && batchTask.offer(element)) {
            return;
        }

        BatchBufferTask<E, ?> next = new BatchBufferTask<>(
            this::createBuffer,
            this::onBufferClose,
            config.getMaxBatchSize(),
            config.getMaxBatchAge());

        if (!next.offer(element)) {
            // This can only happen if element itself is flawed and
            // cannot be added to batch (even a new one), so we must drop it.
            throw new BatchingProcessorException("Failed to add to batch: " + element);
        }

        execute(next);

        batchTask = next;
    }

    /**
     * Creates fresh buffer task and submits to executor.
     */
    private void execute(BatchBufferTask<E, ?> task) {
        try {
            inFlightBatches.acquire();

            logger.debug("Acquired in-flight permit (available: {}, queued: {})",
                inFlightBatches.availablePermits(),
                inFlightBatches.getQueueLength());
        } catch (InterruptedException e) {
            logger.debug("Interrupted while waiting for in-flight batch permit");
            throw new BatchingProcessorException(e.getMessage(), e);
        }

        try {
            executor.execute(() -> {
                try {
                    task.run();
                } finally {
                    inFlightBatches.release();
                }
            });
        } catch (RejectedExecutionException e) {
            inFlightBatches.release();
            throw new BatchingProcessorException("Unable to start batching task", e);
        }
    }

    private Collection<E> createBuffer() {
        return new ArrayList<>(config.getMaxBatchSize());
    }

    private void onBufferClose(Collection<E> elements) {
        if (elements != null && !elements.isEmpty()) {
            logger.debug("Batch completed with {} elements: {}", elements.size(), elements);
            emitBatch(elements);
        } else {
            logger.debug("Batch completed with empty buffer");
        }
    }
}

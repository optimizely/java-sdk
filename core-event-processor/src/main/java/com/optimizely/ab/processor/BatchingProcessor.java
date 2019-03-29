package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Emits buffers in the form of a {@link Collection}, each which contains a maximum number of elements or when teh
 * buffer reaches a maximum age.
 *
 * Buffers are maintained and flushed downstream on a separate, dedicated thread.
 *
 * Supports throttling the number of batches in-flight.
 *
 * @param <T> the type of input elements and output element batches
 * @see BatchingConfig
 * @see BatchingTask
 */
public class BatchingProcessor<T> extends StageProcessor<T, T> {
    public static final Logger logger = LoggerFactory.getLogger(BatchingProcessor.class);

    private final BatchingConfig config;

    /**
     * Permits controlling number of in-flight batches
     */
    private final Semaphore inFlightBatches;

    /**
     * Task that holds & flushes the buffer for latest batch
     */
    private BatchingTask<T, ?> batchTask;

    /**
     * Mutex to serialize modifications to buffer
     */
    private final Object mutex = new Object();

    public BatchingProcessor(BatchingConfig config) {
        super();

        this.config = Assert.notNull(config, "config");
        this.inFlightBatches = createInFlightSemaphore(config);
    }

    /**
     * @param element the element to push
     */
    @Override
    public void process(@Nonnull T element) {
        synchronized (mutex) {
            collect(element);
        }
    }

    /**
     * @param elements the elements to put
     */
    @Override
    public void processBatch(@Nonnull Collection<? extends T> elements) {
        synchronized (mutex) {
            for (T element : elements) {
                collect(element);
            }
        }
    }

    @Override
    protected boolean beforeStop(long timeout, TimeUnit unit) {
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

    protected Collection<T> createBuffer() {
        return (config.getMaxBatchSize() != null) ?
            new ArrayList<>(config.getMaxBatchSize()) :
            new ArrayList<>();
    }

    protected Semaphore createInFlightSemaphore(BatchingConfig config) {
        Integer n = config.getMaxInFlightBatches();
        return (n != null) ? new Semaphore(n) : null;
    }

    protected void onBufferClose(Collection<T> elements) {
        if (elements == null || elements.isEmpty()) {
            logger.debug("Batch completed with empty buffer");
            return;
        }

        logger.debug("Batch completed with {} elements: {}", elements.size(), elements);
        emitBatch(elements);
    }

    // assumes lock is being held

    /**
     * This can only happen if element itself is flawed and cannot be added to batch (even a new one), so we must drop it.
     * Can be overridden for different behavior.
     */
    protected void onElementDiscarded(T element) {
        logger.warn("Discarding element rejected by buffer", element);
    }

    private void collect(T element) {
        // try to add to current buffer
        if (batchTask == null || !batchTask.offer(element)) {
            // otherwise, start a new buffer
            BatchingTask<T, ?> next = new BatchingTask<>(
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
    protected void execute(final BatchingTask<T, ?> task) {
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
                throw new ProcessingException(e.getMessage(), e);
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
            config.getExecutor().execute(runnable);
        } catch (RejectedExecutionException e) {
            // release permit if never ran
            if (inFlightBatches != null) {
                inFlightBatches.release();
            }
            throw new ProcessingException("Unable to start batching task", e);
        }
    }
}

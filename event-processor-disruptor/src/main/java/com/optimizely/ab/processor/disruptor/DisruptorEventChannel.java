package com.optimizely.ab.processor.disruptor;

import com.lmax.disruptor.BatchStartAware;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceReportingEventHandler;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.optimizely.ab.common.LifecycleAware;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.processor.EventChannel;
import com.optimizely.ab.processor.EventSink;
import com.optimizely.ab.common.message.MutableMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.Flushable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class DisruptorEventChannel<U> implements EventChannel<U>, LifecycleAware {
    private static final Logger logger = LoggerFactory.getLogger(DisruptorEventChannel.class);
    private static final int MIN_RINGBUFFER_SIZE = 128;
    private static final int SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS = 50;
    private static final int MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN = 200;

    private final EventSink<U> sink;
    private final EventTranslatorOneArg<MutableMessage<U>, U> translator;

    private volatile Disruptor<MutableMessage<U>> disruptor;
    private BufferBatch<U> batch;

    /**
     * @param waitStrategy strategy used by consumer when buffer is empty
     */
    public DisruptorEventChannel(
        EventSink<U> sink,
        int batchMaxSize,
        int ringBufferSize,
        ThreadFactory threadFactory,
        WaitStrategy waitStrategy,
        ExceptionHandler<Object> exceptionHandler
    ) {
        Assert.notNull(threadFactory, "threadFactory");
        Assert.notNull(waitStrategy, "waitStrategy");

        if (batchMaxSize < 1) {
            logger.warn("Invalid batch size {}, using minimum size 1", batchMaxSize);
            batchMaxSize = 1;
        }

        if (ringBufferSize < MIN_RINGBUFFER_SIZE) {
            logger.warn("Invalid ring buffer size {}, using minimum size {}", ringBufferSize, MIN_RINGBUFFER_SIZE);
            ringBufferSize = MIN_RINGBUFFER_SIZE;
        }

        this.sink = Assert.notNull(sink, "sink");
        this.batch = new BufferBatch<>(sink, batchMaxSize);

        Disruptor<MutableMessage<U>> disruptor = new Disruptor<>(
            MutableMessage::new,
            ringBufferSize,
            threadFactory,
            ProducerType.MULTI,
            waitStrategy);

        disruptor.setDefaultExceptionHandler(exceptionHandler);
        disruptor.handleEventsWith(new RingBufferEventHandler());

        this.disruptor = disruptor;

        // Adapter for Disruptor's EventTranslator interface
        this.translator = (MutableMessage<U> message, long seq, U event) -> message.set(event, null);
    }

    @Override
    public void onStart() {
        Assert.state(sink != null, "sink has not been configured");

        try {
            disruptor.start();
        } catch (IllegalStateException e) {
            logger.debug("channel already started");
        }
    }

    @Override
    public boolean onStop(long timeout, TimeUnit unit) {
        Disruptor disruptor = this.disruptor; // read volatile once
        if (disruptor == null) {
            logger.trace("Not started. Skipping...");
            return true;
        }

        // Prevent new events from being published
        this.disruptor = null;

        final RingBuffer ringBuffer = disruptor.getRingBuffer();
        // Calling Disruptor.shutdown() will wait until all enqueued events are fully processed,
        // but this waiting happens in a busy-spin. To avoid (postpone) wasting CPU,
        // we sleep in short chunks, up to 10 seconds, waiting for the ringbuffer to drain.
        for (int i = 0; hasBacklog(ringBuffer) && i < MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN; i++) {
            try {
                Thread.sleep(SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS); // give up the CPU for a while
            } catch (final InterruptedException e) {
                // ignored
            }
        }

        try {
            // Busy-spins until all events currently in the disruptor have been processed, or timeout
            disruptor.shutdown(timeout, unit);
        } catch (final TimeoutException e) {
            logger.warn("Shutdown timed out after {} {}", timeout, unit);
            disruptor.halt(); // give up on remaining log events, if any
        }

        logger.trace("disruptor has been shutdown");

        return hasBacklog(ringBuffer);
    }


    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public boolean isFull() {
        Disruptor<?> disruptor = this.disruptor;
        Assert.state(disruptor != null, "not started");
        return this.disruptor.getRingBuffer().hasAvailableCapacity(1);
    }

    /**
     * Delivers the specified message to the channel, waiting if necessary for space to become available.
     * <p>
     * This method is thread-safe; it can be called from any thread.
     */
    @Override
    public void put(@Nonnull U item) {
        Disruptor<MutableMessage<U>> disruptor = this.disruptor;
        if (disruptor == null) {
            logger.warn("Ignoring event (not initialized or has been stopped): {}", item);
            return;
        }

        //noinspection ConstantConditions
        if (item == null) {
            logger.trace("Ignoring null from publish()");
            return;
        }

        disruptor.publishEvent(translator, item);
    }

    @Override
    public void putBatch(@Nonnull Collection<? extends U> items) {
        Disruptor<MutableMessage<U>> disruptor = this.disruptor;
        if (disruptor == null) {
            logger.warn("Ignoring events. Not initialized or has been stopped");
            return;
        }

        ArrayList<U> arr = new ArrayList<>();
        for (final U item : items) {
            if (item != null) {
                arr.add(item);
            }
        }

        //noinspection unchecked
        disruptor.publishEvents(translator, (U[]) arr.toArray());
    }

    /**
     * @return true if the specified ring buffer has unhandled events, otherwise false
     */
    private static boolean hasBacklog(final RingBuffer<?> ringBuffer) {
        return !ringBuffer.hasAvailableCapacity(ringBuffer.getBufferSize());
    }

    /**
     * Mutable batch of items that flushes to a sink at the specified size limit.
     * Operations are not thread-safe and should only be
     */
    private static class BufferBatch<E> implements Flushable {
        private final EventSink<E> sink;
        private final int limit;

        private List<E> items;

        private BufferBatch(EventSink<E> sink, int limit) {
            this.sink = sink;
            this.limit = limit;
        }

        public void init(int sizeHint) {
            Assert.state(items == null, "items is non-null");
            logger.trace("Initializing a buffer for {} items", sizeHint);
            items = new ArrayList<>(sizeHint);
        }

        public boolean add(E item) {
            if (items == null) {
                logger.trace("Starting new batch");
                items = new ArrayList<>(limit);
            }

            if (item == null) {
                return false;
            }

            return items.add(item);
        }

        public int remaining() {
            return limit - items.size();
        }

        public boolean shouldFlush() {
            return items != null && remaining() <= 0;
        }

        @Override
        public void flush() {
            if (size() == 0) {
                return;
            }

            logger.trace("Flushing {} items", items.size());
            sink.putBatch(items);
            items = null;
        }

        public int size() {
            return items != null ? items.size() : 0;
        }
    }

    /**
     * Performs queue consumption in separate thread.
     */
    private class RingBufferEventHandler implements SequenceReportingEventHandler<MutableMessage<U>>, BatchStartAware, LifecycleAware {
        private Sequence sequenceCallback;

        @Override
        public void setSequenceCallback(Sequence sequenceCallback) {
            this.sequenceCallback = sequenceCallback;
        }

        /**
         * @param endOfBatch flag to indicate if this is the last event in a batch from the {@link RingBuffer}
         */
        @Override
        public void onEvent(MutableMessage<U> holder, long sequence, boolean endOfBatch) {
            final U event = holder.getValue();

            batch.add(event);

            // clear references from holder
            holder.clear();

            // release when batch is full or ring buffer is empty.
            // in future, we can consider adding a delay for a (potentially) larger batch
            // always consider endOfBatch, even if event wasn't added
            if (endOfBatch || batch.shouldFlush()) {
                batch.flush();
                sequenceCallback.set(sequence);
            }
        }

        @Override
        public void onBatchStart(long batchSize) {
            logger.trace("Starting to process {} items in buffer", batchSize);
            try {
                batch.init(Math.toIntExact(batchSize));
            } catch (ArithmeticException | IllegalStateException e) {
                // ignore
            }
        }

        @Override
        public void onStart() {
            logger.trace("Disruptor handler started");
        }

        @Override
        public boolean onStop(long timeout, TimeUnit unit) {
            logger.trace("Disruptor handler stopped");
            return true;
        }
    }

    public enum LoggingExceptionHandler implements ExceptionHandler<Object> {
        INSTANCE {
            @Override
            public void handleEventException(Throwable ex, long sequence, Object value) {
                logger.error("Exception processing sequence[{}]: {}", sequence, value, ex);
            }

            @Override
            public void handleOnStartException(Throwable ex) {
                logger.error("Exception during onStart()", ex);
            }

            @Override
            public void handleOnShutdownException(Throwable ex) {
                logger.error("Exception during onShutdown()", ex);
            }
        };

        @SuppressWarnings("unchecked")
        public static <S> ExceptionHandler<S> getInstance() {
            return (ExceptionHandler<S>) INSTANCE;
        }
    }
}

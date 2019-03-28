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
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.common.lifecycle.LifecycleAware;
import com.optimizely.ab.common.message.MutableMessage;
import com.optimizely.ab.processor.Stage;
import com.optimizely.ab.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.Flushable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Factory for {@link BufferProcessor} instances.
 *
 * @param <T> the type of elements to in buffer
 */
public class DisruptorBufferStage<T> implements Stage<T, T> {
    private static final Logger logger = LoggerFactory.getLogger(DisruptorBufferStage.class);

    private final Config config;

    public DisruptorBufferStage(Config config) {
        this.config = Assert.notNull(config, "config");
    }

    @Nonnull
    @Override
    public Processor<T> getProcessor(@Nonnull Processor<? super T> sink) {
        return new BufferProcessor<>(config, sink);
    }

    public interface Config {
        int getBatchMaxSize();
        int getCapacity();
        ThreadFactory getThreadFactory();
        WaitStrategy getWaitStrategy();
        ExceptionHandler<Object> getExceptionHandler();
    }

    static class BufferProcessor<U> implements LifecycleAware, Processor<U> {
        static final int MIN_RINGBUFFER_SIZE = 128;
        static final int SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS = 50;
        static final int MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN = 200;

        private final Processor<? super U> sink;
        private final EventTranslatorOneArg<MutableMessage<U>, U> translator;

        private volatile Disruptor<MutableMessage<U>> disruptor;
        private BufferBatch<U> batch;

        BufferProcessor(Config config, Processor<? super U> sink) {
            this(sink,
                config.getBatchMaxSize(),
                config.getCapacity(),
                config.getThreadFactory(),
                config.getWaitStrategy(),
                config.getExceptionHandler());
        }

        /**
         * @param waitStrategy strategy used by consumer when buffer is empty
         */
        protected BufferProcessor(
            Processor<? super U> sink,
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
                logger.debug("Already started");
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


        /**
         * Delivers the specified message to the operator, waiting if necessary for space to become available.
         * <p>
         * This method is thread-safe; it can be called from any thread.
         */
        @Override
        public void process(@Nonnull U element) {
            Disruptor<MutableMessage<U>> disruptor = this.disruptor;
            if (disruptor == null) {
                logger.warn("Ignoring event (not initialized or has been stopped): {}", element);
                return;
            }

            //noinspection ConstantConditions
            if (element == null) {
                logger.trace("Ignoring null from publish()");
                return;
            }

            disruptor.publishEvent(translator, element);
        }

        @Override
        public void processBatch(@Nonnull Collection<? extends U> elements) {
            Disruptor<MutableMessage<U>> disruptor = this.disruptor;
            if (disruptor == null) {
                logger.warn("Ignoring events. Not initialized or has been stopped");
                return;
            }

            ArrayList<U> arr = new ArrayList<>();
            for (final U element : elements) {
                if (element != null) {
                    arr.add(element);
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
         * Mutable batch of elements that flushes to a sink at the specified size limit.
         * Operations are not thread-safe and should only be
         */
        private static class BufferBatch<E> implements Flushable {
            private final Processor<? super E> sink;
            private final int limit;

            private List<E> elements;

            private BufferBatch(Processor<? super E> sink, int limit) {
                this.sink = sink;
                this.limit = limit;
            }

            public void init(int sizeHint) {
                Assert.state(elements == null, "elements is non-null");
                logger.trace("Initializing a buffer for {} elements", sizeHint);
                elements = new ArrayList<>(sizeHint);
            }

            public boolean add(E element) {
                if (elements == null) {
                    logger.trace("Starting new batch");
                    elements = new ArrayList<>(limit);
                }

                if (element == null) {
                    return false;
                }

                return elements.add(element);
            }

            public int remaining() {
                return limit - elements.size();
            }

            public boolean shouldFlush() {
                return elements != null && remaining() <= 0;
            }

            @Override
            public void flush() {
                if (size() == 0) {
                    return;
                }

                logger.trace("Flushing {} elements", elements.size());
                sink.processBatch(elements);
                elements = null;
            }

            public int size() {
                return elements != null ? elements.size() : 0;
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
                logger.trace("Starting to process {} elements in buffer", batchSize);
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

    }

    /**
     * Handles exceptions by logging errors
     */
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

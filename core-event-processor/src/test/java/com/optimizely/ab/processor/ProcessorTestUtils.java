package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class ProcessorTestUtils {

    /**
     * 0. Processor linked to {@link TransferBlock}
     * 1. Starts processor
     * 2. Runs handler
     * 3. Stops processor
     * 4. Returns {@link TransferBlock}
     */
    static <T, R, P extends ProcessorBlock<T, R>> TransferBlock<R> testWith(
        P processor,
        TestHandler<T, R, P> handler
    ) throws Exception {
        TransferBlock<R> target = transferBlock(processor);

        processor.onStart();
        try {
            handler.run(processor, target);
        } finally {
            processor.onStop();
        }

        return target;
    }

    static <T> TransferBlock<T> transferBlock(SourceBlock<T> source) {
        TransferBlock<T> inst = new TransferBlock<>();
        source.linkTo(inst);
        return inst;
    }

    interface TestHandler<T, R, P extends ProcessorBlock<T, R>> {
        void run(P processor, TransferBlock<R> target) throws Exception;
    }

    /**
     * Sink that captures useful information (timing, method) into a {@link BlockingQueue}.
     */
    public static class TransferBlock<T> implements TargetBlock<T> {
        private final BlockingQueue<Envelope<T>> queue;

        private Duration defaultTimeout = Duration.ofSeconds(30L);

        public TransferBlock() {
            this(new LinkedBlockingQueue<>());
        }

        public TransferBlock(BlockingQueue<Envelope<T>> queue) {
            this.queue = Assert.notNull(queue, "queue");
        }

        @Override
        public void post(@Nonnull T element) {
            try {
                queue.put(Envelope.forSingle(element));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void postBatch(@Nonnull Collection<? extends T> elements) {
            try {
                queue.put(Envelope.forBatch(elements));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public Envelope<T> await() throws InterruptedException, TimeoutException {
            return await(defaultTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public Envelope<T> await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            Envelope<T> output = queue.poll(timeout, unit);
            if (output == null) {
                throw new TimeoutException("Timeout waiting for output after " + timeout + " " + unit);
            }
            return output;
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }

        public int size() {
            return queue.size();
        }

        public BlockingQueue<Envelope<T>> getQueue() {
            return queue;
        }

        public void setDefaultTimeout(Duration defaultTimeout) {
            this.defaultTimeout = defaultTimeout;
        }

        /**
         * Holds *either* a single element or a batch of elements.
         */
        public static class Envelope<T> {
            public enum Method {SINGLE, BATCH}

            private final Instant timestamp = Instant.now();
            private T element;
            private Collection<? extends T> elements;
            private Method method;

            static <T> Envelope<T> forSingle(T element) {
                Envelope t = new Envelope<>();
                t.element = element;
                t.method = Method.SINGLE;
                return t;
            }

            static <T> Envelope<T> forBatch(Collection<? extends T> elements) {
                Envelope t = new Envelope<>();
                t.elements = elements;
                t.method = Method.BATCH;
                return t;
            }

            public T single() {
                Assert.state(element != null, "Transfer method was not " + Method.SINGLE);
                return element;
            }

            public Collection<? extends T> batch() {
                Assert.state(elements != null, "Transfer method was not " + Method.BATCH);
                return elements;
            }

            public Method method() {
                return method;
            }

            public Instant timestamp() {
                return timestamp;
            }

            @Override
            public String toString() {
                final StringBuffer sb = new StringBuffer("Envelope{");
                sb.append("timestamp=").append(timestamp);
                sb.append(", method=").append(method);
                if (element != null) sb.append(", element=").append(element);
                if (elements != null) sb.append(", elements=").append(elements);
                sb.append('}');
                return sb.toString();
            }
        }

    }
}

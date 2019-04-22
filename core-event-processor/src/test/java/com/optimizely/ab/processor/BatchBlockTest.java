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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.optimizely.ab.common.internal.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.fail;

@Ignore("TODO fix CI flakey-ness")
public class BatchBlockTest {
    public static final Logger logger = LoggerFactory.getLogger(BatchBlockTest.class);

    private ExecutorService executor;
    private List<Collection<Object>> batches;
    private AtomicInteger batchCount;
    private ThreadFactory threadFactory;

    @Before
    public void setUp() {
        threadFactory = new ThreadFactoryBuilder().setNameFormat(BatchBlockTest.class.getSimpleName() + "-%d").build();
        executor = Executors.newCachedThreadPool(threadFactory);
        batchCount = new AtomicInteger();
        batches = Collections.synchronizedList(new ArrayList<>());
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testPost_maxBatchSize1() throws Exception {
        BatchBlock<Object> buffer = batchingQueue(config -> config
            .maxBatchSize(1)
            .maxBatchAge(Duration.ofDays(1))); // wont reach timeout

        buffer.post("one");
        assertBatchCount(1, buffer);
        buffer.post("two");
        assertBatchCount(2, buffer);
        buffer.post("three");
        assertBatchCount(3, buffer);
        assertThat(batches.get(0), equalTo(newArrayList("one")));
        assertThat(batches.get(1), equalTo(newArrayList("two")));
        assertThat(batches.get(2), equalTo(newArrayList("three")));
    }

    @Test
    public void testPost_maxBatchSize2() throws Exception {
        BatchBlock<Object> buffer = batchingQueue(config -> config
            .maxBatchSize(2)
            .maxBatchAge(Duration.ofDays(1))); // wont reach timeout

        buffer.post("one");
        buffer.post("two");
        assertBatchCount(1, buffer);
        buffer.post("three");
        buffer.post("four");
        assertBatchCount(2, buffer);
        assertThat(batches.get(0), equalTo(newArrayList("one", "two")));
        assertThat(batches.get(1), equalTo(newArrayList("three", "four")));
    }

    @Test
    public void testPost_timeout() throws Exception {
        BatchBlock<Object> buffer = batchingQueue(config -> config
            .maxBatchSize(10)
            .maxBatchAge(Duration.ofMillis(500)));

        buffer.post(0);
        assertBatchCount(1, buffer);
        assertThat(batches.get(0), equalTo(newArrayList(0)));

        for (int i = 1; i <= 10; i++) {
            buffer.post(i);
        }
        assertBatchCount(2, buffer);
        assertThat(batches.get(1), equalTo(newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)));
    }

    @Test
    public void testPostBatch_maxBatchSize1() {
        BatchBlock<Object> buffer = batchingQueue(config -> config
            .maxBatchSize(1)
            .maxBatchAge(Duration.ofDays(1))); // wont reach timeout

        buffer.postBatch(Lists.newArrayList("one", "two", "three"));
        assertBatchCount(3, buffer);
        assertThat(batches.get(0), equalTo(newArrayList("one")));
        assertThat(batches.get(1), equalTo(newArrayList("two")));
        assertThat(batches.get(2), equalTo(newArrayList("three")));
    }

    @Test
    public void testPostBatch_maxBatchSize2() {
        BatchBlock<Object> buffer = batchingQueue(config -> config
            .maxBatchSize(2)
            .maxBatchAge(Duration.ofDays(1))); // wont reach timeout

        buffer.postBatch(Lists.newArrayList("one", "two", "three", "four"));
        assertBatchCount(2, buffer);
        assertThat(batches.get(0), equalTo(newArrayList("one", "two")));
        assertThat(batches.get(1), equalTo(newArrayList("three", "four")));
    }

    @Test
    public void testPostBatch_timeout() {
        BatchBlock<Object> buffer = batchingQueue(config -> config
            .maxBatchSize(2)
            .maxBatchAge(Duration.ofMillis(500)));

        buffer.postBatch(Lists.newArrayList("one", "two", "three"));
        assertBatchCount(2, buffer);
        assertThat(batches.get(0), equalTo(newArrayList("one", "two")));
        assertThat(batches.get(1), equalTo(newArrayList("three")));
    }

    @Test
    public void testFlushingOpenBatch() throws Exception {
        // flush before bounds are reached
        BatchBlock<Object> buffer = batchingQueue(config -> config
            .maxBatchSize(100)
            .maxBatchAge(Duration.ofHours(1)));

        for (int i = 0; i < 10; i++) {
            buffer.post(i);
        }

        buffer.flush();

        assertThat(batchCount.get(), equalTo(1));
        assertThat(batches.get(0).size(), equalTo(10));

        // idempotent
        buffer.flush();
        assertThat(batchCount.get(), equalTo(1));
    }

    @Test
    public void testUnboundedAge() throws Exception {
        BatchBlock<Object> buffer = batchingQueue(config -> config
            .maxBatchAge(null));

        buffer.post("one");
        assertBatchCount(0, buffer);
        buffer.post("two");
        assertBatchCount(0, buffer);
        buffer.post("three");
        assertBatchCount(0, buffer);
        buffer.flush();
        assertBatchCount(1, buffer);
        assertThat(batches.get(0), equalTo(newArrayList("one", "two", "three")));
    }

    @Test
    public void testFlushOnShutdown_true() throws Exception {
        BatchBlock<Object> buffer = batchingQueue(config -> config
            .maxBatchAge(null)
            .flushOnShutdown(true));

        buffer.post("one");
        Thread.sleep(250L);
        assertThat(batchCount.get(), equalTo(0));
        buffer.onStop();
        assertBatchCount(1, buffer);
    }

    @Test
    public void testFlushOnShutdown_false() throws Exception {
        BatchBlock<Object> buffer = batchingQueue(config -> config
            .maxBatchAge(null)
            .flushOnShutdown(false));

        buffer.post("one");
        Thread.sleep(250L);
        assertThat(batchCount.get(), equalTo(0));
        buffer.onStop();
        assertBatchCount(0, buffer);
    }

    @Test
    public void testMaxInflight1() {
        testMaxInflight(1, 1);
    }

    @Test
    public void testMaxInflight3() {
        testMaxInflight(3, 10);
    }

    private void testMaxInflight(int maxInflight, int maxBatchSize) {
        int numProducers = maxInflight + 1;
        ExecutorService producerExecutor = Executors.newFixedThreadPool(numProducers, new ThreadFactoryBuilder()
            .setNameFormat("test-producer-%d")
            .build());

        InstrumentedExecutorService executor = new InstrumentedExecutorService(this.executor);

        try {
            BatchOptions opts = BatchOptions.builder()
                .maxBatchSize(maxBatchSize)
                .maxBatchAge(Duration.ofDays(1)) // wont reach timeout
                .maxBatchInFlight(maxInflight);

            BatchBlock<Object> block = new BatchBlock<>(opts, executor);

            link(delayConsumer(Duration.ofMillis(250)), block);

            List<Object> expected = Collections.synchronizedList(new ArrayList<>());

            // produce batches in parallel
            CompletableFuture[] futures = IntStream.range(0, numProducers)
                .mapToObj(n ->
                    CompletableFuture.runAsync(() -> {
                        for (int i = 0; i < maxBatchSize; i++) {
                            String element = String.format("%s-%s", n, i);
                            block.post(element);
                            logger.debug("processed {}", element);
                            expected.add(element);
                            try {
                                Thread.sleep(10L);
                            } catch (InterruptedException e) {
                                // ignore
                            }
                        }
                    }, producerExecutor))
                .toArray(CompletableFuture[]::new);

            try {
                logger.debug("Waiting for producers to stop");
                CompletableFuture.allOf(futures).get();
                logger.debug("Producers done");
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            assertBatchCount(numProducers, block);

            assertThat(executor.getExecuteCount().intValue(), equalTo(numProducers));
            assertThat(executor.getExecutingCount().intValue(), equalTo(0));
            assertThat(executor.getExecutingMax().intValue(), equalTo(maxInflight));
        } finally {
            producerExecutor.shutdownNow();
        }
    }

    private Consumer<Collection<Object>> delayConsumer(Duration delay) {
        return c -> {
            logger.info("Sleeping for {}", delay);
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            recordBatch(c);
        };
    }

    private BatchBlock<Object> batchingQueue(Consumer<BatchOptions.Builder> configure) {
        return batchingQueue(configure, this::recordBatch);
    }

    private BatchBlock<Object> batchingQueue(
        Consumer<BatchOptions.Builder> configure,
        Consumer<Collection<Object>> consumer
    ) {
        return batchingQueue(configure, consumer, threadFactory);
    }

    private BatchBlock<Object> batchingQueue(
        Consumer<BatchOptions.Builder> configure,
        Consumer<Collection<Object>> consumer,
        ThreadFactory threadFactory
    ) {
        BatchOptions.Builder<Object> opts = BatchOptions.builder();

        // per-test config
        configure.accept(opts);

        BatchBlock<Object> block = Blocks.batch(opts, threadFactory);

        link(consumer, block);

        return block;
    }

    private void link(Consumer<Collection<Object>> consumer, BatchBlock<Object> processor) {
        processor.linkTo(new TargetBlock<Object>() {
            @Override
            public void post(@Nonnull Object element) {
                fail("Not expecting process to be invoked");
            }

            @SuppressWarnings("unchecked")
            @Override
            public void postBatch(@Nonnull Collection<?> elements) {
                consumer.accept((Collection<Object>) elements);
            }
        });
    }

    // receives batching output
    private void recordBatch(Collection<Object> batch) {
        int n = batchCount.incrementAndGet();
        batches.add(batch);
        logger.info("Received batch #{}: {}", n, batch);
    }

    // waits for the configured max age
    private void assertBatchCount(int n, BatchBlock processor) {
        BatchOptions options = processor.getOptions();

        long timeout = 1000L;
        if (BatchOptions.hasMaxAge(options)) {
            // some leniency for context switching
            timeout = Math.max(options.getMaxAge() + 100L, timeout);
        }

        Integer actual = await()
            .atMost(timeout, MILLISECONDS)
            .untilAtomic(batchCount, greaterThanOrEqualTo(n));

        assertThat(actual, equalTo(n));
    }

    private static class InstrumentedExecutorService extends AbstractExecutorService {
        private final ExecutorService delegate;
        private final LongAdder executeCount;
        private final LongAdder executingCount;
        private final LongAccumulator executingMax;

        InstrumentedExecutorService(ExecutorService delegate) {
            this.delegate = Assert.notNull(delegate, "delegate");
            this.executeCount = new LongAdder();
            this.executingCount = new LongAdder();
            this.executingMax = new LongAccumulator(Long::max, Long.MIN_VALUE);
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public void execute(final Runnable command) {
            executeCount.increment();
            delegate.execute(() -> {
                executingCount.increment();
                executingMax.accumulate(executingCount.longValue());
                logger.info("executingCount={}, executingMax={}", executingCount, executingMax);
                try {
                    command.run();
                } finally {
                    executingCount.decrement();
                }
            });
        }

        public LongAdder getExecuteCount() {
            return executeCount;
        }

        public Long getExecutingCount() {
            return executingCount.longValue();
        }

        public Long getExecutingMax() {
            return executingMax.longValue();
        }
    }
}
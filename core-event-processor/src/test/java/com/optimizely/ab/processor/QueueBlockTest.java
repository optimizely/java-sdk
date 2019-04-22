package com.optimizely.ab.processor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.optimizely.ab.processor.ProcessorTestUtils.TransferBlock;
import com.optimizely.ab.processor.ProcessorTestUtils.TransferBlock.Envelope;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.newArrayList;
import static com.optimizely.ab.processor.ProcessorTestUtils.testWith;
import static com.optimizely.ab.processor.ProcessorTestUtils.transferBlock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

public class QueueBlockTest {
    public static final Logger logger = LoggerFactory.getLogger(QueueBlockTest.class);

    private ThreadFactory threadFactory;

    @Before
    public void setUp() {
        threadFactory = new ThreadFactoryBuilder().setNameFormat(QueueBlockTest.class.getSimpleName() + "-%d").build();
    }

    @Test
    public void testStartStop() throws Exception {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch stopLatch = new CountDownLatch(1);
        final AtomicInteger threadCounter = new AtomicInteger();

        ThreadFactory tf = consumerTask -> {
            threadCounter.incrementAndGet();

            // Decorate the consumer task with start/stop latches
            Runnable runnable = () -> {
                startLatch.countDown();
                try {
                    consumerTask.run();
                } finally {
                    stopLatch.countDown();
                }
            };

            return this.threadFactory.newThread(runnable);
        };

        BatchOptions options = BatchOptions.builder()
            .maxBatchSize(10)
            .maxBatchAge(Duration.ofMinutes(1))
            .flushOnShutdown(true)
            .build();

        QueueBlock<Object> inst = new QueueBlock<>(options, tf);

        TransferBlock<Object> sink = transferBlock(inst);

        inst.onStart();
        assertTrue("Consumer thread started", startLatch.await(10, TimeUnit.SECONDS));

        inst.post(1L);
        assertThat(sink.isEmpty(), equalTo(true));

        inst.onStop();
        assertTrue("Consumer thread stopped", stopLatch.await(10, TimeUnit.SECONDS));
        assertThat(threadCounter.get(), equalTo(1));

        // flushes on shutdown
        assertThat(sink.await(10, TimeUnit.SECONDS).batch(), equalTo(newArrayList(1L)));
        assertThat(sink.isEmpty(), equalTo(true));
    }

    @Test
    public void testPost_maxBatchSize1() throws Exception {
        testWith(
            new QueueBlock<>(
                BatchOptions.builder()
                    .maxBatchSize(1)
                    .maxBatchAge(Duration.ofDays(1)) // wont reach timeout
                    .build(),
                threadFactory),
            (source, sink) -> {
                source.post("one");
                source.post("two");
                source.post("three");

                assertThat(sink.await().batch(), equalTo(newArrayList("one")));
                assertThat(sink.await().batch(), equalTo(newArrayList("two")));
                assertThat(sink.await().batch(), equalTo(newArrayList("three")));
                assertThat(sink.isEmpty(), equalTo(true));
            });
    }

    @Test
    public void testPost_maxBatchSize2() throws Exception {
        testWith(
            new QueueBlock<>(
                BatchOptions.builder()
                    .maxBatchSize(2)
                    .maxBatchAge(Duration.ofDays(1)) // wont reach timeout
                    .build(),
                threadFactory),
            (source, sink) -> {
                source.post("one");
                source.post("two");
                source.post("three");
                source.post("four");

                assertThat(sink.await().batch(), equalTo(newArrayList("one", "two")));
                assertThat(sink.await().batch(), equalTo(newArrayList("three", "four")));
                assertThat(sink.isEmpty(), equalTo(true));
            });
    }

    @Test
    public void testPost_timeout() throws Exception {
        testWith(
            new QueueBlock<>(
                BatchOptions.builder()
                    .maxBatchSize(10)
                    .maxBatchAge(Duration.ofMillis(500))
                    .build(),
                threadFactory),
            (source, target) -> {
                Instant start = Instant.now();
                source.post(0);

                Envelope<Object> batch1 = target.await();
                logger.info("Batch received after {}ms", Duration.between(start, batch1.timestamp()).toMillis());

                assertThat(batch1.batch(), equalTo(newArrayList(0)));
                assertThat(target.isEmpty(), equalTo(true));

                // fill a batch; should be received before timeout
                for (int i = 1; i <= 10; i++) {
                    source.post(i);
                }

                Envelope<Object> batch2 = target.await();
                assertThat(batch2.batch(),
                    equalTo(newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)));
                assertThat(target.isEmpty(), equalTo(true));
            });
    }
}
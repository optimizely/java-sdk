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

import com.optimizely.ab.processor.ProcessorTestUtils.TransferBlock;
import com.optimizely.ab.processor.ProcessorTestUtils.TransferBlock.Envelope;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.newArrayList;
import static com.optimizely.ab.processor.ProcessorTestUtils.testWith;
import static com.optimizely.ab.processor.ProcessorTestUtils.transferBlock;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class BatchBlockTest {
    public static final Logger logger = LoggerFactory.getLogger(BatchBlockTest.class);

    @Rule
    public TestName name = new TestName();

    private ThreadFactory threadFactory;

    @Before
    public void setUp() {
        final AtomicInteger threadCount = new AtomicInteger();
        threadFactory = runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setName(String.format("%s-%s", name.getMethodName(), threadCount.incrementAndGet()));
            return thread;
        };
    }

    @Test(timeout = 60_000L)
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

        BatchBlock<Object> queue = BatchBlock.create(options, tf);

        assertThat(threadCounter.get(), equalTo(0));

        TransferBlock<Object> sink = transferBlock(queue);

        queue.onStart();
        startLatch.await();
        logger.info("Started consumer");

        queue.post(1L);
        await().until(queue::isEmpty);

        logger.info("Stopping consumer");
        queue.onStop();
        stopLatch.await();
        assertThat(threadCounter.get(), equalTo(1));

        // flushes on shutdown
        assertThat(sink.await().batch(), equalTo(newArrayList(1L)));
        assertThat(sink.isEmpty(), equalTo(true));
    }

    @Test
    public void testPost_maxBatchSize1() throws Exception {
        testWith(
            BatchBlock.create(
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
            BatchBlock.create(
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
        Duration expectedAge = Duration.ofMillis(500);

        testWith(
            BatchBlock.create(
                BatchOptions.builder().maxBatchAge(expectedAge).build(),
                threadFactory),
            (source, target) -> {
                Instant start = Instant.now();
                source.post(0);

                Envelope<Object> batch1 = target.await();
                Duration actualAge = Duration.between(start, batch1.timestamp());
                logger.info("Batch received after {}ms", actualAge.toMillis());

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
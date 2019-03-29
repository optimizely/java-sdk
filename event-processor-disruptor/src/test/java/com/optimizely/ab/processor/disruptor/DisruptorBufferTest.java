/**
 * Copyright 2019, Optimizely Inc. and contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.processor.disruptor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.optimizely.ab.common.lifecycle.LifecycleAware;
import com.optimizely.ab.processor.Processor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DisruptorBufferTest {
    private static final Logger logger = LoggerFactory.getLogger(DisruptorBufferTest.class);

    private DisruptorBuffer<TestEvent> buffer;
    private List<List<TestEvent>> output;
    private AtomicInteger outputCount;

    private int batchMaxSize = 1;
    private int bufferCapacity = 256;
    private ThreadFactory threadFactory;
    private WaitStrategy waitStrategy;

    @Before
    public void setUp() throws Exception {
        output = new ArrayList<>();
        outputCount = new AtomicInteger(0);

        batchMaxSize = 1;
        bufferCapacity = 256;
        threadFactory = new ThreadFactoryBuilder()
            .setNameFormat(getClass().getSimpleName() + "-%d")
            .setThreadFactory(DaemonThreadFactory.INSTANCE)
            .build();
        waitStrategy = new BlockingWaitStrategy();
    }

    private Processor<TestEvent> createDefaultSink() {
        return new Processor<TestEvent>() {
            @Override
            public void process(TestEvent element) {
                fail("put() not expected to be called");
            }

            @Override
            public void processBatch(Collection<? extends TestEvent> batch) {
                //noinspection unchecked
                output.add((List<TestEvent>) batch);

                outputCount.incrementAndGet();
            }
        };
    }

    private IntSummaryStatistics summarizeBatchSizes() {
        return output.stream().collect(Collectors.summarizingInt(List::size));
    }

    @After
    public void tearDown() throws Exception {
        if (buffer != null) {
            LifecycleAware.stop(buffer, 30, TimeUnit.SECONDS);
        }
    }

    private void init() {
        DisruptorBufferConfig config = DisruptorBufferConfig.builder()
            .batchMaxSize(batchMaxSize)
            .capacity(bufferCapacity)
            .threadFactory(threadFactory)
            .waitStrategy(waitStrategy)
            .exceptionHandler(DisruptorBuffer.LoggingExceptionHandler.getInstance())
            .build();

        buffer = new DisruptorBuffer<>(config);

        buffer.configure(createDefaultSink());

        buffer.onStart();
    }

    @Test
    public void batchMaxSize_1() throws Exception {
        batchMaxSize = 1;
        init();

        for (int i = 0; i < 12; i++) {
            buffer.process(TestEvent.next());
        }

        await().atMost(1, TimeUnit.SECONDS)
            .untilAtomic(outputCount, greaterThanOrEqualTo(12));

        assertThat(summarizeBatchSizes().getMax(), equalTo(1));
    }

    @Test
    public void batchMaxSize_2() throws Exception {
        batchMaxSize = 2;
        init();

        for (int i = 0; i < 12; i++) {
            buffer.process(TestEvent.next());
        }

        await().atMost(1, TimeUnit.SECONDS)
            .untilAtomic(outputCount, greaterThanOrEqualTo(6));

        assertThat(summarizeBatchSizes().getMax(), lessThanOrEqualTo(2));
        assertEquals(12, summarizeBatchSizes().getSum());
    }

    @Test
    public void batchMaxSize_3() throws Exception {
        batchMaxSize = 3;
        init();

        for (int i = 0; i < 12; i++) {
            buffer.process(TestEvent.next());
        }

        await().atMost(1, TimeUnit.SECONDS)
            .untilAtomic(outputCount, greaterThanOrEqualTo(4));

        assertThat(summarizeBatchSizes().getMax(), lessThanOrEqualTo(3));
        assertEquals(12, summarizeBatchSizes().getSum());
    }

    /**
     * Does not wait until full batch is reached
     */
    @Test
    public void batchEarlyRelease() throws Exception {
        batchMaxSize = 1000;
        init();

        for (int i = 0; i < 12; i++) {
            buffer.process(TestEvent.next());
        }

        await().atMost(1, TimeUnit.SECONDS)
            .until(() -> summarizeBatchSizes().getSum() >= 12);

        assertEquals(12, summarizeBatchSizes().getSum());
    }

    private static class TestEvent {
        private static AtomicInteger SERIAL = new AtomicInteger();
        final int id;

        static TestEvent next() {
            return new TestEvent(SERIAL.incrementAndGet());
        }

        TestEvent(int id) {
            this.id = id;
        }
    }
}
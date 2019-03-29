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
package com.optimizely.ab.processor.internal;

import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.optimizely.ab.common.callback.Callback;
import com.optimizely.ab.common.lifecycle.LifecycleAware;
import com.optimizely.ab.common.plugin.Plugin;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.event.internal.payload.Visitor;
import com.optimizely.ab.processor.Processor;
import com.optimizely.ab.processor.Stage;
import com.optimizely.ab.processor.disruptor.DisruptorBuffer;
import com.optimizely.ab.processor.disruptor.DisruptorBufferConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

public class EventProcessorTest {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessorTest.class);
    private OutputCapture output;

    @Before
    public void setUp() {
        output = new OutputCapture();
    }

    private Processor<EventBatch.Builder> processor(Consumer<EventProcessor<EventBatch.Builder>> configure) {
        EventProcessor<EventBatch.Builder> processor = new EventProcessor<>();

        // base configuration
        processor.converter(EventBatch.Builder::build);
        processor.plugin(output);

        // per-test configuration
        configure.accept(processor);

        Processor<EventBatch.Builder> inst = processor.build();

        LifecycleAware.start(inst);

        return inst;
    }

    private <T> Stage<T, T> disruptorStage(Consumer<DisruptorBufferConfig.Builder> configure) {
        DisruptorBufferConfig.Builder config = DisruptorBufferConfig.builder();

        // base configuration
        config.waitStrategy(new BlockingWaitStrategy())
            .threadFactory(DaemonThreadFactory.INSTANCE)
            .batchMaxSize(10)
            .capacity(128);

        // per-test configuration
        configure.accept(config);

        return new DisruptorBuffer<>(config.build());
    }

    @Test
    public void testInterceptorFilter() throws Exception {
        Processor<EventBatch.Builder> stage = processor(builder -> builder
            .interceptor(event -> event.getAccountId().equals("2")));

        stage.process(eventBatchBuilder().setAccountId("1"));
        stage.process(eventBatchBuilder().setAccountId("2").addVisitor(visitorBuilder().setVisitorId("1").build()));
        stage.process(eventBatchBuilder().setAccountId("3"));
        stage.process(eventBatchBuilder().setAccountId("2").addVisitor(visitorBuilder().setVisitorId("2").build()));
        stage.process(eventBatchBuilder().setAccountId("1"));

        await().timeout(3, SECONDS).untilAtomic(output.successesCount, equalTo(2));
        assertEquals(0, output.failures.size());
    }

    @Test
    public void testEventTransformerRuntimeException() throws Exception {
        AtomicInteger count = new AtomicInteger(0);

        Processor<EventBatch.Builder> stage = processor(builder -> builder
            .transformer(b -> {}) // good actor
            .transformer(b -> {   // bad actor
                if (count.getAndIncrement() == 1) {
                    throw new NullPointerException("TEST");
                }
            }));

        stage.process(eventBatchBuilder());
        assertEquals(1, output.payloads.size());
        assertEquals(1, output.successes.size());
        assertEquals(0, output.failures.size());

        stage.process(eventBatchBuilder()); // throws
        assertEquals(2, output.payloads.size());
        assertEquals(2, output.successes.size());
        assertEquals(0, output.failures.size());

        stage.process(eventBatchBuilder()); // should complete
        assertEquals(3, output.payloads.size());
        assertEquals(3, output.successes.size());
        assertEquals(0, output.failures.size());
    }

    @Test
    public void testEventInterceptorRuntimeException() throws Exception {
        AtomicInteger count = new AtomicInteger(0);

        Processor<EventBatch.Builder> stage = processor(builder -> builder
            .interceptor(event -> true) // good actor
            .interceptor(event -> {     // bad actor
                if (count.getAndIncrement() == 1) {
                    throw new NullPointerException("TEST");
                }
                return true;
            }));

        stage.process(eventBatchBuilder());
        assertEquals(1, output.payloads.size());
        assertEquals(1, output.successes.size());
        assertEquals(0, output.failures.size());

        stage.process(eventBatchBuilder()); // throws
        assertEquals(2, output.payloads.size());
        assertEquals(2, output.successes.size());
        assertEquals(0, output.failures.size());

        stage.process(eventBatchBuilder()); // should complete
        assertEquals(3, output.payloads.size());
        assertEquals(3, output.successes.size());
        assertEquals(0, output.failures.size());
    }

    @Test
    public void testEventCallbackRuntimeException() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger others = new AtomicInteger(0);

        Processor<EventBatch.Builder> stage = processor(builder -> builder
            .callback(event -> others.incrementAndGet())
            .callback(event -> {
                if (count.getAndIncrement() == 1) {
                    throw new NullPointerException("TEST");
                }
            })
            .callback(event -> others.incrementAndGet()));

        stage.process(eventBatchBuilder());
        assertEquals(1, output.payloads.size());
        assertEquals(1, output.successes.size());
        assertEquals(0, output.failures.size());
        assertEquals(2, others.get());

        stage.process(eventBatchBuilder()); // throws
        assertEquals(2, output.payloads.size()); // event still goes out
        assertEquals(2, output.successes.size());
        assertEquals(0, output.failures.size());
        assertEquals(4, others.get());

        stage.process(eventBatchBuilder()); // should complete
        assertEquals(3, output.payloads.size());
        assertEquals(3, output.successes.size());
        assertEquals(0, output.failures.size());
        assertEquals(6, others.get());
    }

    @Test
    public void testBatchesByEventContext() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);
        final Phaser phaser = new Phaser();

        // block the consumer to allow us to fill up
        phaser.register();

        Processor<EventBatch.Builder> stage = processor(builder -> builder
            .bufferStage(
                disruptorStage(d -> d
                    .waitStrategy(new TestableWaitStrategy(phaser))
                    .batchMaxSize(100)))
            .transformer(e -> {
                switch (counter.getAndIncrement()) {
                    case 1:
                        e.setAccountId("other");
                        break;
                    case 2:
                        e.setAnonymizeIp(true);
                        break;
                    case 3:
                        e.setClientName("other");
                        break;
                    case 4:
                        e.setClientVersion("other");
                        break;
                    case 5:
                        e.setProjectId("other");
                        break;
                    case 6:
                        e.setRevision("other");
                        break;
                    default:
                        break;
                }
            }));

        for (int i = 0; i < 10; i++) {
            stage.process(eventBatchBuilder()
                .addVisitor(visitorBuilder().setVisitorId(String.valueOf(i)).build()));
            logger.info("Added visitor {}", i);
        }

        phaser.arriveAndDeregister();

        await().timeout(1, SECONDS).untilAtomic(output.successesCount, equalTo(10));

        assertEquals(7, output.payloads.size());   // transformed get their own batch
        assertEquals(10, output.successes.size()); // total
        assertEquals(0, output.failures.size());
    }

    /**
     * Testing utility. Allows external synchronization
     */
    public static class TestableWaitStrategy implements WaitStrategy {
        private static final Logger logger = LoggerFactory.getLogger(TestableWaitStrategy.class);

        private final Phaser phaser;
        private final WaitStrategy delegate;

        public TestableWaitStrategy(Phaser phaser) {
            this(phaser, new YieldingWaitStrategy());
        }

        public TestableWaitStrategy(Phaser phaser, WaitStrategy delegate) {
            this.phaser = phaser;
            this.delegate = delegate;

            phaser.register();
        }

        @Override
        public long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier barrier) throws AlertException, InterruptedException, TimeoutException {
            int arrivalPhase = phaser.arrive();

            logger.debug("Waiting on {} of {} to advance from phase {}...",
                phaser.getUnarrivedParties(),
                phaser.getRegisteredParties(),
                arrivalPhase);

            phaser.awaitAdvance(arrivalPhase);

            logger.debug("Advanced to phase {}", phaser.getPhase());

            return delegate.waitFor(sequence, cursor, dependentSequence, barrier);
        }

        @Override
        public void signalAllWhenBlocking() {
            delegate.signalAllWhenBlocking();
        }
    }

    @Test
    public void testMergesVisitors() throws Exception {
        Processor<EventBatch.Builder> stage = processor(builder -> builder
            .eventFactory(TestLogEvent::new)
            .bufferStage(disruptorStage(d -> d.batchMaxSize(3))));

        stage.process(eventBatchBuilder()
            .addVisitor(visitorBuilder()
                .setVisitorId("1").build()));
        stage.process(eventBatchBuilder()
            .addVisitor(visitorBuilder()
                .setVisitorId("2").build())
            .addVisitor(visitorBuilder()
                .setVisitorId("3").build()));
        stage.process(eventBatchBuilder()
            .addVisitor(visitorBuilder()
                .setVisitorId("1").build()));

        await().timeout(1, SECONDS).untilAtomic(output.successesCount, equalTo(3));
        assertEquals(0, output.failures.size());

        LongSummaryStatistics visitorsSizeSummary = output.payloadStats(logEvent ->
            logEvent.getEventBatch().getVisitors().size());

        List<Visitor> visitors = output.flatMapPayloads(logEvent -> logEvent.getEventBatch().getVisitors());

        assertEquals(4, visitorsSizeSummary.getSum());
        assertEquals("1", visitors.get(0).getVisitorId());
        assertEquals("2", visitors.get(1).getVisitorId());
        assertEquals("3", visitors.get(2).getVisitorId());
        assertEquals("1", visitors.get(3).getVisitorId());
    }

    @Ignore
    @Test
    public void scratch() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        Processor<EventBatch.Builder> stage = processor(builder -> builder
            .transformer(b -> {
                b.setAccountId("test");
            })
            .transformer(b -> {
                b.addVisitor(visitorBuilder()
                    .setVisitorId(String.valueOf(counter.incrementAndGet()))
                    .build());
            }));

        stage.process(eventBatchBuilder());

        stage.process(eventBatchBuilder());

        stage.process(eventBatchBuilder());

        assertEquals(2, output.payloads.size());
        assertEquals(3, output.successes.size());
        assertEquals(0, output.failures.size());

        // interceptor
        for (final EventBatch eventBatch : output.successes) {
            assertEquals("test", eventBatch.getAccountId());
        }
    }

    private Visitor.Builder visitorBuilder() {
        return new Visitor.Builder()
            .setVisitorId("test")
            .setAttributes(new ArrayList<>())
            .setSnapshots(new ArrayList<>());
    }

    private EventBatch.Builder eventBatchBuilder() {
        return new EventBatch.Builder()
            .setAccountId("1")
            .setProjectId("2")
            .setRevision("0")
            .setVisitors(new ArrayList<>());
    }

    private static class FailureArgs {
        EventBatch eventBatch;
        Throwable throwable;
    }

    /**
     * Plugin that configures {@link EventHandler} that captures payloads
     * and callback that captures invocations.
     *
     * Collections are paired with Atomic for more efficient wait conditions.
     */
    public static class OutputCapture implements Plugin<EventProcessor<EventBatch.Builder>> {
        AtomicInteger payloadCount = new AtomicInteger();
        List<LogEvent> payloads = Collections.synchronizedList(new LinkedList<>());

        AtomicInteger successesCount = new AtomicInteger();
        List<EventBatch> successes = Collections.synchronizedList(new LinkedList<>());

        AtomicInteger failuresCount = new AtomicInteger();
        List<FailureArgs> failures = Collections.synchronizedList(new LinkedList<>());

        AtomicInteger transformerCount = new AtomicInteger();
        AtomicInteger interceptorCount = new AtomicInteger();
        AtomicInteger inflightCount = new AtomicInteger();

        void dump() {
            logger.info("service invocations: {}", payloads.size());
            logger.info("success callbacks: {}", successes.size());
            logger.info("failure callbacks: {}", failures.size());
        }

        @Override
        public void configure(EventProcessor<EventBatch.Builder> builder) {
            builder.transformer(event -> {
                transformerCount.incrementAndGet();
                inflightCount.incrementAndGet();
            });

            builder.interceptor(event -> {
                interceptorCount.incrementAndGet();
                return true;
            });

            builder.eventHandler(new EventHandler() {
                @Override
                public synchronized void dispatchEvent(LogEvent logEvent) {
                    logger.info("Sending {}", logEvent);
                    payloadCount.incrementAndGet();
                    payloads.add(logEvent);
                    logEvent.markSuccess();
                }
            });

            builder.callback(new Callback<EventBatch>() {
                @Override
                public synchronized void success(EventBatch context) {
                    successesCount.incrementAndGet();
                    successes.add(context);

                    inflightCount.decrementAndGet();
                }

                @Override
                public synchronized void failure(EventBatch context, Throwable throwable) {
                    FailureArgs args = new FailureArgs();
                    args.eventBatch = context;
                    args.throwable = throwable;

                    failuresCount.incrementAndGet();
                    failures.add(args);

                    inflightCount.decrementAndGet();
                }
            });
        }

        <T> List<T> mapPayloads(Function<LogEvent, T> mapper) {
            return payloads.stream().map(mapper).collect(Collectors.toList());
        }

        <T> List<T> flatMapPayloads(Function<LogEvent, Collection<T>> mapper) {
            return payloads.stream().flatMap(e -> mapper.apply(e).stream()).collect(Collectors.toList());
        }


        LongSummaryStatistics payloadStats(ToLongFunction<LogEvent> fn) {
            return payloads.stream().collect(Collectors.summarizingLong(fn));
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("OutputCapture{");
            sb.append("payloadCount=").append(payloadCount.get());
            sb.append(", successesCount=").append(successesCount.get());
            sb.append(", failuresCount=").append(failuresCount.get());
            sb.append(", payloads=").append(payloads);
            sb.append(", successes=").append(successes);
            sb.append(", failures=").append(failures);
            sb.append('}');
            return sb.toString();
        }
    }

    /**
     * Testing extension that exposes the underlying {@link EventBatch}
     */
    class TestLogEvent extends LogEvent {
        final EventBatch eventBatch;

        public TestLogEvent(EventBatch eventBatch) {
            super(RequestMethod.POST, "https://test", Collections.emptyMap(), eventBatch);
            this.eventBatch = eventBatch;
        }
    }
}
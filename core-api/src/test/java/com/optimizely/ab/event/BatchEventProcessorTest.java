/**
 *
 *    Copyright 2019, Optimizely and contributors
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
package com.optimizely.ab.event;

import com.optimizely.ab.EventHandlerRule;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.event.internal.*;
import com.optimizely.ab.notification.NotificationCenter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BatchEventProcessorTest {

    private static final String EVENT_ID = "eventId";
    private static final String EVENT_NAME = "eventName";
    private static final String USER_ID = "userId";

    private static final int MAX_BATCH_SIZE = 10;
    private static final long MAX_DURATION_MS = 1000;
    private static final long TIMEOUT_MS = 5000;

    @Mock
    private ProjectConfig projectConfig;

    @Rule
    public EventHandlerRule eventHandlerRule = new EventHandlerRule();

    private BlockingQueue<Object> eventQueue;
    private BatchEventProcessor eventProcessor;
    private NotificationCenter notificationCenter;

    @Before
    public void setUp() throws Exception {
        when(projectConfig.getRevision()).thenReturn("1");
        when(projectConfig.getProjectId()).thenReturn("X");

        eventQueue = new ArrayBlockingQueue<>(100);
        notificationCenter = new NotificationCenter();
    }

    @After
    public void tearDown() throws Exception {
        if (eventProcessor != null) {
            eventProcessor.close();
        }
    }

    @Test
    public void testDrainOnClose() throws Exception {
        UserEvent userEvent = buildConversionEvent(EVENT_NAME);
        setEventProcessor(eventHandlerRule);
        eventProcessor.process(userEvent);
        eventProcessor.close();

        assertEquals(0, eventQueue.size());
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);
    }

    @Test
    public void testFlushOnMaxTimeout() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        setEventProcessor(logEvent -> {
            eventHandlerRule.dispatchEvent(logEvent);
            countDownLatch.countDown();
        });

        UserEvent userEvent = buildConversionEvent(EVENT_NAME);
        eventProcessor.process(userEvent);
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);

        if (!countDownLatch.await(MAX_DURATION_MS * 3, TimeUnit.MILLISECONDS)) {
            fail("Exceeded timeout waiting for events to flush.");
        }

        eventProcessor.close();
        assertEquals(0, eventQueue.size());
        eventHandlerRule.expectCalls(1);
    }

    @Test
    public void testFlushMaxBatchSize() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        setEventProcessor(logEvent -> {
            assertEquals(MAX_BATCH_SIZE, logEvent.getEventBatch().getVisitors().size());
            eventHandlerRule.dispatchEvent(logEvent);
            countDownLatch.countDown();
        });

        for (int i = 0; i < MAX_BATCH_SIZE; i++) {
            String eventName = EVENT_NAME + i;
            UserEvent userEvent = buildConversionEvent(eventName);
            eventProcessor.process(userEvent);
            eventHandlerRule.expectConversion(eventName, USER_ID);
        }

        if (!countDownLatch.await(MAX_DURATION_MS * 3, TimeUnit.MILLISECONDS)) {
            fail("Exceeded timeout waiting for events to flush.");
        }

        assertEquals(0, eventQueue.size());
        eventHandlerRule.expectCalls(1);
    }

    @Test
    public void testFlush() throws Exception {
        setEventProcessor(logEvent -> eventHandlerRule.dispatchEvent(logEvent));

        UserEvent userEvent = buildConversionEvent(EVENT_NAME);
        eventProcessor.process(userEvent);
        eventProcessor.flush();
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);

        eventProcessor.process(userEvent);
        eventProcessor.flush();
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);

        eventHandlerRule.expectCalls(2);
    }

    @Test
    public void testFlushOnMismatchRevision() throws Exception {
        setEventProcessor(logEvent -> eventHandlerRule.dispatchEvent(logEvent));

        ProjectConfig projectConfig1 = mock(ProjectConfig.class);
        when(projectConfig1.getRevision()).thenReturn("1");
        when(projectConfig1.getProjectId()).thenReturn("X");
        UserEvent userEvent1 = buildConversionEvent(EVENT_NAME, projectConfig1);
        eventProcessor.process(userEvent1);
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);

        ProjectConfig projectConfig2 = mock(ProjectConfig.class);
        when(projectConfig2.getRevision()).thenReturn("2");
        when(projectConfig1.getProjectId()).thenReturn("X");
        UserEvent userEvent2 = buildConversionEvent(EVENT_NAME, projectConfig2);
        eventProcessor.process(userEvent2);
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);

        eventProcessor.close();
        eventHandlerRule.expectCalls(2);
    }

    @Test
    public void testFlushOnMismatchProjectId() throws Exception {
        setEventProcessor(logEvent -> eventHandlerRule.dispatchEvent(logEvent));

        ProjectConfig projectConfig1 = mock(ProjectConfig.class);
        when(projectConfig1.getRevision()).thenReturn("1");
        when(projectConfig1.getProjectId()).thenReturn("X");
        UserEvent userEvent1 = buildConversionEvent(EVENT_NAME, projectConfig1);
        eventProcessor.process(userEvent1);
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);

        ProjectConfig projectConfig2 = mock(ProjectConfig.class);
        when(projectConfig1.getRevision()).thenReturn("1");
        when(projectConfig2.getProjectId()).thenReturn("Y");
        UserEvent userEvent2 = buildConversionEvent(EVENT_NAME, projectConfig2);
        eventProcessor.process(userEvent2);
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);

        eventProcessor.close();
        eventHandlerRule.expectCalls(2);
    }

    @Test
    public void testStopAndStart() throws Exception {
        setEventProcessor(logEvent -> eventHandlerRule.dispatchEvent(logEvent));

        UserEvent userEvent = buildConversionEvent(EVENT_NAME);
        eventProcessor.process(userEvent);
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);

        eventProcessor.close();

        eventProcessor.process(userEvent);
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);

        eventProcessor.start();

        eventProcessor.close();
        eventHandlerRule.expectCalls(2);
    }

    @Test
    public void testNotificationCenter() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        notificationCenter.addNotificationHandler(LogEvent.class, x -> counter.incrementAndGet());
        setEventProcessor(logEvent -> {});

        UserEvent userEvent = buildConversionEvent(EVENT_NAME);
        eventProcessor.process(userEvent);
        eventProcessor.close();

        assertEquals(1, counter.intValue());
    }

    @Test
    public void testCloseTimeout() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        setEventProcessor(logEvent -> {
            if (!countDownLatch.await(TIMEOUT_MS * 2, TimeUnit.MILLISECONDS)) {
                fail("Exceeded timeout waiting for close.");
            }
        });

        UserEvent userEvent = buildConversionEvent(EVENT_NAME);
        eventProcessor.process(userEvent);
        eventProcessor.close();

        countDownLatch.countDown();
    }

    @Test
    public void testCloseEventHandler() throws Exception {
        EventHandler mockEventHandler = mock(
            EventHandler.class,
            withSettings().extraInterfaces(AutoCloseable.class)
        );

        setEventProcessor(mockEventHandler);
        eventProcessor.close();
        verify((AutoCloseable) mockEventHandler).close();
    }

    @Test
    public void testInvalidBatchSizeUsesDefault() {
        eventProcessor = BatchEventProcessor.builder()
            .withEventQueue(eventQueue)
            .withBatchSize(-1)
            .withFlushInterval(MAX_DURATION_MS)
            .withEventHandler(new NoopEventHandler())
            .withNotificationCenter(notificationCenter)
            .withTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build();

        assertEquals(eventProcessor.batchSize, BatchEventProcessor.DEFAULT_BATCH_SIZE);
    }

    @Test
    public void testInvalidFlushIntervalUsesDefault() {
        eventProcessor = BatchEventProcessor.builder()
            .withEventQueue(eventQueue)
            .withBatchSize(MAX_BATCH_SIZE)
            .withFlushInterval(-1L)
            .withEventHandler(new NoopEventHandler())
            .withNotificationCenter(notificationCenter)
            .withTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build();

        assertEquals(eventProcessor.flushInterval, BatchEventProcessor.DEFAULT_BATCH_INTERVAL);
    }

    @Test
    public void testInvalidTimeoutUsesDefault() {
        eventProcessor = BatchEventProcessor.builder()
            .withEventQueue(eventQueue)
            .withBatchSize(MAX_BATCH_SIZE)
            .withFlushInterval(MAX_DURATION_MS)
            .withEventHandler(new NoopEventHandler())
            .withNotificationCenter(notificationCenter)
            .withTimeout(-1L, TimeUnit.MILLISECONDS)
            .build();

        assertEquals(eventProcessor.timeoutMillis, BatchEventProcessor.DEFAULT_TIMEOUT_INTERVAL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultEventHandler() {
        eventProcessor = BatchEventProcessor.builder().build();
    }

    private void setEventProcessor(EventHandler eventHandler) {
        eventProcessor = BatchEventProcessor.builder()
            .withEventQueue(eventQueue)
            .withBatchSize(MAX_BATCH_SIZE)
            .withFlushInterval(MAX_DURATION_MS)
            .withEventHandler(eventHandler)
            .withNotificationCenter(notificationCenter)
            .withTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build();
    }

    private ConversionEvent buildConversionEvent(String eventName) {
        return buildConversionEvent(eventName, projectConfig);
    }

    private static ConversionEvent buildConversionEvent(String eventName, ProjectConfig projectConfig) {
        return UserEventFactory.createConversionEvent(projectConfig, USER_ID, EVENT_ID, eventName,
            Collections.emptyMap(), Collections.emptyMap());
    }
}
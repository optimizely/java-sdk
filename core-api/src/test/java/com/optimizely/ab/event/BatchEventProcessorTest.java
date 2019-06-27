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
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.internal.*;
import com.optimizely.ab.event.internal.payload.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BatchEventProcessorTest {

    private static final String EVENT_ID = "eventId";
    private static final String EVENT_NAME = "eventName";
    private static final String USER_ID = "userId";

    private static final int MAX_BATCH_SIZE = 10;
    private static final int MAX_DURATION_MS = 1000;

    @Mock
    private ProjectConfig projectConfig;

    @Rule
    public EventHandlerRule eventHandlerRule = new EventHandlerRule();

    private BlockingQueue<Object> eventQueue;
    private BatchEventProcessor eventProcessor;

    @Before
    public void setUp() throws Exception {
        when(projectConfig.getRevision()).thenReturn("1");

        eventQueue = new ArrayBlockingQueue<>(100);
        eventProcessor = new BatchEventProcessor(eventQueue, MAX_BATCH_SIZE, MAX_DURATION_MS, null);
        eventProcessor.addHandler(eventHandlerRule::dispatchEvent);
    }

    @After
    public void tearDown() throws Exception {
        eventProcessor.close();
    }

    @Test
    public void testDrainOnClose() throws Exception {
        UserEvent userEvent = buildConversionEvent(EVENT_NAME);
        eventProcessor.process(userEvent);
        eventProcessor.close();

        assertEquals(0, eventQueue.size());
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);
    }

    @Test
    public void testFlushOnMaxTimeout() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        eventProcessor.addHandler(logEvent -> countDownLatch.countDown());

        UserEvent userEvent = buildConversionEvent(EVENT_NAME);
        eventProcessor.process(userEvent);
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);

        if (!countDownLatch.await(MAX_DURATION_MS * 3, TimeUnit.MILLISECONDS)) {
            fail("Exceeded timeout waiting for notification.");
        }

        eventProcessor.close();
        assertEquals(0, eventQueue.size());
    }

    @Test
    public void testFlushMaxBatchSize() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        eventProcessor.addHandler(logEvent -> {
            assertEquals(MAX_BATCH_SIZE, logEvent.getEventBatch().getVisitors().size());
            countDownLatch.countDown();
        });

        for (int i = 0; i < MAX_BATCH_SIZE; i++) {
            String eventName = EVENT_NAME + i;
            UserEvent userEvent = buildConversionEvent(eventName);
            eventProcessor.process(userEvent);
            eventHandlerRule.expectConversion(eventName, USER_ID);
        }

        countDownLatch.await();
        assertEquals(0, eventQueue.size());
    }

    @Test
    public void testFlushOnMismatchRevision() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        eventProcessor.addHandler(logEvent -> {
            countDownLatch.countDown();
            System.out.println(logEvent);
        });

        ProjectConfig projectConfig1 = mock(ProjectConfig.class);
        when(projectConfig1.getRevision()).thenReturn("1");
        UserEvent userEvent1 = buildConversionEvent(EVENT_NAME, projectConfig1);
        eventProcessor.process(userEvent1);
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);

        ProjectConfig projectConfig2 = mock(ProjectConfig.class);
        when(projectConfig2.getRevision()).thenReturn("2");
        UserEvent userEvent2 = buildConversionEvent(EVENT_NAME, projectConfig2);
        eventProcessor.process(userEvent2);
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);

        eventProcessor.close();
        if (!countDownLatch.await(MAX_DURATION_MS * 3, TimeUnit.MILLISECONDS)) {
            fail("Exceeded timeout waiting for notification.");
        }
    }

    private ConversionEvent buildConversionEvent(String eventName) {
        return buildConversionEvent(eventName, projectConfig);
    }

    private static ConversionEvent buildConversionEvent(String eventName, ProjectConfig projectConfig) {
        return UserEventFactory.createConversionEvent(projectConfig, USER_ID, EVENT_ID, eventName,
            Collections.emptyMap(), Collections.emptyMap());
    }
}
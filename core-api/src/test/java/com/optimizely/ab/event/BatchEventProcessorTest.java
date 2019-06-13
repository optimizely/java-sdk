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
import com.optimizely.ab.event.internal.payload.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class BatchEventProcessorTest {

    private static final String EVENT_NAME = "eventName";
    private static final String USER_ID = "userId";

    private static final int MAX_BATCH_SIZE = 10;
    private static final int MAX_DURATION_MS = 1000;

    @Rule
    public EventHandlerRule eventHandlerRule = new EventHandlerRule();

    private BlockingQueue<EventBatch> eventQueue;
    private BatchEventProcessor eventProcessor;

    @Before
    public void setUp() throws Exception {
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
        EventBatch eventBatch = createEventBatch(EVENT_NAME);
        eventProcessor.process(eventBatch);
        eventProcessor.close();

        assertEquals(0, eventQueue.size());
        eventHandlerRule.expectConversion(EVENT_NAME, USER_ID);
    }

    @Test
    public void testFlushOnMaxTimeout() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        eventProcessor.addHandler(logEvent -> countDownLatch.countDown());

        EventBatch eventBatch = createEventBatch(EVENT_NAME);
        eventProcessor.process(eventBatch);
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
            EventBatch eventBatch = createEventBatch(eventName);
            eventProcessor.process(eventBatch);
            eventHandlerRule.expectConversion(eventName, USER_ID);
        }

        countDownLatch.await();
        assertEquals(0, eventQueue.size());
    }

    private EventBatch createEventBatch(String eventName) {
        Event impressionEvent = new Event.Builder()
            .setTimestamp(System.currentTimeMillis())
            .setUuid(UUID.randomUUID().toString())
            .setEntityId("entityId")
            .setKey(eventName)
            .setTags(Collections.emptyMap())
            .build();

        List<Event> events = new ArrayList<>();
        events.add(impressionEvent);

        Snapshot snapshot = new Snapshot.Builder()
            .setDecisions(new ArrayList<>())
            .setEvents(events)
            .build();

        Visitor visitor = new Visitor.Builder()
            .setVisitorId("userId")
            .setAttributes(Collections.emptyList())
            .setSnapshots(Collections.singletonList((snapshot)))
            .build();

        List<Visitor> visitors = new ArrayList<>();
        visitors.add(visitor);

        return new EventBatch.Builder()
            .setClientName("clientName")
            .setClientVersion("clientVersion")
            .setAccountId("accountId")
            .setVisitors(visitors)
            .setAnonymizeIp(true)
            .setProjectId("projectId")
            .setRevision("revision")
            .build();

    }

}
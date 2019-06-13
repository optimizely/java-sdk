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

import com.optimizely.ab.event.internal.EventFactory;
import com.optimizely.ab.event.internal.payload.EventBatch;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class ForwardingEventProcessorTest {

    private static final EventBatch SENTINAL = new EventBatch();
    private ForwardingEventProcessor eventProcessor;

    @Before
    public void setUp() throws Exception {
        eventProcessor = new ForwardingEventProcessor();
    }

    @Test
    public void testAddHandler() {
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        eventProcessor.addHandler(logEvent -> {
            assertEquals(logEvent.getEventBatch(), SENTINAL);
            assertEquals(logEvent.getRequestMethod(), LogEvent.RequestMethod.POST);
            assertEquals(logEvent.getEndpointUrl(), EventFactory.EVENT_ENDPOINT);
            atomicBoolean.set(true);
        });

        eventProcessor.process(SENTINAL);
        assertTrue(atomicBoolean.get());
    }
}

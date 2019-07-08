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

import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.event.internal.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ForwardingEventProcessorTest {

    private static final String EVENT_ID = "eventId";
    private static final String EVENT_NAME = "eventName";
    private static final String USER_ID = "userId";

    private ForwardingEventProcessor eventProcessor;
    private AtomicBoolean atomicBoolean = new AtomicBoolean();

    @Mock
    private ProjectConfig projectConfig;

    @Before
    public void setUp() throws Exception {
        atomicBoolean.set(false);
        eventProcessor = new ForwardingEventProcessor(logEvent -> {
            assertNotNull(logEvent.getEventBatch());
            assertEquals(logEvent.getRequestMethod(), LogEvent.RequestMethod.POST);
            assertEquals(logEvent.getEndpointUrl(), EventFactory.EVENT_ENDPOINT);
            atomicBoolean.set(true);
        });
    }

    @Test
    public void testAddHandler() {
        UserEvent userEvent = buildConversionEvent(EVENT_NAME);
        eventProcessor.process(userEvent);
        assertTrue(atomicBoolean.get());
    }

    private ConversionEvent buildConversionEvent(String eventName) {
        return UserEventFactory.createConversionEvent(projectConfig, USER_ID, EVENT_ID, eventName,
            Collections.emptyMap(), Collections.emptyMap());
    }
}

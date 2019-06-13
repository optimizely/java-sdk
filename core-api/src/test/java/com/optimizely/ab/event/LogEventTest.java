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

import com.optimizely.ab.event.internal.payload.EventBatch;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class LogEventTest {

    private static final LogEvent.RequestMethod REQUEST_METHOD = LogEvent.RequestMethod.POST;
    private static final String ENDPOINT_URL = "endpoint";
    private static final Map<String, String> REQUEST_PARAMS = Collections.singletonMap("KEY", "VALUE");
    private static final EventBatch EVENT_BATCH = new EventBatch();

    private LogEvent logEvent;

    @Before
    public void setUp() throws Exception {
        logEvent = new LogEvent(REQUEST_METHOD, ENDPOINT_URL, REQUEST_PARAMS, EVENT_BATCH);
    }

    @Test
    public void testGetRequestMethod() {
        assertEquals(REQUEST_METHOD, logEvent.getRequestMethod());
    }

    @Test
    public void testGetEndpointUrl() {
        assertEquals(ENDPOINT_URL, logEvent.getEndpointUrl());
    }

    @Test
    public void testGetRequestParams() {
        assertEquals(REQUEST_PARAMS, logEvent.getRequestParams());
    }

    @Test
    public void testGetBody() {
        assertEquals("{}", logEvent.getBody());
    }

    @Test
    public void testGetEventBatch() {
        assertEquals(EVENT_BATCH, logEvent.getEventBatch());
    }

    @Test
    public void testToString() {
        assertEquals("LogEvent{requestMethod=POST, endpointUrl='endpoint', requestParams={KEY=VALUE}, body='{}'}", logEvent.toString());
    }

    @Test
    public void testEquals() {
        LogEvent otherLogEvent = new LogEvent(REQUEST_METHOD, ENDPOINT_URL, REQUEST_PARAMS, EVENT_BATCH);
        assertTrue(logEvent.equals(logEvent));
        assertTrue(logEvent.equals(otherLogEvent));
    }
}
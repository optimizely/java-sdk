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
package com.optimizely.ab.notification;

import com.optimizely.ab.event.LogEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class TrackNotificationTest {

    private static final String EVENT_KEY = "eventKey";
    private static final String USER_ID = "userID";
    private static final Map<String, String> USER_ATTRIBUTES = Collections.singletonMap("user", "attr");
    private static final Map<String, String> EVENT_TAGS = Collections.singletonMap("event", "tag");
    private static final LogEvent LOG_EVENT = new LogEvent(
        LogEvent.RequestMethod.POST,
        "endpoint",
        Collections.emptyMap(),
        null
    );

    private TrackNotification trackNotification;

    @Before
    public void setUp() throws Exception {
        trackNotification = new TrackNotification(EVENT_KEY, USER_ID, USER_ATTRIBUTES, EVENT_TAGS, LOG_EVENT);
    }

    @Test
    public void testGetEventKey() {
        assertEquals(EVENT_KEY, trackNotification.getEventKey());
    }

    @Test
    public void testGetUserId() {
        assertEquals(USER_ID, trackNotification.getUserId());
    }

    @Test
    public void testGetAttributes() {
        assertEquals(USER_ATTRIBUTES, trackNotification.getAttributes());
    }

    @Test
    public void testGetEventTags() {
        assertEquals(EVENT_TAGS, trackNotification.getEventTags());
    }

    @Test
    public void testGetEvent() {
        assertEquals(LOG_EVENT, trackNotification.getEvent());
    }

    @Test
    public void testToString() {
        assertEquals("TrackNotification{eventKey='eventKey', userId='userID', attributes={user=attr}, eventTags={event=tag}, event=LogEvent{requestMethod=POST, endpointUrl='endpoint', requestParams={}, body=''}}", trackNotification.toString());
    }
}

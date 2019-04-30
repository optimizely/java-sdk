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

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class TrackNotificationListenerTest {

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
    private TrackNotificationHandler trackNotificationHandler;

    @Before
    public void setUp() throws Exception {
        trackNotification = new TrackNotification(EVENT_KEY, USER_ID, USER_ATTRIBUTES, EVENT_TAGS, LOG_EVENT);
        trackNotificationHandler = new TrackNotificationHandler();
    }

    @Test
    public void testNotifyWithArgArray() {
        trackNotificationHandler.notify(EVENT_KEY, USER_ID, USER_ATTRIBUTES, EVENT_TAGS, LOG_EVENT);
    }

    @Test
    public void testNotifyWithTrackNotificationArg() {
        trackNotificationHandler.notify(trackNotification);
    }

    private static class TrackNotificationHandler extends TrackNotificationListener {

        @Override
        public void onTrack(@Nonnull String eventKey, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Map<String, ?> eventTags, @Nonnull LogEvent event) {
            assertEquals(EVENT_KEY, eventKey);
            assertEquals(USER_ID, userId);
            assertEquals(USER_ATTRIBUTES, attributes);
            assertEquals(EVENT_TAGS, eventTags);
            assertEquals(LOG_EVENT, event);
        }
    }
}

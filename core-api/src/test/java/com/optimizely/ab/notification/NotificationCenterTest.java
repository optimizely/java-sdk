/**
 *
 *    Copyright 2018-2019, Optimizely and contributors
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

import ch.qos.logback.classic.Level;
import com.optimizely.ab.OptimizelyRuntimeException;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.internal.LogbackVerifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class NotificationCenterTest {
    private NotificationCenter notificationCenter;
    private ActivateNotificationListener activateNotification;
    private TrackNotificationListener trackNotification;

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    @Before
    public void setUp() {
        notificationCenter = new NotificationCenter();
        activateNotification = mock(ActivateNotificationListener.class);
        trackNotification = mock(TrackNotificationListener.class);
    }

    @After
    public void tearDown() {
        notificationCenter.clearAllNotificationListeners();
    }

    @Test
    public void testAddWrongTrackNotificationListener() {
        int notificationId = notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, trackNotification);
        logbackVerifier.expectMessage(Level.WARN, "Notification listener was the wrong type. It was not added to the notification center.");
        assertEquals(-1, notificationId);
        assertFalse(notificationCenter.removeNotificationListener(notificationId));

    }

    @Test
    public void testAddWrongActivateNotificationListener() {
        int notificationId = notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Track, activateNotification);
        logbackVerifier.expectMessage(Level.WARN, "Notification listener was the wrong type. It was not added to the notification center.");
        assertEquals(-1, notificationId);
        assertFalse(notificationCenter.removeNotificationListener(notificationId));
    }

    @Test
    public void testAddDecisionNotificationTwice() {
        NotificationHandler<DecisionNotification> handler = decisionNotification -> { };
        NotificationManager<DecisionNotification> manager =
            notificationCenter.getNotificationManager(DecisionNotification.class);

        int notificationId = manager.addHandler(handler);
        int notificationId2 = manager.addHandler(handler);
        logbackVerifier.expectMessage(Level.WARN, "Notification listener was already added");
        assertEquals(-1, notificationId2);
        assertTrue(notificationCenter.removeNotificationListener(notificationId));
    }

    @Test
    public void testAddActivateNotificationTwice() {
        ActivateNotificationListener listener = new ActivateNotificationListener() {
            @Override
            public void onActivate(@Nonnull Experiment experiment, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Variation variation, @Nonnull LogEvent event) {

            }
        };
        int notificationId = notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, listener);
        int notificationId2 = notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, listener);
        logbackVerifier.expectMessage(Level.WARN, "Notification listener was already added");
        assertEquals(-1, notificationId2);
        assertTrue(notificationCenter.removeNotificationListener(notificationId));
    }

    @Test
    public void testAddActivateNotification() {
        int notificationId = notificationCenter.addActivateNotificationListener(new ActivateNotificationListener() {
            @Override
            public void onActivate(@Nonnull Experiment experiment, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Variation variation, @Nonnull LogEvent event) {

            }
        });
        assertNotSame(-1, notificationId);
        assertTrue(notificationCenter.removeNotificationListener(notificationId));
    }

    @Test
    public void testAddDecisionNotification() {
        NotificationManager<DecisionNotification> manager = notificationCenter.getNotificationManager(DecisionNotification.class);
        int notificationId = manager.addHandler(decisionNotification -> { });
        assertNotSame(-1, notificationId);
        assertTrue(notificationCenter.removeNotificationListener(notificationId));
    }

    @Test
    public void testAddTrackNotification() {
        int notificationId = notificationCenter.addTrackNotificationListener(new TrackNotificationListener() {
            @Override
            public void onTrack(@Nonnull String eventKey, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Map<String, ?> eventTags, @Nonnull LogEvent event) {

            }
        });
        assertNotSame(-1, notificationId);
        assertTrue(notificationCenter.removeNotificationListener(notificationId));
    }

    @Test
    public void testNotificationTypeClasses() {
        assertEquals(NotificationCenter.NotificationType.Activate.getNotificationTypeClass(),
            ActivateNotificationListener.class);
        assertEquals(NotificationCenter.NotificationType.Track.getNotificationTypeClass(), TrackNotificationListener.class);
    }

    @Test
    public void testAddTrackNotificationInterface() {
        int notificationId = notificationCenter.addTrackNotificationListener(new TrackNotificationListenerInterface() {
            @Override
            public void onTrack(@Nonnull String eventKey, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Map<String, ?> eventTags, @Nonnull LogEvent event) {

            }
        });
        assertNotSame(-1, notificationId);
        assertTrue(notificationCenter.removeNotificationListener(notificationId));
    }

    @Test
    public void testAddDecisionNotificationInterface() {
        NotificationManager<DecisionNotification> manager = notificationCenter.getNotificationManager(DecisionNotification.class);
        int notificationId = manager.addHandler(decisionNotification -> { });
        assertNotSame(-1, notificationId);
        assertTrue(manager.remove(notificationId));
    }

    @Test
    public void testAddActivateNotificationInterface() {
        int notificationId = notificationCenter.addActivateNotificationListener(new ActivateNotificationListenerInterface() {
            @Override
            public void onActivate(@Nonnull Experiment experiment, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Variation variation, @Nonnull LogEvent event) {

            }
        });
        assertNotSame(-1, notificationId);
        assertTrue(notificationCenter.removeNotificationListener(notificationId));
    }

    @Test
    @Deprecated
    public void testClearNotificationByActivateType() {
        NotificationManager<ActivateNotification> manager = notificationCenter.getNotificationManager(ActivateNotification.class);
        int id = manager.addHandler(message -> {});

        notificationCenter.clearNotificationListeners(NotificationCenter.NotificationType.Activate);
        assertFalse(manager.remove(id));
    }

    @Test
    @Deprecated
    public void testClearNotificationByTrackType() {
        NotificationManager<TrackNotification> manager = notificationCenter.getNotificationManager(TrackNotification.class);
        int id = manager.addHandler(message -> {});

        notificationCenter.clearNotificationListeners(NotificationCenter.NotificationType.Track);
        assertFalse(manager.remove(id));
    }

    @Test
    @Deprecated
    public void testAddActivateListenerInterface() {
        int id = notificationCenter.addActivateNotificationListener((experiment, userId, attributes, variation, event) -> { });

        NotificationManager<ActivateNotification> manager = notificationCenter.getNotificationManager(ActivateNotification.class);
        assertTrue(manager.remove(id));
    }

    @Test
    @Deprecated
    public void testAddTrackListenerInterface() {
        int id = notificationCenter.addTrackNotificationListener((experiment, userId, attributes, variation, event) -> { });

        NotificationManager<TrackNotification> manager = notificationCenter.getNotificationManager(TrackNotification.class);
        assertTrue(manager.remove(id));
    }

    @Test(expected = OptimizelyRuntimeException.class)
    public void testSendWithoutHandler() {
        notificationCenter.send(new TestNotification(""));
    }

    @Test
    public void testSendWithHandler() {
        testSendWithNotification(new TrackNotification());
        testSendWithNotification(new DecisionNotification());
        testSendWithNotification(new ActivateNotification());
    }

    private void testSendWithNotification(Object notification) {
        TestNotificationHandler handler = new TestNotificationHandler<>();
        notificationCenter.getNotificationManager(notification.getClass()).addHandler(handler);
        notificationCenter.send(notification);

        List messages = handler.getMessages();
        assertEquals(1, messages.size());
        assertEquals(notification, messages.get(0));

    }
}

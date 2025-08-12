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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.mockito.Mockito.mock;

import com.optimizely.ab.OptimizelyRuntimeException;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.internal.LogbackVerifier;

import ch.qos.logback.classic.Level;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertTrue;

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
        assertTrue(manager.remove(notificationId));
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
        final AtomicBoolean triggered = new AtomicBoolean();
        int notificationId = notificationCenter.addTrackNotificationListener((eventKey, userId, attributes, eventTags, event) -> triggered.set(true));
        notificationCenter.send(new TrackNotification());

        assertNotSame(-1, notificationId);
        assertTrue(triggered.get());
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
        final AtomicBoolean triggered = new AtomicBoolean();
        int notificationId = notificationCenter.addActivateNotificationListener((experiment, userId, attributes, variation, event) -> triggered.set(true));
        notificationCenter.send(new ActivateNotification());

        assertNotSame(-1, notificationId);
        assertTrue(triggered.get());
        assertTrue(notificationCenter.removeNotificationListener(notificationId));
    }

    @Test
    public void testAddValidNotificationHandler() {
        assertEquals(1, notificationCenter.addNotificationHandler(ActivateNotification.class, x -> {}));
        assertEquals(2, notificationCenter.addNotificationHandler(DecisionNotification.class, x -> {}));
        assertEquals(3, notificationCenter.addNotificationHandler(TrackNotification.class, x -> {}));
        assertEquals(4, notificationCenter.addNotificationHandler(UpdateConfigNotification.class, x -> {}));
        assertEquals(5, notificationCenter.addNotificationHandler(LogEvent.class, x -> {}));
    }

    @Test
    public void testAddInvalidNotificationHandler() {
        int actual = notificationCenter.addNotificationHandler(Integer.class, i -> {});
        assertEquals(-1, actual);
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
        testSendWithNotification(new LogEvent(LogEvent.RequestMethod.GET, "localhost", Collections.emptyMap(), null));
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

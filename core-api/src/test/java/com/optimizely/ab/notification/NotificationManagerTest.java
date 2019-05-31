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

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class NotificationManagerTest {

    private NotificationManager<TestNotification> notificationManager;
    private AtomicInteger counter;

    @Before
    public void setUp() {
        counter = new AtomicInteger();
        notificationManager = new NotificationManager<>(counter);
    }

    @Test
    public void testAddListener() {
        assertEquals(1, notificationManager.addHandler(new TestNotificationHandler<>()));
        assertEquals(2, notificationManager.addHandler(new TestNotificationHandler<>()));
        assertEquals(3, notificationManager.addHandler(new TestNotificationHandler<>()));
    }

    @Test
    public void testSend() {
        TestNotificationHandler<TestNotification> handler = new TestNotificationHandler<>();
        assertEquals(1, notificationManager.addHandler(handler));

        notificationManager.send(new TestNotification("message1"));
        notificationManager.send(new TestNotification("message2"));
        notificationManager.send(new TestNotification("message3"));

        List<TestNotification> messages = handler.getMessages();
        assertEquals(3, messages.size());
        assertEquals("message1", messages.get(0).getMessage());
        assertEquals("message2", messages.get(1).getMessage());
        assertEquals("message3", messages.get(2).getMessage());
    }

    @Test
    public void testSendWithError() {
        TestNotificationHandler<TestNotification> handler = new TestNotificationHandler<>();
        assertEquals(1, notificationManager.addHandler(message -> {throw new RuntimeException("handle me");}));
        assertEquals(2, notificationManager.addHandler(handler));

        notificationManager.send(new TestNotification("message1"));

        List<TestNotification> messages = handler.getMessages();
        assertEquals(1, messages.size());
        assertEquals("message1", messages.get(0).getMessage());
    }
}

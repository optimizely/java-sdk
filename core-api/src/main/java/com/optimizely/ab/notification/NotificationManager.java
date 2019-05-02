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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NotificationManger is a generic class for managing notifications for a given class.
 *
 * The NotificationManager is responsible for storing a collection of NotificationHandlers and mapping
 * them to a globally unique integer so that they can be removed on demand.
 */
public class NotificationManager<T> {

    private static final Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    private final Map<Integer, NotificationHandler<T>> handlers = new LinkedHashMap<>();
    private final AtomicInteger counter;

    NotificationManager(AtomicInteger counter) {
        this.counter = counter;
    }

    public int addHandler(NotificationHandler<T> newHandler) {

        // Prevent registering a duplicate listener.
        for (NotificationHandler<T> handler: handlers.values()) {
            if (handler.equals(newHandler)) {
                logger.warn("Notification listener was already added");
                return -1;
            }
        }

        int notificationId = counter.incrementAndGet();
        handlers.put(notificationId, newHandler);

        return notificationId;
    }

    public void send(T message) {
        for (NotificationHandler<T> handler: handlers.values()) {
            handler.handle(message);
        }
    }

    public void clear() {
        handlers.clear();
    }

    public boolean remove(int notificationID) {
        NotificationHandler<T> handler = handlers.remove(notificationID);
        return handler != null;
    }
}

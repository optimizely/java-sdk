/**
 *
 *    Copyright 2017-2019, Optimizely and contributors
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

import com.optimizely.ab.OptimizelyRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.optimizely.ab.notification.Notification.ACTIVATE;
import static com.optimizely.ab.notification.Notification.TRACK;


/**
 * This class handles impression and conversion notificationsListeners. It replaces NotificationBroadcaster and is intended to be
 * more flexible.
 */
public class NotificationCenter {
    public enum DecisionNotificationType {
        AB_TEST("ab-test"),
        FEATURE("feature"),
        FEATURE_TEST("feature-test"),
        FEATURE_VARIABLE("feature-variable");

        private final String key;

        DecisionNotificationType(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }

    }

    /**
     * NotificationType is used for the notification types supported.
     */
    @Deprecated
    public enum NotificationType {

        Activate(ActivateNotificationListener.class), // Activate was called. Track an impression event
        Track(TrackNotificationListener.class); // Track was called.  Track a conversion event

        private Class notificationTypeClass;

        NotificationType(Class notificationClass) {
            this.notificationTypeClass = notificationClass;
        }

        public Class getNotificationTypeClass() {
            return notificationTypeClass;
        }
    }

    private final AtomicInteger counter = new AtomicInteger();
    private final ConcurrentHashMap<Class, NotificationManager> notifierMap = new ConcurrentHashMap<>();

    @Nonnull
    @SuppressWarnings("unchecked")
    public <T> NotificationManager<T> getNotificationManager(Class<? extends Notification> clazz) {
        NotificationManager<T> newManager = new NotificationManager<>(counter);
        NotificationManager<T> manager = (NotificationManager<T>) notifierMap.putIfAbsent(clazz, newManager);

        return manager == null ? newManager : manager;
    }

    final private static Logger logger = LoggerFactory.getLogger(NotificationCenter.class);

    /**
     * Convenience method to support lambdas as callbacks in later version of Java (8+).
     *
     * @param activateNotificationListener
     * @return greater than zero if added.
     */
    @Deprecated
    public int addActivateNotificationListener(final ActivateNotificationListenerInterface activateNotificationListener) {
        NotificationManager<ActivateNotification> notificationManager = getNotificationManager(ACTIVATE);

        if (activateNotificationListener instanceof ActivateNotificationListener) {
            return notificationManager.addHandler((ActivateNotificationListener) activateNotificationListener);
        } else {
            return notificationManager.addHandler(message -> activateNotificationListener.onActivate(
                message.getExperiment(),
                message.getUserId(),
                message.getAttributes(),
                message.getVariation(),
                message.getEvent()
            ));
        }
    }

    /**
     * Convenience method to support lambdas as callbacks in later versions of Java (8+)
     *
     * @param trackNotificationListener
     * @return greater than zero if added.
     */
    @Deprecated
    public int addTrackNotificationListener(final TrackNotificationListenerInterface trackNotificationListener) {
        NotificationManager<TrackNotification> notificationManager = getNotificationManager(TRACK);

        if (trackNotificationListener instanceof TrackNotificationListener) {
            return notificationManager.addHandler((TrackNotificationListener) trackNotificationListener);
        } else {
            return notificationManager.addHandler(message -> trackNotificationListener.onTrack(
                message.getEventKey(),
                message.getUserId(),
                message.getAttributes(),
                message.getEventTags(),
                message.getEvent()
            ));
        }
    }

    /**
     * Add a notification listener to the notification center.
     *
     * @param notificationType     - enum NotificationType to add.
     * @param notificationListener - Notification to add.
     * @return the notification id used to remove the notification.  It is greater than 0 on success.
     */
    @Deprecated
    public int addNotificationListener(NotificationType notificationType, NotificationListener notificationListener) {

        Class clazz = notificationType.getNotificationTypeClass();
        if (clazz == null || !clazz.isInstance(notificationListener)) {
            logger.warn("Notification listener was the wrong type. It was not added to the notification center.");
            return -1;
        }

        switch (notificationType) {
            case Track:
                return addTrackNotificationListener((TrackNotificationListener) notificationListener);
            case Activate:
                return addActivateNotificationListener((ActivateNotificationListener) notificationListener);
            default:
                throw new OptimizelyRuntimeException("Unsupported notificationType");
        }
    }

    /**
     * Remove the notification listener based on the notificationId passed back from addDecisionNotificationHandler.
     *
     * @param notificationID the id passed back from add notification.
     * @return true if removed otherwise false (if the notification is already registered, it returns false).
     */
    public boolean removeNotificationListener(int notificationID) {
        for (NotificationManager<?> manager : notifierMap.values()) {
            if (manager.remove(notificationID)) {
                logger.info("Notification listener removed {}", notificationID);
                return true;
            }
        }

        logger.warn("Notification listener with id {} not found", notificationID);
        return false;
    }

    /**
     * Clear out all the notification listeners.
     */
    public void clearAllNotificationListeners() {
        for (NotificationManager<?> manager : notifierMap.values()) {
            manager.clear();
        }
    }

    /**
     * Clear notification listeners by notification type.
     *
     * @param notificationType type of notificationsListeners to remove.
     */
    @Deprecated
    public void clearNotificationListeners(NotificationType notificationType) {
        switch (notificationType) {
            case Track:
                getNotificationManager(TRACK).clear();
                break;
            case Activate:
                getNotificationManager(ACTIVATE).clear();
                break;
            default:
                throw new OptimizelyRuntimeException("Unsupported notificationType");
        }
    }

    @SuppressWarnings("unchecked")
    public void send(Notification notification) {
        NotificationManager handler = notifierMap.get(notification.getClass());
        if (handler == null) {
            return;
        }

        handler.send(notification);
    }
}

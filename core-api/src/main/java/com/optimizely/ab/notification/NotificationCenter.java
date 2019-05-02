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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NotificationCenter handles all notification listeners.
 * It replaces NotificationBroadcaster and is intended to be more flexible.
 *
 * NotificationCenter is a holder for a set of supported {@link NotificationManager} instances.
 * If a notification object is sent via {@link NotificationCenter#send(Object)} that is not supported
 * an {@link OptimizelyRuntimeException} will be thrown. This is an internal interface so
 * usage should be restricted to the SDK.
 *
 * Supported notification classes are setup within {@link NotificationCenter#NotificationCenter()}
 * as an unmodifiable map so additional notifications must be added there.
 *
 * Currently supported notification classes are:
 * * {@link ActivateNotification}
 * * {@link TrackNotification}
 * * {@link DecisionNotification} with this class replacing {@link ActivateNotification}
 */
public class NotificationCenter {

    private static final Logger logger = LoggerFactory.getLogger(NotificationCenter.class);
    private final Map<Class, NotificationManager> notifierMap;

    // TODO move to DecisionNotification.
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

    public NotificationCenter() {
        AtomicInteger counter = new AtomicInteger();
        Map<Class, NotificationManager> validManagers = new HashMap<>();
        validManagers.put(ActivateNotification.class, new NotificationManager<ActivateNotification>(counter));
        validManagers.put(TrackNotification.class, new NotificationManager<TrackNotification>(counter));
        validManagers.put(DecisionNotification.class, new NotificationManager<DecisionNotification>(counter));

        notifierMap = Collections.unmodifiableMap(validManagers);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> NotificationManager<T> getNotificationManager(Class clazz) {
        return notifierMap.get(clazz);
    }

    /**
     * Convenience method to support lambdas as callbacks in later version of Java (8+).
     *
     * @param activateNotificationListener
     * @return greater than zero if added.
     *
     * @deprecated by {@link NotificationManager#addHandler(NotificationHandler)}
     */
    @Deprecated
    public int addActivateNotificationListener(final ActivateNotificationListenerInterface activateNotificationListener) {
        NotificationManager<ActivateNotification> notificationManager = getNotificationManager(ActivateNotification.class);
        if (notificationManager == null) {
            logger.warn("Notification listener was the wrong type. It was not added to the notification center.");
            return -1;
        }

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
     *
     * @deprecated by {@link NotificationManager#addHandler(NotificationHandler)}
     */
    @Deprecated
    public int addTrackNotificationListener(final TrackNotificationListenerInterface trackNotificationListener) {
        NotificationManager<TrackNotification> notificationManager = getNotificationManager(TrackNotification.class);
        if (notificationManager == null) {
            logger.warn("Notification listener was the wrong type. It was not added to the notification center.");
            return -1;
        }

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
     *
     * @deprecated by {@link NotificationManager#addHandler(NotificationHandler)}
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
     *
     * @deprecated by {@link NotificationCenter#clearNotificationListeners(Class)}
     */
    @Deprecated
    public void clearNotificationListeners(NotificationType notificationType) {
        switch (notificationType) {
            case Track:
                clearNotificationListeners(TrackNotification.class);
                break;
            case Activate:
                clearNotificationListeners(ActivateNotification.class);
                break;
            default:
                throw new OptimizelyRuntimeException("Unsupported notificationType");
        }
    }

    /**
     * Clear notification listeners by notification class.
     */
    public void clearNotificationListeners(Class clazz) {
        NotificationManager notificationManager = getNotificationManager(clazz);
        if (notificationManager == null) {
            throw new OptimizelyRuntimeException("Unsupported notification type.");
        }

        notificationManager.clear();
    }

    @SuppressWarnings("unchecked")
    public void send(Object notification) {
        NotificationManager handler = getNotificationManager(notification.getClass());
        if (handler == null) {
            throw new OptimizelyRuntimeException("Unsupported notificationType");
        }

        handler.send(notification);
    }
}

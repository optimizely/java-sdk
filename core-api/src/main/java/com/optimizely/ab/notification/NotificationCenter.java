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

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class handles impression and conversion notificationsListeners. It replaces NotificationBroadcaster and is intended to be
 * more flexible.
 */
public class NotificationCenter {
    public enum DecisionNotificationType {
        FEATURE("feature"),
        FEATURE_VARIABLE("feature_variable"),
        EXPERIMENT("experiment");

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

    ;


    // the notification id is incremented and is assigned as the callback id, it can then be used to remove the notification.
    private AtomicInteger notificationListenerID = new AtomicInteger();

    final private static Logger logger = LoggerFactory.getLogger(NotificationCenter.class);

    // notification holder holds the id as well as the notification.
    private static class NotificationHolder {
        int notificationId;
        NotificationListener notificationListener;
        DecisionNotificationListener decisionNotificationListener;

        NotificationHolder(int id, NotificationListener notificationListener) {
            notificationId = id;
            this.notificationListener = notificationListener;
        }

        NotificationHolder(int id, DecisionNotificationListener decisionNotificationListener) {
            notificationId = id;
            this.decisionNotificationListener = decisionNotificationListener;
        }
    }

    /**
     * Instantiate a new NotificationCenter
     */
    public NotificationCenter() {
        notificationsListeners.put(NotificationType.Activate, new ArrayList<NotificationHolder>());
        notificationsListeners.put(NotificationType.Track, new ArrayList<NotificationHolder>());
    }

    // private list of notification by notification type.
    // we used a list so that notification order can mean something.
    private Map<NotificationType, ArrayList<NotificationHolder>> notificationsListeners = new HashMap<NotificationType, ArrayList<NotificationHolder>>();
    private List<NotificationHolder> decisionListenerHolder = new ArrayList<>();

    /**
     * Convenience method to support lambdas as callbacks in later version of Java (8+).
     *
     * @param decisionNotificationListener
     * @return greater than zero if added.
     */
    public int addDecisionNotificationListener(DecisionNotificationListener decisionNotificationListener) {
        if (decisionNotificationListener != null) {
            for (NotificationHolder holder : decisionListenerHolder) {
                if (holder.decisionNotificationListener == decisionNotificationListener) {
                    // TODO: 3/27/2019 change log level from warn to info and to return existing listener ID
                    logger.warn("Notification listener was already added");
                    return -1;
                }
            }
            int id = this.notificationListenerID.incrementAndGet();
            decisionListenerHolder.add(new NotificationHolder(id, decisionNotificationListener));
            return id;
        } else {
            logger.warn("Notification listener was the wrong type. It was not added to the notification center.");
            return -1;
        }
    }

    /**
     * Convenience method to support lambdas as callbacks in later version of Java (8+).
     *
     * @param activateNotificationListenerInterface
     * @return greater than zero if added.
     */
    @Deprecated
    public int addActivateNotificationListener(final ActivateNotificationListenerInterface activateNotificationListenerInterface) {
        if (activateNotificationListenerInterface instanceof ActivateNotificationListener) {
            return addNotificationListener(NotificationType.Activate, (NotificationListener) activateNotificationListenerInterface);
        } else {
            return addNotificationListener(NotificationType.Activate, new ActivateNotificationListener() {
                @Override
                public void onActivate(@Nonnull Experiment experiment, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Variation variation, @Nonnull LogEvent event) {
                    activateNotificationListenerInterface.onActivate(experiment, userId, attributes, variation, event);
                }
            });
        }
    }

    /**
     * Convenience method to support lambdas as callbacks in later versions of Java (8+)
     *
     * @param trackNotificationListenerInterface
     * @return greater than zero if added.
     */
    public int addTrackNotificationListener(final TrackNotificationListenerInterface trackNotificationListenerInterface) {
        if (trackNotificationListenerInterface instanceof TrackNotificationListener) {
            return addNotificationListener(NotificationType.Track, (NotificationListener) trackNotificationListenerInterface);
        } else {
            return addNotificationListener(NotificationType.Track, new TrackNotificationListener() {
                @Override
                public void onTrack(@Nonnull String eventKey, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Map<String, ?> eventTags, @Nonnull LogEvent event) {
                    trackNotificationListenerInterface.onTrack(eventKey, userId, attributes, eventTags, event);
                }
            });
        }
    }

    /**
     * Add a notification listener to the notification center.
     *
     * @param notificationType     - enum NotificationType to add.
     * @param notificationListener - Notification to add.
     * @return the notification id used to remove the notification.  It is greater than 0 on success.
     */
    public int addNotificationListener(NotificationType notificationType, NotificationListener notificationListener) {

        Class clazz = notificationType.notificationTypeClass;
        if (clazz == null || !clazz.isInstance(notificationListener)) {
            logger.warn("Notification listener was the wrong type. It was not added to the notification center.");
            return -1;
        }

        for (NotificationHolder holder : notificationsListeners.get(notificationType)) {
            if (holder.notificationListener == notificationListener) {
                logger.warn("Notification listener was already added");
                return -1;
            }
        }
        int id = this.notificationListenerID.incrementAndGet();
        notificationsListeners.get(notificationType).add(new NotificationHolder(id, notificationListener));
        logger.info("Notification listener {} was added with id {}", notificationListener.toString(), id);
        return id;
    }

    /**
     * Remove the notification listener based on the notificationId passed back from addNotificationListener.
     *
     * @param notificationID the id passed back from add notification.
     * @return true if removed otherwise false (if the notification is already registered, it returns false).
     */
    public boolean removeNotificationListener(int notificationID) {

        for (List<NotificationHolder> notificationHolders : notificationsListeners.values()) {
            if (removeNotificationListener(notificationID, notificationHolders)) {
                return true;
            }
        }

        if (removeNotificationListener(notificationID, decisionListenerHolder)) {
            return true;
        }

        logger.warn("Notification listener with id {} not found", notificationID);

        return false;
    }

    /**
     * Helper method to iterate find NotificationHolder in an List identified by the notificationId
     *
     * @param notificationID the id passed back from add notification.
     * @param notificationHolderList list from which to remove notification listener.
     * @return true if removed otherwise false
     */
    private boolean removeNotificationListener(int notificationID, List<NotificationHolder> notificationHolderList) {
        for (NotificationHolder holder : notificationHolderList) {
            if (holder.notificationId == notificationID) {
                notificationHolderList.remove(holder);
                logger.info("Notification listener removed {}", notificationID);
                return true;
            }
        }

        return false;
    }

    /**
     * Clear out all the notification listeners.
     */
    public void clearAllNotificationListeners() {
        decisionListenerHolder.clear();
        for (NotificationType type : NotificationType.values()) {
            clearNotificationListeners(type);
        }
    }

    /**
     * Clear notification listeners by notification type.
     *
     * @param notificationType type of notificationsListeners to remove.
     */
    public void clearNotificationListeners(NotificationType notificationType) {
        notificationsListeners.get(notificationType).clear();
    }

    /**
     * fire a notificaiton of Decision Notification type.
     *
     * @param decision containing Decision Notification object
     */
    public void sendNotifications(DecisionNotification decision) {
        for (NotificationHolder holder : decisionListenerHolder) {
            try {
                holder.decisionNotificationListener.onDecision(decision);
            } catch (Exception e) {
                logger.error("Unexpected exception calling notification listener {}", holder.notificationId, e);
            }
        }
    }

    // fire a notificaiton of a certain type.  The arg list changes depending on the type of notification sent.
    public void sendNotifications(NotificationType notificationType, Object... args) {
        ArrayList<NotificationHolder> holders = notificationsListeners.get(notificationType);
        for (NotificationHolder holder : holders) {
            try {
                holder.notificationListener.notify(args);
            } catch (Exception e) {
                logger.error("Unexpected exception calling notification listener {}", holder.notificationId, e);
            }
        }
    }

}

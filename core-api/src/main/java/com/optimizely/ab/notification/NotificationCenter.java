/**
 *
 *    Copyright 2017-2018, Optimizely and contributors
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
import com.optimizely.ab.faultinjection.ExceptionSpot;
import com.optimizely.ab.faultinjection.FaultInjectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * This class handles impression and conversion notificationsListeners. It replaces NotificationBroadcaster and is intended to be
 * more flexible.
 */
public class NotificationCenter {
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
    };

    private static void injectFault(ExceptionSpot spot) {
        FaultInjectionManager.getInstance().injectFault(spot);
    }

    // the notification id is incremented and is assigned as the callback id, it can then be used to remove the notification.
    private int notificationListenerID = 1;

    final private static Logger logger = LoggerFactory.getLogger(NotificationCenter.class);

    // notification holder holds the id as well as the notification.
    private static class NotificationHolder
    {
        int notificationId;
        NotificationListener notificationListener;

        NotificationHolder(int id, NotificationListener notificationListener) {
            notificationId = id;
            this.notificationListener = notificationListener;
        }
    }

    /**
     * Instantiate a new NotificationCenter
     */
    public NotificationCenter() {
        injectFault(ExceptionSpot.NotificationCenter_constructor_spot1);
        notificationsListeners.put(NotificationType.Activate, new ArrayList<NotificationHolder>());
        notificationsListeners.put(NotificationType.Track, new ArrayList<NotificationHolder>());
    }

    // private list of notification by notification type.
    // we used a list so that notification order can mean something.
    private Map<NotificationType, ArrayList<NotificationHolder>> notificationsListeners =new HashMap<NotificationType, ArrayList<NotificationHolder>>();

    /**
     * Convenience method to support lambdas as callbacks in later version of Java (8+).
     * @param activateNotificationListenerInterface
     * @return greater than zero if added.
     */
    public int addActivateNotificationListener(final ActivateNotificationListenerInterface activateNotificationListenerInterface) {
        injectFault(ExceptionSpot.NotificationCenter_addActivateNotificationListener_spot1);
        if (activateNotificationListenerInterface instanceof ActivateNotificationListener) {
            return addNotificationListener(NotificationType.Activate, (NotificationListener)activateNotificationListenerInterface);
        }
        else {
            return addNotificationListener(NotificationType.Activate, new ActivateNotificationListener() {
                @Override
                public void onActivate(@Nonnull Experiment experiment, @Nonnull String userId, @Nonnull Map<String, String> attributes, @Nonnull Variation variation, @Nonnull LogEvent event) {
                    activateNotificationListenerInterface.onActivate(experiment, userId, attributes, variation, event);
                }
            });
        }
    }

    /**
     * Convenience method to support lambdas as callbacks in later versions of Java (8+)
     * @param trackNotificationListenerInterface
     * @return greater than zero if added.
     */
    public int addTrackNotificationListener(final TrackNotificationListenerInterface trackNotificationListenerInterface) {
        injectFault(ExceptionSpot.NotificationCenter_addTrackNotificationListener_spot1);
        if (trackNotificationListenerInterface instanceof TrackNotificationListener) {
            return addNotificationListener(NotificationType.Activate, (NotificationListener)trackNotificationListenerInterface);
        }
        else {
            return addNotificationListener(NotificationType.Track, new TrackNotificationListener() {
                @Override
                public void onTrack(@Nonnull String eventKey, @Nonnull String userId, @Nonnull Map<String, String> attributes, @Nonnull Map<String, ?> eventTags, @Nonnull LogEvent event) {
                    trackNotificationListenerInterface.onTrack(eventKey, userId, attributes, eventTags, event);
                }
            });
        }
    }

    /**
     * Add a notification listener to the notification center.
     *
     * @param notificationType - enum NotificationType to add.
     * @param notificationListener - Notification to add.
     * @return the notification id used to remove the notification.  It is greater than 0 on success.
     */
    public int addNotificationListener(NotificationType notificationType, NotificationListener notificationListener) {

        injectFault(ExceptionSpot.NotificationCenter_addNotificationListener_spot1);
        Class clazz = notificationType.notificationTypeClass;
        if (clazz == null || !clazz.isInstance(notificationListener)) {
            logger.warn("Notification listener was the wrong type. It was not added to the notification center.");
            return -1;
        }

        for (NotificationHolder holder : notificationsListeners.get(notificationType)) {
            injectFault(ExceptionSpot.NotificationCenter_addNotificationListener_spot2);
            if (holder.notificationListener == notificationListener) {
                logger.warn("Notificication listener was already added");
                return -1;
            }
        }
        int id = notificationListenerID++;
        notificationsListeners.get(notificationType).add(new NotificationHolder(id, notificationListener ));
        logger.info("Notification listener {} was added with id {}", notificationListener.toString(), id);
        injectFault(ExceptionSpot.NotificationCenter_addNotificationListener_spot3);
        return id;
    }

    /**
     * Remove the notification listener based on the notificationId passed back from addNotificationListener.
     * @param notificationID the id passed back from add notification.
     * @return true if removed otherwise false (if the notification is already registered, it returns false).
     */
   public boolean removeNotificationListener(int notificationID) {
       injectFault(ExceptionSpot.NotificationCenter_removeNotificationListener_spot1);
       for (NotificationType type : NotificationType.values()) {
            for (NotificationHolder holder : notificationsListeners.get(type)) {
                if (holder.notificationId == notificationID) {
                    notificationsListeners.get(type).remove(holder);
                    logger.info("Notification listener removed {}", notificationID);
                    return true;
                }
            }
        }

        logger.warn("Notification listener with id {} not found", notificationID);

        return false;
    }

    /**
     * Clear out all the notification listeners.
     */
    public void clearAllNotificationListeners() {
        injectFault(ExceptionSpot.NotificationCenter_clearAllNotificationListeners_spot1);
        for (NotificationType type : NotificationType.values()) {
            clearNotificationListeners(type);
        }
    }

    /**
     * Clear notification listeners by notification type.
     * @param notificationType type of notificationsListeners to remove.
     */
    public void clearNotificationListeners(NotificationType notificationType) {
        injectFault(ExceptionSpot.NotificationCenter_clearNotificationListeners_spot1);
        notificationsListeners.get(notificationType).clear();
    }

    // fire a notificaiton of a certain type.  The arg list changes depending on the type of notification sent.
    public void sendNotifications(NotificationType notificationType, Object ...args) {
        injectFault(ExceptionSpot.NotificationCenter_sendNotifications_spot1);
        ArrayList<NotificationHolder> holders = notificationsListeners.get(notificationType);
        for (NotificationHolder holder : holders) {
            try {
                holder.notificationListener.notify(args);
            }
            catch (Exception e) {
                logger.error("Unexpected exception calling notification listener {}", holder.notificationId, e);
            }
        }
    }

}

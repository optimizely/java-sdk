/**
 *
 *    Copyright 2017, Optimizely and contributors
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

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * This class handles impression and conversion notifications. It replaces NotificationBroadcaster and is intended to be
 * more flexible.
 */
public class NotificationCenter {
    /**
     * NotificationType is used for the notification types supported.
     */
    public enum NotificationType {

        Activate(ActivateNotification.class), // Activate was called. Track an impression event
        Track(TrackNotification.class); // Track was called.  Track a conversion event

        private Class notificationClass;

        NotificationType(Class notificationClass) {
            this.notificationClass = notificationClass;
        }

        public Class getNotificationClass() {
            return notificationClass;
        }
    };


    // the notification id is incremented and is assigned as the callback id, it can then be used to remove the notification.
    private int notificationID = 1;

    final private Logger logger;

    // notification holder holds the id as well as the notification.
    private static class NotificationHolder
    {
        int notificationId;
        Notification notification;

        NotificationHolder(int id, Notification notification) {
            notificationId = id;
            this.notification = notification;
        }
    }

    /**
     * Instantiate a new NotificationCenter
     * @param logger pass in logger to use.
     */
    public NotificationCenter(Logger logger) {
        this.logger = logger;
        notifications.put(NotificationType.Activate, new ArrayList<NotificationHolder>());
        notifications.put(NotificationType.Track, new ArrayList<NotificationHolder>());
    }

    // private list of notification by notification type.
    // we used a list so that notification order can mean something.
    private Map<NotificationType, ArrayList<NotificationHolder>> notifications =new HashMap<NotificationType, ArrayList<NotificationHolder>>();


    /**
     * Add a notification listener to the notification center.
     *
     * @param notificationType - enum NotificationType to add.
     * @param notification - Notification to add.
     * @return the notification id used to remove the notification.  It is greater than 0 on success.
     */
    public int addNotification(NotificationType notificationType, Notification notification) {

        Class clazz = notificationType.notificationClass;
        if (clazz == null || !clazz.isInstance(notification)) {
            logger.warn("Notification listener was the wrong type. It was not added to the notification center.");
            return -1;
        }

        for (NotificationHolder holder : notifications.get(notificationType)) {
            if (holder.notification == notification) {
                logger.warn("Notificication listener was already added");
                return -1;
            }
        }
        int id = notificationID++;
        notifications.get(notificationType).add(new NotificationHolder(id, notification ));
        logger.info("Notification listener {} was added with id {}", notification.toString(), id);
        return id;
    }

    /**
     * Remove the notification listener based on the notificationId passed back from addNotification.
     * @param notificationID the id passed back from add notification.
     * @return true if removed otherwise false (if the notification is already registered, it returns false).
     */
   public boolean removeNotification(int notificationID) {
       for (NotificationType type : NotificationType.values()) {
            for (NotificationHolder holder : notifications.get(type)) {
                if (holder.notificationId == notificationID) {
                    notifications.get(type).remove(holder);
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
    public void clearAllNotifications() {
        for (NotificationType type : NotificationType.values()) {
            clearNotifications(type);
        }
    }

    /**
     * Clear notification listeners by notification type.
     * @param notificationType type of notifications to remove.
     */
    public void clearNotifications(NotificationType notificationType) {
        notifications.get(notificationType).clear();
    }

    // fire a notificaiton of a certain type.  The arg list changes depending on the type of notification sent.
    public void sendNotifications(NotificationType notificationType, Object ...args) {
        ArrayList<NotificationHolder> holders = notifications.get(notificationType);
        for (NotificationHolder holder : holders) {
            try {
                holder.notification.notify(args);
            }
            catch (Exception e) {
                logger.error("Unexpected exception calling notification listener {}", holder.notificationId, e);
            }
        }
    }

}

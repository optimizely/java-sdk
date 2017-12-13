package com.optimizely.ab.notification;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class NotificationCenter {
    public enum NotificationType {
        Activate, // Activate was called.
        Track, // track an impression event
    };


    // the notification id is incremented and is assigned as the callback id, it can then be used to remove the notification.
    private int notificationID = 1;
    // use tuple if available.
    private static class NotificationHolder
    {
        int notificationId;
        Notification notification;

        NotificationHolder(int id, Notification notification) {
            notificationId = id;
            this.notification = notification;
        }
    }

    public NotificationCenter() {
        notifications.put(NotificationType.Activate, new ArrayList<NotificationHolder>());
        notifications.put(NotificationType.Track, new ArrayList<NotificationHolder>());
    }
// dictionary of enum to notification lists.  So, DECISION would have an array of notification holders.
    private Map<NotificationType, ArrayList<NotificationHolder>> notifications =new HashMap<NotificationType, ArrayList<NotificationHolder>>();

    // add a notification to your dictionary.  Look at python implementation for details.
// return notification id
    public int addNotification(NotificationType notificaitonType, Notification notification) {
        for (NotificationHolder holder : notifications.get(notificaitonType)) {
            if (holder.notification == notification) {
                return -1;
            }
        }
        int id = notificationID;
        notifications.get(notificaitonType).add(new NotificationHolder(notificationID++, notification ));
        return id;
    }

    // remove notification by id
   public boolean removeNotifiation(int notificationID) {
        ArrayList<NotificationType> types = new ArrayList<NotificationType>();
        types.add(NotificationType.Activate);
        types.add(NotificationType.Track);

        for (NotificationType type : types) {
            for (NotificationHolder holder : notifications.get(type)) {
                if (holder.notificationId == notificationID) {
                    notifications.get(type).remove(holder);
                    return true;
                }
            }
        }

        return false;
    }

    // clean out all notifications.
    public void cleanAllNotifications() {
        ArrayList<NotificationType> types = new ArrayList<NotificationType>();
        types.add(NotificationType.Activate);
        types.add(NotificationType.Track);

        for (NotificationType type : types) {
            notifications.get(type).clear();
        }
    }

    // clean out notifications by notification type.
    public void clearNotifications(NotificationType notificationType) {
        notifications.get(notificationType).clear();
    }

    // fire a notificaiton of a certain type.  The arg list changes depending on the type of notification sent.
    public void sendNotifications(NotificationType notificationType, Object ...args) {
        ArrayList<NotificationHolder> holders = notifications.get(notificationType);
        for (NotificationHolder holder : holders) {
            holder.notification.notify(args);
        }
    }

}

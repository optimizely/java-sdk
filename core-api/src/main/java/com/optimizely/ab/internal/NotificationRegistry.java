/**
 *
 *    Copyright 2023, Optimizely
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
package com.optimizely.ab.internal;

import com.optimizely.ab.notification.NotificationCenter;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationRegistry {
    private final static Map<String, NotificationCenter> _notificationCenters = new ConcurrentHashMap<>();

    private NotificationRegistry()
    {
    }

    public static NotificationCenter getInternalNotificationCenter(@Nonnull String sdkKey)
    {
        NotificationCenter notificationCenter = null;
        if (sdkKey != null) {
            if (_notificationCenters.containsKey(sdkKey)) {
                notificationCenter = _notificationCenters.get(sdkKey);
            } else {
                notificationCenter = new NotificationCenter();
                _notificationCenters.put(sdkKey, notificationCenter);
            }
        }
        return notificationCenter;
    }

    public static void clearNotificationCenterRegistry(@Nonnull String sdkKey) {
        if (sdkKey != null) {
            _notificationCenters.remove(sdkKey);
        }
    }

}

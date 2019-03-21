/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.notification.decisionInfo;

import com.optimizely.ab.notification.NotificationCenter;

import java.util.Map;

public class DecisionNotification {
    public String type;
    public String userId;
    public Map<String, ?> attributes;
    public Map<String, ?> decisionInfo;
    private NotificationCenter notificationCenter;

    protected DecisionNotification() {
    }

    protected DecisionNotification(NotificationCenter notificationCenter,
                                   String type,
                                   String userId,
                                   Map<String, ?> attributes,
                                   Map<String, ?> decisionInfo) {
        this.type = type;
        this.userId = userId;
        this.attributes = attributes;
        this.decisionInfo = decisionInfo;
        this.notificationCenter = notificationCenter;
    }

    public void sendNotification() {
        notificationCenter.sendNotifications(NotificationCenter.NotificationType.Decision,
            this);
    }
}

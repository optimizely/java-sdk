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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class NotificationRegistryTest {

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getSameNotificationcenterWhenSDKkeyIsNull() {
        String sdkKey = null;
        NotificationCenter notificationCenter1 = NotificationRegistry.getNotificationCenter(sdkKey);
        NotificationCenter notificationCenter2 = NotificationRegistry.getNotificationCenter(sdkKey);
        assertEquals(notificationCenter1, notificationCenter2);
    }

    @Test
    public void getSameNotificationcenterWhenSDKkeyIsSameButNotNull() {
        String sdkKey = "testSDkKey";
        NotificationCenter notificationCenter1 = NotificationRegistry.getNotificationCenter(sdkKey);
        NotificationCenter notificationCenter2 = NotificationRegistry.getNotificationCenter(sdkKey);
        assertEquals(notificationCenter1, notificationCenter2);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getSameNotificationcenterWhenSDKkeyIsNullAndAnotherIsEmpty() {
        String sdkKey1 = "";
        String sdkKey2 = null;
        NotificationCenter notificationCenter1 = NotificationRegistry.getNotificationCenter(sdkKey1);
        NotificationCenter notificationCenter2 = NotificationRegistry.getNotificationCenter(sdkKey2);
        assertEquals(notificationCenter1, notificationCenter2);
    }

    @Test
    public void getDifferentNotificationcenterWhenSDKkeyIsNotSame() {
        String sdkKey1 = "testSDkKey1";
        String sdkKey2 = "testSDkKey2";
        NotificationCenter notificationCenter1 = NotificationRegistry.getNotificationCenter(sdkKey1);
        NotificationCenter notificationCenter2 = NotificationRegistry.getNotificationCenter(sdkKey2);
        Assert.assertNotEquals(notificationCenter1, notificationCenter2);
    }

    @Test
    public void clearRegistryNotificationcenterClearsOldNotificationCenter() {
        String sdkKey1 = "testSDkKey1";
        NotificationCenter notificationCenter1 = NotificationRegistry.getNotificationCenter(sdkKey1);
        NotificationRegistry.clearNotificationCenterRegistry();
        NotificationCenter notificationCenter2 = NotificationRegistry.getNotificationCenter(sdkKey1);

        Assert.assertNotEquals(notificationCenter1, notificationCenter2);
    }
}

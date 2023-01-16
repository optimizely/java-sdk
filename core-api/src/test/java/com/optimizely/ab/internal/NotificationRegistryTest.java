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
import static org.junit.Assert.assertNull;


public class NotificationRegistryTest {

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void getNullNotificationCenterWhenSDKeyIsNull() {
        String sdkKey = null;
        NotificationCenter notificationCenter = NotificationRegistry.getInternalNotificationCenter(sdkKey);
        assertNull(notificationCenter);
    }

    @Test
    public void getSameNotificationCenterWhenSDKKeyIsSameButNotNull() {
        String sdkKey = "testSDkKey";
        NotificationCenter notificationCenter1 = NotificationRegistry.getInternalNotificationCenter(sdkKey);
        NotificationCenter notificationCenter2 = NotificationRegistry.getInternalNotificationCenter(sdkKey);
        assertEquals(notificationCenter1, notificationCenter2);
    }

    @Test
    public void getSameNotificationCenterWhenSDKKeyIsEmpty() {
        String sdkKey1 = "";
        String sdkKey2 = "";
        NotificationCenter notificationCenter1 = NotificationRegistry.getInternalNotificationCenter(sdkKey1);
        NotificationCenter notificationCenter2 = NotificationRegistry.getInternalNotificationCenter(sdkKey2);
        assertEquals(notificationCenter1, notificationCenter2);
    }

    @Test
    public void getDifferentNotificationCenterWhenSDKKeyIsNotSame() {
        String sdkKey1 = "testSDkKey1";
        String sdkKey2 = "testSDkKey2";
        NotificationCenter notificationCenter1 = NotificationRegistry.getInternalNotificationCenter(sdkKey1);
        NotificationCenter notificationCenter2 = NotificationRegistry.getInternalNotificationCenter(sdkKey2);
        Assert.assertNotEquals(notificationCenter1, notificationCenter2);
    }

    @Test
    public void clearRegistryNotificationCenterClearsOldNotificationCenter() {
        String sdkKey1 = "testSDkKey1";
        NotificationCenter notificationCenter1 = NotificationRegistry.getInternalNotificationCenter(sdkKey1);
        NotificationRegistry.clearNotificationCenterRegistry(sdkKey1);
        NotificationCenter notificationCenter2 = NotificationRegistry.getInternalNotificationCenter(sdkKey1);

        Assert.assertNotEquals(notificationCenter1, notificationCenter2);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Test
    public void clearRegistryNotificationCenterWillNotCauseExceptionIfPassedNullSDkKey() {
        String sdkKey1 = "testSDkKey1";
        NotificationCenter notificationCenter1 = NotificationRegistry.getInternalNotificationCenter(sdkKey1);
        NotificationRegistry.clearNotificationCenterRegistry(null);
        NotificationCenter notificationCenter2 = NotificationRegistry.getInternalNotificationCenter(sdkKey1);

        Assert.assertEquals(notificationCenter1, notificationCenter2);
    }
}

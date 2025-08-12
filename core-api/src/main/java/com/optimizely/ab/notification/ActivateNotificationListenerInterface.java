/**
 *
 *    Copyright 2018-2019, Optimizely and contributors
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

import java.util.Map;

import javax.annotation.Nonnull;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;

/**
 * ActivateNotificationListenerInterface provides and interface for activate event notification.
 *
 * @deprecated along with {@link ActivateNotification} and {@link ActivateNotificationListener}
 * and users should implement NotificationHandler&lt;DecisionNotification&gt; directly.
 */
@Deprecated
public interface ActivateNotificationListenerInterface {
    /**
     * onActivate called when an activate was triggered
     *
     * @param experiment - The experiment object being activated.
     * @param userId     - The userId passed into activate.
     * @param attributes - The filtered attribute list passed into activate
     * @param variation  - The variation that was returned from activate.
     * @param event      - The impression event that was triggered.
     */
    public void onActivate(@Nonnull Experiment experiment,
                           @Nonnull String userId,
                           @Nonnull Map<String, ?> attributes,
                           @Nonnull Variation variation,
                           @Nonnull LogEvent event);

}

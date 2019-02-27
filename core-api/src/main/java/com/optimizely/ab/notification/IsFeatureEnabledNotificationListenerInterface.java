/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                   *
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
package com.optimizely.ab.notification;

import javax.annotation.Nonnull;
import java.util.Map;

public interface IsFeatureEnabledNotificationListenerInterface {
    /**
     * onIsFeatureEnabled is called when a IsFeatureEnabled event is triggered
     *
     * @param featureKey  - The unique key of the feature.
     * @param userId      - user id passed into track.
     * @param attributes  - filtered attributes list after passed into track
     * @param featureInfo - map consisting of 3 keys: enabled, source and event. Enabled denotes whether or not feature was enabled for the user, source denoting whether feature was accessed through EXPERIMENT or ROLLOUT and the event object if an event was sent to the Optimizely backend as a result of the feature access
     */
    void onIsFeatureEnabled(@Nonnull String featureKey,
                            @Nonnull String userId,
                            @Nonnull Map<String, ?> attributes,
                            @Nonnull Map<String, ?> featureInfo);
}

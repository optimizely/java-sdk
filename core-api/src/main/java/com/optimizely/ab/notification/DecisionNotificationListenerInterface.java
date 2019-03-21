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

package com.optimizely.ab.notification;

import com.optimizely.ab.notification.decisionInfo.DecisionNotification;

import javax.annotation.Nonnull;

public interface DecisionNotificationListenerInterface {

    /**
     * onDecision called when an activate was triggered
     *
     * @param decisionNotification - The decision notification object containing:
     *                             type         - The notification type.
     *                             userId       - The userId passed to the API.
     *                             attributes   - The attribute map passed to the API.
     *                             decisionInfo - The decision information containing all parameters passed in API.
     */
    void onDecision(@Nonnull DecisionNotification decisionNotification);
}

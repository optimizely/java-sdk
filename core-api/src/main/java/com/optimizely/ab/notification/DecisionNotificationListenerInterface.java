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

import javax.annotation.Nonnull;
import java.util.Map;

public interface DecisionNotificationListenerInterface {

    /**
     * onDecision called when an activate was triggered
     *
     * @param type         - The notification type.
     * @param userId       - The userId passed into API.
     * @param attributes   - The filtered attribute list passed into API.
     * @param decisionInfo - The decision information containing all parameters passed in API.
     */
    void onDecision(@Nonnull String type,
                    @Nonnull String userId,
                    @Nonnull Map<String, ?> attributes,
                    @Nonnull Map<String, ?> decisionInfo);
}

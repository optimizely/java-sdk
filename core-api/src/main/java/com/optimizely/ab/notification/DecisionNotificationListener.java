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
import java.util.HashMap;
import java.util.Map;

public abstract class DecisionNotificationListener implements NotificationListener, DecisionNotificationListenerInterface {

    /**
     * Base notify called with var args.  This method parses the parameters and calls the abstract method.
     *
     * @param args - variable argument list based on the type of notification.
     */
    @Override
    public final void notify(Object... args) {
        assert (args[0] instanceof String);
        String type = (String) args[0];
        assert (args[1] instanceof String);
        String userId = (String) args[1];
        Map<String, ?> attributes = null;
        if (args[2] != null) {
            assert (args[2] instanceof java.util.Map);
            attributes = (Map<String, ?>) args[2];
        } else {
            attributes = new HashMap<>();
        }
        
        assert (args[3] instanceof java.util.Map);
        Map<String, ?> decisionInfo = (Map<String, ?>) args[3];
        onDecision(type, userId, attributes, decisionInfo);
    }

    @Override
    public abstract void onDecision(@Nonnull String type,
                                    @Nonnull String userId,
                                    @Nonnull Map<String, ?> attributes,
                                    @Nonnull Map<String, ?> decisionInfo);
}

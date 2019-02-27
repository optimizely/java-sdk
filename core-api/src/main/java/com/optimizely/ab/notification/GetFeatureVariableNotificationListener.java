/**
 *
 *    Copyright 2019, Optimizely and contributors
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

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public abstract class GetFeatureVariableNotificationListener implements NotificationListener, GetFeatureVariableNotificationListenerInterface {

    @Override
    public void notify(Object... args) {
        assert (args[0] instanceof String);
        String featureKey = (String) args[0];
        assert (args[1] instanceof String);
        String variableKey = (String) args[1];
        assert (args[2] instanceof String);
        String userId = (String) args[2];
        Map<String, ?> attributes = null;
        if (args[3] != null) {
            assert (args[3] instanceof java.util.Map);
            attributes = (Map<String, ?>) args[3];
        }
        Map<String, ?> featureInfo = null;
        if (args[4] != null) {
            assert (args[4] instanceof java.util.Map);
            featureInfo = (HashMap<String, ?>) args[4];
        }

        onGetFeatureVariable(featureKey, variableKey, userId, attributes, featureInfo);
    }

    @Override
    public abstract void onGetFeatureVariable(@Nonnull String featureKey,
                                              @Nonnull String variableKey,
                                              @Nonnull String userId,
                                              @Nonnull Map<String, ?> attributes,
                                              @Nonnull Map<String, ?> featureVariableInfo);
}

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

import java.util.HashMap;
import java.util.Map;

import static com.optimizely.ab.notification.DecisionNotification.ExperimentDecisionNotificationBuilder.EXPERIMENT_KEY;
import static com.optimizely.ab.notification.DecisionNotification.ExperimentDecisionNotificationBuilder.VARIATION_KEY;

public class FeatureTestSourceInfo implements SourceInfo {
    private String experimentKey;
    private String variationKey;

    public FeatureTestSourceInfo(String experimentKey, String variationKey) {
        this.experimentKey = experimentKey;
        this.variationKey = variationKey;
    }

    @Override
    public Map<String, String> get() {
        Map<String, String> sourceInfo = new HashMap<>();
        sourceInfo.put(EXPERIMENT_KEY, experimentKey);
        sourceInfo.put(VARIATION_KEY, variationKey);

        return sourceInfo;
    }
}

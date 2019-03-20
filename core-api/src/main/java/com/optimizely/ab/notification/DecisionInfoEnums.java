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

public class DecisionInfoEnums {
    public enum FeatureVariableDecisionInfo {
        FEATURE_KEY("feature_key"),
        FEATURE_ENABLED("feature_enabled"),
        SOURCE("source"),
        SOURCE_EXPERIMENT_KEY("source_experiment_key"),
        SOURCE_VARIATION_KEY("source_variation_key"),
        VARIABLE_KEY("variable_key"),
        VARIABLE_TYPE("variable_type"),
        VARIABLE_VALUE("variable_value");

        private final String key;

        FeatureVariableDecisionInfo(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }
}

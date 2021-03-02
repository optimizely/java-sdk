/**
 *
 *    Copyright 2016-2017,2019,2021, Optimizely and contributors
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
package com.optimizely.ab.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectConfigUtils {

    /**
     * Helper method for creating convenience mappings from key to entity
     *
     * @param nameables The list of IdMapped entities
     * @param <T> This is the type parameter
     * @return The map of key to entity
     */
    public static <T extends IdKeyMapped> Map<String, T> generateNameMapping(List<T> nameables) {
        Map<String, T> nameMapping = new HashMap<String, T>();
        for (T nameable : nameables) {
            nameMapping.put(nameable.getKey(), nameable);
        }

        return Collections.unmodifiableMap(nameMapping);
    }

    /**
     * Helper method for creating convenience mappings from ID to entity
     *
     * @param nameables The list of IdMapped entities
     * @param <T> This is the type parameter
     * @return The map of ID to entity
     */
    public static <T extends IdMapped> Map<String, T> generateIdMapping(List<T> nameables) {
        Map<String, T> nameMapping = new HashMap<String, T>();
        for (T nameable : nameables) {
            nameMapping.put(nameable.getId(), nameable);
        }

        return Collections.unmodifiableMap(nameMapping);
    }

    /**
     * Helper method for creating convenience mappings of ExperimentID to featureFlags it is included in.
     *
     * @param featureFlags The list of feture flags
     * @return The mapping of ExperimentID to featureFlags
     */
    public static Map<String, List<String>> generateExperimentFeatureMapping(List<FeatureFlag> featureFlags) {
        Map<String, List<String>> experimentFeatureMap = new HashMap<>();
        for (FeatureFlag featureFlag : featureFlags) {
            for (String experimentId : featureFlag.getExperimentIds()) {
                if (experimentFeatureMap.containsKey(experimentId)) {
                    experimentFeatureMap.get(experimentId).add(featureFlag.getKey());
                } else {
                    ArrayList<String> featureFlagKeysList = new ArrayList<>();
                    featureFlagKeysList.add(featureFlag.getKey());
                    experimentFeatureMap.put(experimentId, featureFlagKeysList);
                }
            }
        }
        return Collections.unmodifiableMap(experimentFeatureMap);
    }
}

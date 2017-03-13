package com.optimizely.ab.bucketing;

import java.util.HashMap;
import java.util.Map;

/**
 *
 *    Copyright 2016, Optimizely and contributors
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
public class UserProfileSimple implements UserProfile {

    private Map<String, Map<String, String>> records = new HashMap<String, Map<String, String>>();

    @Override
    public boolean save(String userId, String experimentId, String variationId) {
        Map<String, String> experimentVariationMapping = records.containsKey(userId) ? records.get(userId) : new HashMap<String, String>();
        experimentVariationMapping.put(experimentId, variationId);
        records.put(userId, experimentVariationMapping);
        return true;
    }

    @Override
    public String lookup(String userId, String experimentId) {
        return records.containsKey(userId) ? records.get(userId).get(experimentId) : null;
    }

    @Override
    public boolean remove(String userId, String experimentId) {
        Map<String, String> experimentVariationMapping = records.containsKey(userId) ? records.get(userId) : new HashMap<String, String>();
        experimentVariationMapping.remove(experimentId);
        records.put(userId, experimentVariationMapping);
        return true;
    }

    @Override
    public Map<String, Map<String, String>> getAllRecords() {
        return records;
    }
}

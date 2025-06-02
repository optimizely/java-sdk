/**
 *
 *    Copyright 2016-2019, 2021, Optimizely and contributors
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

import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.EmptyCondition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;

import java.util.List;
import java.util.Map;

public interface ExperimentCore extends IdKeyMapped {
    String getLayerId();
    String getGroupId();
    List<String> getAudienceIds();
    Condition<AudienceIdCondition> getAudienceConditions();
    List<Variation> getVariations();
    List<TrafficAllocation> getTrafficAllocation();
    Map<String, Variation> getVariationKeyToVariationMap();
    Map<String, Variation> getVariationIdToVariationMap();
    Map<String, String> getUserIdToVariationKeyMap();
}
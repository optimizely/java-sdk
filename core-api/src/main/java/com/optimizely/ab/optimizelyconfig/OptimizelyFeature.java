/****************************************************************************
 * Copyright 2020-2021, Optimizely, Inc. and contributors                        *
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
package com.optimizely.ab.optimizelyconfig;

import com.optimizely.ab.config.IdKeyMapped;

import java.util.List;
import java.util.Map;

/**
 * Represents the feature's map in {@link OptimizelyConfig}
 */
public class OptimizelyFeature implements IdKeyMapped {

    private String id;
    private String key;

    private List<OptimizelyExperiment> deliveryRules;
    private List<OptimizelyExperiment> experimentRules;

    private Map<String, OptimizelyExperiment> experimentsMap;
    private Map<String, OptimizelyVariable> variablesMap;

    public OptimizelyFeature(String id,
                              String key, 
                              Map<String, OptimizelyExperiment> experimentsMap,
                              Map<String, OptimizelyVariable> variablesMap) {
        this.id = id;
        this.key = key;
        this.experimentsMap = experimentsMap;
        this.variablesMap = variablesMap;
    }

    public String getId() {
        return id;
    }
    
    public String getKey() {
        return key;
    }

    public Map<String, OptimizelyExperiment> getExperimentsMap() {
        return experimentsMap;
    }

    public Map<String, OptimizelyVariable> getVariablesMap() {
        return variablesMap;
    }

    public List<OptimizelyExperiment> getExperimentRules() { return experimentRules; }

    public List<OptimizelyExperiment> getDeliveryRules() { return deliveryRules; }

    public void setExperimentRules(List<OptimizelyExperiment> experimentRules) {
        this.experimentRules = experimentRules;
    }

    public void setDeliveryRules(List<OptimizelyExperiment> deliveryRules) {
        this.deliveryRules = deliveryRules;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        OptimizelyFeature optimizelyFeature = (OptimizelyFeature) obj;
        return id.equals(optimizelyFeature.getId()) &&
            key.equals(optimizelyFeature.getKey()) &&
            experimentsMap.equals(optimizelyFeature.getExperimentsMap()) &&
            variablesMap.equals(optimizelyFeature.getVariablesMap());
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + experimentsMap.hashCode() + variablesMap.hashCode();
        return result;
    }
}

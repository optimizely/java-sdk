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
package com.optimizely.ab.config.optimizely;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Represents the feature's map in {@link OptimizelyConfig}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OptimizelyFeature {

    @JsonProperty("id")
    private String id;
    @JsonProperty("key")
    private String key;
    @JsonProperty("experimentsMap")
    private Map<String, OptimizelyExperiment> experimentsMap;
    @JsonProperty("variablesMap")
    private Map<String, OptimizelyVariable> variablesMap;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, OptimizelyExperiment> getExperimentsMap() {
        return experimentsMap;
    }

    public void setExperimentsMap(Map<String, OptimizelyExperiment> experimentsMap) {
        this.experimentsMap = experimentsMap;
    }

    public Map<String, OptimizelyVariable> getVariablesMap() {
        return variablesMap;
    }

    public void setVariablesMap(Map<String, OptimizelyVariable> variablesMap) {
        this.variablesMap = variablesMap;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        OptimizelyFeature optimizelyFeature = (OptimizelyFeature) obj;
        return id.equals(optimizelyFeature.getId())
            && experimentsMap.equals(optimizelyFeature.getExperimentsMap())
            && variablesMap.equals(optimizelyFeature.getVariablesMap());
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + experimentsMap.hashCode() + variablesMap.hashCode();
        return result;
    }
}

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
package com.optimizely.ab.optimizelyconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
/**
 * Details of variation in {@link OptimizelyExperiment}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OptimizelyVariation {

    @JsonProperty("id")
    private String id;
    @JsonProperty("key")
    private String key;
    @JsonProperty("featureEnabled")
    private Boolean featureEnabled;
    @JsonProperty("variablesMap")
    private Map<String, OptimizelyVariable> variablesMap;

    public OptimizelyVariation(String id, 
                              String key, 
                              Boolean featureEnabled, 
                              Map<String, OptimizelyVariable> variablesMap) {
        this.id = id;
        this.key = key;
        this.featureEnabled = featureEnabled;
        this.variablesMap = variablesMap;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public Boolean getFeatureEnabled() {
        return featureEnabled;
    }

    public Map<String, OptimizelyVariable> getVariablesMap() {
        return variablesMap;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        OptimizelyVariation optimizelyVariation = (OptimizelyVariation) obj;
        return id.equals(optimizelyVariation.getId()) && variablesMap.equals(optimizelyVariation.getVariablesMap());
    }

    @Override
    public int hashCode() {
        int hash = id.hashCode();
        hash = 31 * hash + variablesMap.hashCode();
        return hash;
    }
}

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
 * Represents the experiment's map in {@link OptimizelyConfig}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OptimizelyExperiment {

    @JsonProperty("id")
    private String id;
    @JsonProperty("key")
    private String key;
    @JsonProperty("variationsMap")
    private Map<String, OptimizelyVariation> variationsMap;

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

    public Map<String, OptimizelyVariation> getVariationsMap() {
        return variationsMap;
    }

    public void setVariationsMap(Map<String, OptimizelyVariation> variationsMap) {
        this.variationsMap = variationsMap;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        OptimizelyExperiment optimizelyExperiment = (OptimizelyExperiment) obj;
        return id.equals(optimizelyExperiment.getId()) && variationsMap.equals(optimizelyExperiment.getVariationsMap());
    }

    @Override
    public int hashCode() {
        int hash = id.hashCode();
        hash = 31 * hash + variationsMap.hashCode();
        return hash;
    }
}

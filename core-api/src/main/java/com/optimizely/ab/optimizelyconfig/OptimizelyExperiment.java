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

import com.optimizely.ab.config.IdKeyMapped;

import java.util.Map;

/**
 * Represents the experiment's map in {@link OptimizelyConfig}
 */
public class OptimizelyExperiment implements IdKeyMapped {

    private String id;
    private String key;
    private Map<String, OptimizelyVariation> variationsMap;

    public OptimizelyExperiment(String id, String key, Map<String, OptimizelyVariation> variationsMap) {
        this.id = id;
        this.key = key;
        this.variationsMap = variationsMap;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public Map<String, OptimizelyVariation> getVariationsMap() {
        return variationsMap;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        OptimizelyExperiment optimizelyExperiment = (OptimizelyExperiment) obj;
        return id.equals(optimizelyExperiment.getId()) &&
            key.equals(optimizelyExperiment.getKey()) &&
            variationsMap.equals(optimizelyExperiment.getVariationsMap());
    }

    @Override
    public int hashCode() {
        int hash = id.hashCode();
        hash = 31 * hash + variationsMap.hashCode();
        return hash;
    }
}

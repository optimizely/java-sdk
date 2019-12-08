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
 * Represents the feature's map in {@link OptimizelyConfig}
 */
public class OptimizelyFeature implements IdKeyMapped {

    private String id;
    private String key;

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
}

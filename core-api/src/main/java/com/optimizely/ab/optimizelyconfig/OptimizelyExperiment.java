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
}

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

/**
 * Details of feature variable in {@link OptimizelyConfig}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OptimizelyVariable {

    @JsonProperty("id")
    private String id;
    @JsonProperty("key")
    private String key;
    @JsonProperty("type")
    private String type;
    @JsonProperty("value")
    private String value;

    public OptimizelyVariable(String id, String key, String type, String value) {
        this.id = id;
        this.key = key;
        this.type = type;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}

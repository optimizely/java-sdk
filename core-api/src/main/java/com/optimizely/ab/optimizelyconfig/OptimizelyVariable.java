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

/**
 * Details of feature variable in {@link OptimizelyConfig}
 */
public class OptimizelyVariable implements IdKeyMapped {

    private String id;
    private String key;
    private String type;
    private String value;

    public OptimizelyVariable(String id,
                              String key,
                              String type,
                              String value) {
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        OptimizelyVariable optimizelyVariable = (OptimizelyVariable) obj;
        return id.equals(optimizelyVariable.getId()) &&
            value.equals(optimizelyVariable.getValue()) &&
            key.equals(optimizelyVariable.getKey()) &&
            type.equals(optimizelyVariable.getType()
        );
    }

    @Override
    public int hashCode() {
        int hash = id.hashCode();
        hash = 31 * hash + value.hashCode();
        return hash;
    }
}

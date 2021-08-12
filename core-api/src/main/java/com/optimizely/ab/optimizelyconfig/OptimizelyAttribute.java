/****************************************************************************
 * Copyright 2021, Optimizely, Inc. and contributors                        *
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
 * Represents the Attribute's map {@link OptimizelyConfig}
 */
public class OptimizelyAttribute  implements IdKeyMapped {

    private String id;
    private String key;

    public OptimizelyAttribute(String id,
                               String key) {
        this.id = id;
        this.key = key;
    }

    public String getId() { return id; }

    public String getKey() { return key; }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        OptimizelyAttribute optimizelyAttribute = (OptimizelyAttribute) obj;
        return id.equals(optimizelyAttribute.getId()) &&
            key.equals(optimizelyAttribute.getKey());
    }

    @Override
    public int hashCode() {
        int hash = id.hashCode();
        hash = 31 * hash + key.hashCode();
        return hash;
    }
}

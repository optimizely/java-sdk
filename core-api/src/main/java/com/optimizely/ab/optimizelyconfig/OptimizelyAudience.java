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
import com.optimizely.ab.config.audience.Condition;

import java.util.List;

/**
 * Represents the Audiences list {@link OptimizelyConfig}
 */
public class OptimizelyAudience implements IdKeyMapped{

    private String id;
    private String name;
    private String conditions;

    public OptimizelyAudience(String id,
                              String name,
                              String conditions) {
        this.id = id;
        this.name = name;
        this.conditions = conditions;
    }

    public String getId() { return id; }

    public String getName() { return name; }

    public String getKey() { return name; }

    public String getConditions() { return conditions; }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        OptimizelyAudience optimizelyAudience = (OptimizelyAudience) obj;
        return id.equals(optimizelyAudience.getId()) &&
            name.equals(optimizelyAudience.getKey()) &&
            conditions.equals(optimizelyAudience.getConditions());
    }

    @Override
    public int hashCode() {
        int hash = id.hashCode();
        hash = 31 * hash + conditions.hashCode();
        return hash;
    }

}

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

import java.util.List;

/**
 * Represents the Events's map {@link OptimizelyConfig}
 */
public class OptimizelyEvent implements IdKeyMapped {

    private String id;
    private String key;
    private List<String> experimentIds;

    public OptimizelyEvent(String id,
                           String key,
                           List<String> experimentIds) {
        this.id = id;
        this.key = key;
        this.experimentIds = experimentIds;
    }

    public String getId() { return id; }

    public String getKey() { return key; }

    public List<String> getExperimentIds() { return experimentIds; }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        OptimizelyEvent optimizelyEvent = (OptimizelyEvent) obj;
        return id.equals(optimizelyEvent.getId()) &&
            key.equals(optimizelyEvent.getKey()) &&
            experimentIds.equals(optimizelyEvent.getExperimentIds());
    }

    @Override
    public int hashCode() {
        int hash = id.hashCode();
        hash = 31 * hash + experimentIds.hashCode();
        return hash;
    }
}

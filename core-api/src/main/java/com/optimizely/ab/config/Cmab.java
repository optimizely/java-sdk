/**
 *
 *    Copyright 2025 Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.config;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * Represents the Optimizely Traffic Allocation configuration.
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Cmab {

    private final List<String> attributeIds;
    private final int trafficAllocation;

    @JsonCreator
    public Cmab(@JsonProperty("attributeIds") List<String> attributeIds,
                @JsonProperty("trafficAllocation") int trafficAllocation) {
        this.attributeIds = attributeIds;
        this.trafficAllocation = trafficAllocation;
    }

    public List<String> getAttributeIds() {
        return attributeIds;
    }

    public int getTrafficAllocation() {
        return trafficAllocation;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Cmab cmab = (Cmab) obj;
        return trafficAllocation == cmab.trafficAllocation &&
               Objects.equals(attributeIds, cmab.attributeIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeIds, trafficAllocation);
    }

    @Override
    public String toString() {
        return "Cmab{" +
            "attributeIds=" + attributeIds +
            ", trafficAllocation=" + trafficAllocation +
            '}';
    }
}
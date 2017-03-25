/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the Optimizely Traffic Allocation configuration.
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrafficAllocation {

    private final String entityId;
    private final int endOfRange;

    @JsonCreator
    public TrafficAllocation(@JsonProperty("entityId") String entityId,
                             @JsonProperty("endOfRange") int endOfRange) {
        this.entityId = entityId;
        this.endOfRange = endOfRange;
    }

    public String getEntityId() {
        return entityId;
    }

    public int getEndOfRange() {
        return endOfRange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrafficAllocation that = (TrafficAllocation) o;

        if (endOfRange != that.endOfRange) return false;
        return entityId.equals(that.entityId);
    }

    @Override
    public int hashCode() {
        int result = entityId.hashCode();
        result = 31 * result + endOfRange;
        return result;
    }

    @Override
    public String toString() {
        return "TrafficAllocation{" +
               "entityId='" + entityId + '\'' +
               ", endOfRange=" + endOfRange +
               '}';
    }
}


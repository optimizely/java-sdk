/**
 *
 *    Copyright 2017, Optimizely and contributors
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
import com.optimizely.ab.faultinjection.ExceptionSpot;
import com.optimizely.ab.faultinjection.FaultInjectionManager;

import java.util.List;
import java.util.Map;

/**
 * Represents a FeatureFlag definition at the project level
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureFlag implements IdKeyMapped{

    private final String id;
    private final String key;
    private final String rolloutId;
    private final List<String> experimentIds;
    private final List<LiveVariable> variables;
    private final Map<String, LiveVariable> variableKeyToLiveVariableMap;

    private static void injectFault(ExceptionSpot spot) {
        FaultInjectionManager.getInstance().injectFault(spot);
    }

    @JsonCreator
    public FeatureFlag(@JsonProperty("id") String id,
                       @JsonProperty("key") String key,
                       @JsonProperty("rolloutId") String rolloutId,
                       @JsonProperty("experimentIds") List<String> experimentIds,
                       @JsonProperty("variables") List<LiveVariable> variables) {

        injectFault(ExceptionSpot.FeatureFlag_constructor_spot1);
        this.id = id;
        this.key = key;
        this.rolloutId = rolloutId;
        this.experimentIds = experimentIds;
        this.variables = variables;
        this.variableKeyToLiveVariableMap = ProjectConfigUtils.generateNameMapping(variables);
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getRolloutId() {
        return rolloutId;
    }

    public List<String> getExperimentIds() {
        return experimentIds;
    }

    public List<LiveVariable> getVariables() {
        return variables;
    }

    public Map<String, LiveVariable> getVariableKeyToLiveVariableMap() {
        return variableKeyToLiveVariableMap;
    }

    @Override
    public String toString() {
        injectFault(ExceptionSpot.FeatureFlag_toString_spot1);
        return "FeatureFlag{" +
                "id='" + id + '\'' +
                ", key='" + key + '\'' +
                ", rolloutId='" + rolloutId + '\'' +
                ", experimentIds=" + experimentIds +
                ", variables=" + variables +
                ", variableKeyToLiveVariableMap=" + variableKeyToLiveVariableMap +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        injectFault(ExceptionSpot.FeatureFlag_equals_spot1);
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeatureFlag that = (FeatureFlag) o;

        injectFault(ExceptionSpot.FeatureFlag_equals_spot1);
        if (!id.equals(that.id)) return false;
        if (!key.equals(that.key)) return false;
        if (!rolloutId.equals(that.rolloutId)) return false;
        if (!experimentIds.equals(that.experimentIds)) return false;
        if (!variables.equals(that.variables)) return false;
        return variableKeyToLiveVariableMap.equals(that.variableKeyToLiveVariableMap);
    }

    @Override
    public int hashCode() {
        injectFault(ExceptionSpot.FeatureFlag_hashCode_spot1);
        int result = id.hashCode();
        result = 31 * result + key.hashCode();
        result = 31 * result + rolloutId.hashCode();
        result = 31 * result + experimentIds.hashCode();
        result = 31 * result + variables.hashCode();
        result = 31 * result + variableKeyToLiveVariableMap.hashCode();
        return result;
    }
}

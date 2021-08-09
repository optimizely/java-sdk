/**
 *
 *    Copyright 2016-2017, 2019, Optimizely and contributors
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the Optimizely Variation configuration.
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Variation implements IdKeyMapped {

    private final String id;
    private final String key;
    private final Boolean featureEnabled;
    private final List<FeatureVariableUsageInstance> featureVariableUsageInstances;
    private final Map<String, FeatureVariableUsageInstance> variableIdToFeatureVariableUsageInstanceMap;

    public Variation(String id, String key) {
        this(id, key, null);
    }

    public Variation(String id,
                     String key,
                     List<FeatureVariableUsageInstance> featureVariableUsageInstances) {
        this(id, key, null, featureVariableUsageInstances);
    }

    @JsonCreator
    public Variation(@JsonProperty("id") String id,
                     @JsonProperty("key") String key,
                     @JsonProperty("featureEnabled") Boolean featureEnabled,
                     @JsonProperty("variables") List<FeatureVariableUsageInstance> featureVariableUsageInstances) {
        this.id = id;
        this.key = key;
        if (featureEnabled != null)
            this.featureEnabled = featureEnabled;
        else
            this.featureEnabled = null;
        if (featureVariableUsageInstances == null) {
            this.featureVariableUsageInstances = Collections.emptyList();
        } else {
            this.featureVariableUsageInstances = featureVariableUsageInstances;
        }
        this.variableIdToFeatureVariableUsageInstanceMap = ProjectConfigUtils.generateIdMapping(this.featureVariableUsageInstances);
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getKey() {
        return key;
    }

    @Nonnull
    public Boolean getFeatureEnabled() {
        return featureEnabled;
    }

    @Nullable
    public List<FeatureVariableUsageInstance> getFeatureVariableUsageInstances() {
        return featureVariableUsageInstances;
    }

    public Map<String, FeatureVariableUsageInstance> getVariableIdToFeatureVariableUsageInstanceMap() {
        return variableIdToFeatureVariableUsageInstanceMap;
    }

    public boolean is(String otherKey) {
        return key.equals(otherKey);
    }

    @Override
    public String toString() {
        return "Variation{" +
            "id='" + id + '\'' +
            ", key='" + key + '\'' +
            '}';
    }
}

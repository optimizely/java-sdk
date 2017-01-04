/**
 *
 *    Copyright 2016, Optimizely and contributors
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

import java.util.List;

import javax.annotation.concurrent.Immutable;

/**
 * Represents a Optimizely Group configuration
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Group implements IdMapped {

    private final String id;
    private final String policy;
    private final List<TrafficAllocation> trafficAllocation;
    private final List<Experiment> experiments;

    public static final String RANDOM_POLICY = "random";

    @JsonCreator
    public Group(@JsonProperty("id") String id,
                 @JsonProperty("policy") String policy,
                 @JsonProperty("experiments") List<Experiment> experiments,
                 @JsonProperty("trafficAllocation") List<TrafficAllocation> trafficAllocation) {
        this.id = id;
        this.policy = policy;
        this.trafficAllocation = trafficAllocation;
        this.experiments = experiments;
    }

    public String getId() {
        return id;
    }

    public String getPolicy() {
        return policy;
    }

    public List<TrafficAllocation> getTrafficAllocation() {
        return trafficAllocation;
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    @Override
    public String toString() {
        return "Group{" +
                "id='" + id + '\'' +
                ", policy='" + policy + '\'' +
                ", experiments=" + experiments +
                ", trafficAllocation=" + trafficAllocation +
                '}';
    }
}

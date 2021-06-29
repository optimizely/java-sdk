/****************************************************************************
 * Copyright 2020-2021, Optimizely, Inc. and contributors                        *
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


import com.fasterxml.jackson.annotation.JsonInclude;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;

import java.util.*;

/**
 * Interface for OptimizleyConfig
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OptimizelyConfig {
    
    private Map<String, OptimizelyExperiment> experimentsMap;
    private Map<String, OptimizelyFeature> featuresMap;
    private List<OptimizelyAttribute> attributes;
    private List<OptimizelyEvent> events;
    private String revision;
    private String sdkKey;
    private String environmentKey;
    private String datafile;

    public OptimizelyConfig(Map<String, OptimizelyExperiment> experimentsMap, 
                            Map<String, OptimizelyFeature> featuresMap,
                            String revision, String sdkKey, String environmentKey) {
        this(experimentsMap, featuresMap, revision, sdkKey, environmentKey, null);
    }

    public OptimizelyConfig(Map<String, OptimizelyExperiment> experimentsMap,
                            Map<String, OptimizelyFeature> featuresMap,
                            String revision,
                            String sdkKey,
                            String environmentKey,
                            String datafile) {
        this.experimentsMap = experimentsMap;
        this.featuresMap = featuresMap;
        this.revision = revision;
        this.sdkKey = sdkKey;
        this.environmentKey = environmentKey;
        this.datafile = datafile;
    }

    public OptimizelyConfig(Map<String, OptimizelyExperiment> experimentsMap,
                            Map<String, OptimizelyFeature> featuresMap,
                            String revision, String sdkKey, String environmentKey,
                            List<OptimizelyAttribute> attributes,
                            List<OptimizelyEvent> events) {
        this(experimentsMap, featuresMap, revision, sdkKey, environmentKey, attributes, events, null);
    }

    public OptimizelyConfig(Map<String, OptimizelyExperiment> experimentsMap,
                            Map<String, OptimizelyFeature> featuresMap,
                            String revision,
                            String sdkKey,
                            String environmentKey,
                            List<OptimizelyAttribute> attributes,
                            List<OptimizelyEvent> events,
                            String datafile) {
        this.experimentsMap = experimentsMap;
        this.featuresMap = featuresMap;
        this.revision = revision;
        this.sdkKey = sdkKey;
        this.environmentKey = environmentKey;
        this.attributes = attributes;
        this.events = events;
        this.datafile = datafile;
    }

    public Map<String, OptimizelyExperiment> getExperimentsMap() {
        return experimentsMap;
    }

    public Map<String, OptimizelyFeature> getFeaturesMap() {
        return featuresMap;
    }

    public List<OptimizelyAttribute> getAttributes() { return attributes; }

    public List<OptimizelyEvent> getEvents() { return events; }

    public String getRevision() {
        return revision;
    }

    public String getSdkKey() { return sdkKey; }

    public String getEnvironmentKey() {
        return environmentKey;
    }

    public String getDatafile() {
        return datafile;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        OptimizelyConfig optimizelyConfig = (OptimizelyConfig) obj;
        return revision.equals(optimizelyConfig.getRevision()) &&
            experimentsMap.equals(optimizelyConfig.getExperimentsMap()) &&
            featuresMap.equals(optimizelyConfig.getFeaturesMap());
    }

    @Override
    public int hashCode() {
        int hash = revision.hashCode();
        hash = 31 * hash + experimentsMap.hashCode();
        return hash;
    }
}

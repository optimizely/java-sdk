/****************************************************************************
 * Copyright 2020, Optimizely, Inc. and contributors                        *
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


import java.util.*;

/**
 * Interface for OptimizleyConfig
 */
public class OptimizelyConfig {
    
    private Map<String, OptimizelyExperiment> experimentsMap;
    private Map<String, OptimizelyFeature> featuresMap;
    private String revision;
    private String datafile;

    public OptimizelyConfig(Map<String, OptimizelyExperiment> experimentsMap, 
                            Map<String, OptimizelyFeature> featuresMap,
                            String revision) {
        this(experimentsMap, featuresMap, revision, null);
    }

    public OptimizelyConfig(Map<String, OptimizelyExperiment> experimentsMap,
                            Map<String, OptimizelyFeature> featuresMap,
                            String revision,
                            String datafile) {
        this.experimentsMap = experimentsMap;
        this.featuresMap = featuresMap;
        this.revision = revision;
        this.datafile = datafile;
    }

    public Map<String, OptimizelyExperiment> getExperimentsMap() {
        return experimentsMap;
    }

    public Map<String, OptimizelyFeature> getFeaturesMap() {
        return featuresMap;
    }

    public String getRevision() {
        return revision;
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

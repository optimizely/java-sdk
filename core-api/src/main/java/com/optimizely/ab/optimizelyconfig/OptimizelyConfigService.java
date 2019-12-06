/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
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

import com.optimizely.ab.config.*;

import java.util.*;

public class OptimizelyConfigService {

    /**
     * create maps for experiment and features to be returned as one object
     *
     * @param projectConfig the current projectConfig
     * @return {@link OptimizelyConfig} containing experiments and features
     */
    public OptimizelyConfig getOptimizelyConfig(ProjectConfig projectConfig) {
        Map<String, OptimizelyExperiment> experimentsMap = getExperimentsMap(projectConfig);
        OptimizelyConfig optimizelyConfig = new OptimizelyConfig(
            experimentsMap,
            getFeaturesMap(projectConfig, experimentsMap),
            projectConfig.getRevision()
        );
        return optimizelyConfig;
    }

    /**
     * gets map of {@link Experiment} except rollouts
     *
     * @param   projectConfig the current projectConfig
     * @return  map of experiments
     */
    private Map<String, OptimizelyExperiment> getExperimentsMap(ProjectConfig projectConfig) {
        Map<String, OptimizelyExperiment> experimentsMap = new HashMap<>();
        List<Experiment> experiments = projectConfig.getExperiments();
        if(experiments != null) {
            for (Experiment experiment : experiments) {
                OptimizelyExperiment optimizelyExperiment = new OptimizelyExperiment(
                    experiment.getId(),
                    experiment.getKey(),
                    getVariationsMap(projectConfig, experiment.getVariations(), experiment.getId())
                );
                experimentsMap.put(experiment.getKey(), optimizelyExperiment);            
            }
        }
        return experimentsMap;
    }

    private Map<String, OptimizelyVariation> getVariationsMap(ProjectConfig projectConfig, List<Variation> variations, String experimentId) {
        Map<String, OptimizelyVariation> variationsMap = new HashMap<>();
        if (variations != null) {
            for (Variation variation : variations) {
                List<String> features = projectConfig.getExperimentFeatureKeyMapping().get(experimentId);
                Boolean isFeatureEnabled = features != null && !features.isEmpty() ? variation.getFeatureEnabled() : null;
                OptimizelyVariation optimizelyVariation = new OptimizelyVariation(
                    variation.getId(),
                    variation.getKey(),
                    isFeatureEnabled,
                    getMergedVariablesMap(projectConfig, variation, experimentId)
                );
                variationsMap.put(variation.getKey(), optimizelyVariation);
            }
        }
        return variationsMap;
    }

    /**
     * Merges feature key and type from feature variables to variation variables
     *
     * @param projectConfig             the current projectConfig
     * @param variation                 variation for which the value of {@link FeatureVariable} needs to be merged
     * @param experimentId              {@link Experiment} id for getting its feature id
     * @param featureVariablesMap       map for {@link FeatureVariable} of a {@link FeatureFlag}
     * @return optimizelyVariableMap    returns the map after merging the value of {@link FeatureVariable}
     */
    private Map<String, OptimizelyVariable> getMergedVariablesMap(ProjectConfig projectConfig, Variation variation, String experimentId) {

        Map<String, OptimizelyVariable> optimizelyVariableMap = new HashMap<>();
        Map<String, List<FeatureVariable>> featureIDVariablesMap = new HashMap<>();
        
        List<String> featureKeys = projectConfig.getExperimentFeatureKeyMapping().get(experimentId);
        String featureKey = featureKeys != null ? featureKeys.get(0) : null;

        List<FeatureFlag> featureFlags = projectConfig.getFeatureFlags();
        if (featureFlags != null) {
            for (FeatureFlag featureFlag : featureFlags) {
                featureIDVariablesMap.put(featureFlag.getKey(), featureFlag.getVariables());
            }
        }

        Map<String, OptimizelyVariable> tempVariableIdMap = new HashMap();
        if (featureKey != null) {
            for (FeatureVariableUsageInstance variable : variation.getFeatureVariableUsageInstances()) {
                OptimizelyVariable tempVariable = new OptimizelyVariable(variable.getId(), null, null, variable.getValue());
                tempVariableIdMap.put(tempVariable.getId(), tempVariable);
            }
            if(featureIDVariablesMap.get(featureKey) != null) {
                featureIDVariablesMap.get(featureKey).forEach(featureVariable -> {
                    OptimizelyVariable tempVariable = tempVariableIdMap.get(featureVariable.getId());
                    String variableValue = variation.getFeatureEnabled() && tempVariable != null ? tempVariable.getValue() : featureVariable.getDefaultValue();
                    OptimizelyVariable optimizelyVariable = new OptimizelyVariable(
                        featureVariable.getId(),
                        featureVariable.getKey(),
                        featureVariable.getType().getVariableType().toLowerCase(),
                        variableValue
                    );
                    optimizelyVariableMap.put(featureVariable.getKey(), optimizelyVariable);
                });
            }
        }
        return optimizelyVariableMap;
    }

    /**
     * gets map of all {@link FeatureFlag}s and associated {@link Experiment} map inside it
     *
     * @param projectConfig the current projectConfig
     * @return map of {@link FeatureFlag} key and {@link OptimizelyFeature}
     */
    private Map<String, OptimizelyFeature> getFeaturesMap(ProjectConfig projectConfig, Map<String, OptimizelyExperiment> experimentMap) {
        Map<String, OptimizelyFeature> featuresMap = new HashMap<>();
        List<FeatureFlag> featureFlags = projectConfig.getFeatureFlags();
        if(featureFlags != null) {
            for (FeatureFlag featureFlag : featureFlags) {
                OptimizelyFeature optimizelyFeature = new OptimizelyFeature(
                    featureFlag.getId(),
                    featureFlag.getKey(),
                    getFeatureExperimentMap(featureFlag.getExperimentIds(), experimentMap),
                    getFeatureVariableMap(featureFlag.getVariables())
                );
                featuresMap.put(featureFlag.getKey(), optimizelyFeature);
            }
        }
        return featuresMap;
    }

    /**
     * Gets the feature experiment only
     *
     * @param experimentIds feature experiments to be filtered for feature map
     * @return experimentMap for feature map
     */
    private Map<String, OptimizelyExperiment> getFeatureExperimentMap(List<String> experimentIds,
                                                                      Map<String, OptimizelyExperiment> experimentsMap) {
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = new HashMap<>();
        if (experimentIds != null) {
            for (String experimentId : experimentIds) {
                experimentsMap.forEach((experimentKey, experiment) -> {
                    if(experiment.getId().equals(experimentId))
                        optimizelyExperimentMap.put(experimentKey, experiment);
                });
            }
        }
        return optimizelyExperimentMap;
    }

    /**
     * Add the details for feature variables
     *
     * @param featureVariables feature variables for feature experiments
     * @return variables map for feature map
     */
    private Map<String, OptimizelyVariable> getFeatureVariableMap(List<FeatureVariable> featureVariables) {
        Map<String, OptimizelyVariable> optimizelyVariableMap = new HashMap<>();
        if (featureVariables != null) {
            for (FeatureVariable featureVariable : featureVariables) {
                OptimizelyVariable optimizelyVariable = new OptimizelyVariable(
                  featureVariable.getId(),
                  featureVariable.getKey(),
                  featureVariable.getType().getVariableType().toLowerCase(),
                  featureVariable.getDefaultValue()
                );
                optimizelyVariableMap.put(featureVariable.getKey(), optimizelyVariable);
            }
        }
        return optimizelyVariableMap;
    }
}

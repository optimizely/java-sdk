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
        OptimizelyConfig optimizelyConfig = new OptimizelyConfig();
        optimizelyConfig.setRevision(projectConfig.getRevision());
        optimizelyConfig.setExperimentsMap(getExperimentsMap(projectConfig));
        optimizelyConfig.setFeaturesMap(getFeaturesMap(projectConfig,  optimizelyConfig.getExperimentsMap()));
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
        // experiment id's of the experiments for all rollouts
        List<String> rolloutExperimentIds = getRolloutExperimentIds(projectConfig.getRollouts());
        // reduce feature variables to key value pair map
        Map<String, FeatureVariable> featureVariablesMap = getFeatureVariablesMap(projectConfig.getFeatureFlags());
        List<Experiment> experiments = projectConfig.getExperiments();

        if(experiments != null)
            for (Experiment experiment : experiments)
                if (!rolloutExperimentIds.contains(experiment.getId())) {
                    Map<String, OptimizelyVariation> variationMap = new HashMap<>();
                    // searching for variables in all variations
                    List<Variation> variations = experiment.getVariations();
                    if (variations != null)
                        for (Variation variation : variations) {
                            OptimizelyVariation optimizelyVariation = new OptimizelyVariation();
                            optimizelyVariation.setId(variation.getId());
                            optimizelyVariation.setKey(variation.getKey());
                            List<String> features = projectConfig.getExperimentFeatureKeyMapping().get(experiment.getId());
                            // set the value only if feature experiment
                            optimizelyVariation.setFeatureEnabled(features != null && !features.isEmpty() ? variation.getFeatureEnabled() : null);
                            optimizelyVariation.setVariablesMap(getMergedVariablesMap(projectConfig, variation, experiment.getId(), featureVariablesMap));
                            variationMap.put(variation.getKey(), optimizelyVariation);
                        }
                    // creating experiment map having all the experiment details
                    OptimizelyExperiment optimizelyExperiment = new OptimizelyExperiment();
                    optimizelyExperiment.setId(experiment.getId());
                    optimizelyExperiment.setKey(experiment.getKey());
                    optimizelyExperiment.setVariationsMap(variationMap);
                    experimentsMap.put(experiment.getKey(), optimizelyExperiment);
                }
        return experimentsMap;
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
        if(featureFlags != null)
            // get features map
            for (FeatureFlag featureFlag : featureFlags) {
                // setting up feature
                OptimizelyFeature optimizelyFeature = new OptimizelyFeature();
                optimizelyFeature.setId(featureFlag.getId());
                optimizelyFeature.setKey(featureFlag.getKey());
                optimizelyFeature.setExperimentsMap(getFeatureExperimentMap(featureFlag.getExperimentIds(), experimentMap));
                optimizelyFeature.setVariablesMap(getFeatureVariableMap(featureFlag.getVariables()));
                featuresMap.put(featureFlag.getKey(), optimizelyFeature);
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
                OptimizelyVariable optimizelyVariable = new OptimizelyVariable();
                optimizelyVariable.setId(featureVariable.getId());
                optimizelyVariable.setKey(featureVariable.getKey());
                optimizelyVariable.setType(featureVariable.getType().getVariableType().toLowerCase());
                optimizelyVariable.setValue(featureVariable.getDefaultValue());
                optimizelyVariableMap.put(featureVariable.getKey(), optimizelyVariable);
            }
        }
        return optimizelyVariableMap;
    }

    /**
     * Returns list of experiment id's of the experiments for all rollouts
     *
     * @param rollouts rollouts from {@link ProjectConfig}
     * @return List of {@link Experiment} id's
     */
    private List<String> getRolloutExperimentIds(List<Rollout> rollouts) {
        List<String> experimentIds = new ArrayList<>();
        // project having rollouts
        if(rollouts != null)
            for (Rollout rollout : rollouts)
                if (rollout.getExperiments() != null)
                    for (Experiment experiment : rollout.getExperiments())
                        experimentIds.add(experiment.getId());
        return experimentIds;
    }

    /**
     * Returns Map for all features which contains List of Variables
     *
     * @param featureFlags features rollouts from {@link ProjectConfig}
     * @return {@link FeatureVariable} for all the features in the current project
     */
    private Map<String, FeatureVariable> getFeatureVariablesMap(List<FeatureFlag> featureFlags) {
        Map<String, FeatureVariable> featureVariablesMap = new HashMap<>();
        if (featureFlags != null)
            for(FeatureFlag featureFlag : featureFlags)
                for(FeatureVariable variable : featureFlag.getVariables())
                    featureVariablesMap.put(variable.getId(), variable);
        return featureVariablesMap;
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
    private Map<String, OptimizelyVariable> getMergedVariablesMap(ProjectConfig projectConfig,
                                                                  Variation variation,
                                                                  String experimentId,
                                                                  Map<String, FeatureVariable> featureVariablesMap) {

        Map<String, OptimizelyVariable> optimizelyVariableMap = new HashMap<>();
        Map<String, List<FeatureVariable>> featureIDVariablesMap = new HashMap<>();

        List<String> featureList = generateExperimentFeatureIdMapping(projectConfig.getFeatureFlags()).get(experimentId);
        List<FeatureVariableUsageInstance> featureVariableUsageInstances = variation.getFeatureVariableUsageInstances();

        List<FeatureFlag> featureFlags = projectConfig.getFeatureFlags();
        if (featureFlags != null) {
            for (FeatureFlag featureFlag : featureFlags)
                featureIDVariablesMap.put(featureFlag.getId(), featureFlag.getVariables());
        }

        if (featureList != null) {
            if (featureVariableUsageInstances != null) {
                for (FeatureVariableUsageInstance featureVariableUsageInstance : featureVariableUsageInstances) {
                    OptimizelyVariable optimizelyVariable = new OptimizelyVariable();
                    optimizelyVariable.setId(featureVariableUsageInstance.getId());
                    optimizelyVariable.setValue(variation.getFeatureEnabled()
                        ? featureVariableUsageInstance.getValue()
                        : featureVariablesMap.get(featureVariableUsageInstance.getId()).getDefaultValue());
                    optimizelyVariable.setKey(featureVariablesMap.get(featureVariableUsageInstance.getId()).getKey());
                    optimizelyVariable.setType(featureVariablesMap.get(featureVariableUsageInstance.getId()).getType().getVariableType().toLowerCase());
                    optimizelyVariableMap.put(featureVariablesMap.get(featureVariableUsageInstance.getId()).getKey(), optimizelyVariable);
                }
            }
            featureList.forEach(featId -> {
                if(featureIDVariablesMap.get(featId) != null) {
                    featureIDVariablesMap.get(featId).forEach(featureVariable -> {
                        if (!optimizelyVariableMap.containsKey(featureVariable.getKey())) {
                            OptimizelyVariable optimizelyVariable = new OptimizelyVariable();
                            optimizelyVariable.setId(featureVariable.getId());
                            optimizelyVariable.setValue(featureVariable.getDefaultValue());
                            optimizelyVariable.setKey(featureVariable.getKey());
                            optimizelyVariable.setType(featureVariable.getType().getVariableType().toLowerCase());
                            optimizelyVariableMap.put(featureVariable.getKey(), optimizelyVariable);
                        }
                    });
                }
            });
        }
        return optimizelyVariableMap;
    }

    /**
     * get experiment feature id mappings
     *
     * @param featureFlags all the feature flags in project config
     * @return map of experiments feature id's
     */
    private static Map<String, List<String>> generateExperimentFeatureIdMapping(List<FeatureFlag> featureFlags) {
        Map<String, List<String>> experimentFeatureMap = new HashMap<>();
        for (FeatureFlag featureFlag : featureFlags) {
            for (String experimentId : featureFlag.getExperimentIds()) {
                if (experimentFeatureMap.containsKey(experimentId)) {
                    experimentFeatureMap.get(experimentId).add(featureFlag.getId());
                } else {
                    ArrayList<String> featureFlagKeysList = new ArrayList<>();
                    featureFlagKeysList.add(featureFlag.getId());
                    experimentFeatureMap.put(experimentId, featureFlagKeysList);
                }
            }
        }
        return Collections.unmodifiableMap(experimentFeatureMap);
    }
}

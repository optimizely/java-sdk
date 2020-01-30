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

import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.config.*;
import java.util.*;

public class OptimizelyConfigService {

    private ProjectConfig projectConfig;
    private OptimizelyConfig optimizelyConfig;

    public OptimizelyConfigService(ProjectConfig projectConfig) {
        this.projectConfig = projectConfig;

        Map<String, OptimizelyExperiment> experimentsMap = getExperimentsMap();
        optimizelyConfig = new OptimizelyConfig(
            experimentsMap,
            getFeaturesMap(experimentsMap),
            projectConfig.getRevision()
        );
    }

    /**
     * returns maps for experiment and features to be returned as one object
     *
     * @return {@link OptimizelyConfig} containing experiments and features
     */
    public OptimizelyConfig getConfig() {
        return optimizelyConfig;
    }

    /**
     * Generates a Map which contains list of variables for each feature key.
     * This map is used for merging variation and feature variables.
     */
    @VisibleForTesting
    Map<String, List<FeatureVariable>> generateFeatureKeyToVariablesMap() {
        List<FeatureFlag> featureFlags = projectConfig.getFeatureFlags();
        if (featureFlags == null) {
            return Collections.emptyMap();
        }
        Map<String, List<FeatureVariable>> featureVariableIdMap = new HashMap<>();
        for (FeatureFlag featureFlag : featureFlags) {
            featureVariableIdMap.put(featureFlag.getKey(), featureFlag.getVariables());
        }
        return featureVariableIdMap;
    }

    @VisibleForTesting
    String getExperimentFeatureKey(String experimentId) {
        List<String> featureKeys = projectConfig.getExperimentFeatureKeyMapping().get(experimentId);
        return featureKeys != null ? featureKeys.get(0) : null;
    }

    @VisibleForTesting
    Map<String, OptimizelyExperiment> getExperimentsMap() {
        List<Experiment> experiments = projectConfig.getExperiments();
        if (experiments == null) {
            return Collections.emptyMap();
        }
        Map<String, OptimizelyExperiment> featureExperimentMap = new HashMap<>();
        for (Experiment experiment : experiments) {
            featureExperimentMap.put(experiment.getKey(), new OptimizelyExperiment(
                experiment.getId(),
                experiment.getKey(),
                getVariationsMap(experiment.getVariations(), experiment.getId())
            ));
        }
        return featureExperimentMap;
    }

    @VisibleForTesting
    Map<String, OptimizelyVariation> getVariationsMap(List<Variation> variations, String experimentId) {
        if (variations == null) {
            return Collections.emptyMap();
        }
        Boolean isFeatureExperiment = this.getExperimentFeatureKey(experimentId) != null;
        Map<String, OptimizelyVariation> variationKeyMap = new HashMap<>();
        for (Variation variation : variations) {
            variationKeyMap.put(variation.getKey(), new OptimizelyVariation(
                variation.getId(),
                variation.getKey(),
                isFeatureExperiment ? variation.getFeatureEnabled() : null,
                getMergedVariablesMap(variation, experimentId)
            ));
        }
        return variationKeyMap;
    }

    /**
     * Merges Additional information from variables in feature flag with variation variables as per the following logic.
     * 1. If Variation has variables and feature is enabled, then only `type` and `key` are merged from feature variable.
     * 2. If Variation has variables and feature is disabled, then `type` and `key` are merged and `defaultValue` of feature variable is merged as `value` of variation variable.
     * 3. If Variation does not contain a variable, then all `id`, `key`, `type` and defaultValue as `value` is used from feature varaible and added to variation.
     */
    @VisibleForTesting
    Map<String, OptimizelyVariable> getMergedVariablesMap(Variation variation, String experimentId) {
        String featureKey = this.getExperimentFeatureKey(experimentId);
        if (featureKey != null) {
            // Map containing variables list for every feature key used for merging variation and feature variables.
            Map<String, List<FeatureVariable>> featureKeyToVariablesMap = generateFeatureKeyToVariablesMap();

            // Generate temp map of all the available variable values from variation.
            Map<String, OptimizelyVariable> tempVariableIdMap = getFeatureVariableUsageInstanceMap(variation.getFeatureVariableUsageInstances());

            // Iterate over all the variables available in associated feature.
            // Use value from variation variable if variable is available in variation and feature is enabled, otherwise use defaultValue from feature variable.
            List<FeatureVariable> featureVariables = featureKeyToVariablesMap.get(featureKey);
            if (featureVariables == null) {
                return Collections.emptyMap();
            }

            Map<String, OptimizelyVariable> featureVariableKeyMap = new HashMap<>();
            for (FeatureVariable featureVariable : featureVariables) {
                featureVariableKeyMap.put(featureVariable.getKey(), new OptimizelyVariable(
                    featureVariable.getId(),
                    featureVariable.getKey(),
                    featureVariable.getType().getVariableType().toLowerCase(),
                    variation.getFeatureEnabled() && tempVariableIdMap.get(featureVariable.getId()) != null
                        ? tempVariableIdMap.get(featureVariable.getId()).getValue()
                        : featureVariable.getDefaultValue()
                ));
            }
            return featureVariableKeyMap;
        }
        return Collections.emptyMap();
    }

    @VisibleForTesting
    Map<String, OptimizelyVariable> getFeatureVariableUsageInstanceMap(List<FeatureVariableUsageInstance> featureVariableUsageInstances) {
        if (featureVariableUsageInstances == null) {
            return Collections.emptyMap();
        }

        Map<String, OptimizelyVariable> featureVariableIdMap = new HashMap<>();
        for (FeatureVariableUsageInstance featureVariableUsageInstance : featureVariableUsageInstances) {
            featureVariableIdMap.put(featureVariableUsageInstance.getId(), new OptimizelyVariable(
                featureVariableUsageInstance.getId(),
                null,
                null,
                featureVariableUsageInstance.getValue()
            ));
        }

        return featureVariableIdMap;
    }

    @VisibleForTesting
    Map<String, OptimizelyFeature> getFeaturesMap(Map<String, OptimizelyExperiment> allExperimentsMap) {
        List<FeatureFlag> featureFlags = projectConfig.getFeatureFlags();
        if (featureFlags == null) {
            return Collections.emptyMap();
        }

        Map<String, OptimizelyFeature> optimizelyFeatureKeyMap = new HashMap<>();
        for (FeatureFlag featureFlag : featureFlags) {
            optimizelyFeatureKeyMap.put(featureFlag.getKey(), new OptimizelyFeature(
                featureFlag.getId(),
                featureFlag.getKey(),
                getExperimentsMapForFeature(featureFlag.getExperimentIds(), allExperimentsMap),
                getFeatureVariablesMap(featureFlag.getVariables())
            ));
        }
        return optimizelyFeatureKeyMap;
    }

    @VisibleForTesting
    Map<String, OptimizelyExperiment> getExperimentsMapForFeature(List<String> experimentIds, Map<String, OptimizelyExperiment> allExperimentsMap) {
        if (experimentIds == null) {
            return Collections.emptyMap();
        }

        Map<String, OptimizelyExperiment> optimizelyExperimentKeyMap = new HashMap<>();
        for (String experimentId : experimentIds) {
            String experimentKey = projectConfig.getExperimentIdMapping().get(experimentId).getKey();
            optimizelyExperimentKeyMap.put(experimentKey, allExperimentsMap.get(experimentKey));
        }

        return optimizelyExperimentKeyMap;
    }

    @VisibleForTesting
    Map<String, OptimizelyVariable> getFeatureVariablesMap(List<FeatureVariable> featureVariables) {
        if (featureVariables == null) {
            return Collections.emptyMap();
        }

        Map<String, OptimizelyVariable> featureVariableKeyMap = new HashMap<>();
        for (FeatureVariable featureVariable : featureVariables) {
            featureVariableKeyMap.put(featureVariable.getKey(), new OptimizelyVariable(
                featureVariable.getId(),
                featureVariable.getKey(),
                featureVariable.getType().getVariableType().toLowerCase(),
                featureVariable.getDefaultValue()
            ));
        }

        return featureVariableKeyMap;
    }
}

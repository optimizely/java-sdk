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

import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OptimizelyConfigService {

    private static final Logger logger = LoggerFactory.getLogger(OptimizelyConfigService.class);
    private ProjectConfig projectConfig;
    private OptimizelyConfig optimizelyConfig;

    // Map containing variables list for every feature key used for merging variation and feature variables.
    private Map<String, List<FeatureVariable>> featureKeyToVariablesMap;

    public OptimizelyConfigService(ProjectConfig projectConfig) {
        this.projectConfig = projectConfig;
        this.featureKeyToVariablesMap = generateFeatureKeyToVariablesMap();

        logger.info("Getting experiment map for project config");
        Map<String, OptimizelyExperiment> experimentsMap = getExperimentsMap();
        logger.info("Creating the complete optimizely config");
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
            logger.warn("Returning empty map because there are no feature flags in project config");
            return Collections.emptyMap();
        }
        return featureFlags.stream().collect(Collectors.toMap(FeatureFlag::getKey, featureFlag -> featureFlag.getVariables()));
    }

    @VisibleForTesting
    String getExperimentFeatureKey(String experimentId) {
        List<String> featureKeys = projectConfig.getExperimentFeatureKeyMapping().get(experimentId);
        logger.info("Keys for feature experiment is null {}", featureKeys == null);
        return featureKeys != null ? featureKeys.get(0) : null;
    }

    @VisibleForTesting
    Map<String, OptimizelyExperiment> getExperimentsMap() {
        List<Experiment> experiments = projectConfig.getExperiments();
        if(experiments == null) {
            logger.warn("Returning empty map because there are no experiments in project config");
            return Collections.emptyMap();
        }
        return experiments.stream().collect(Collectors.toMap(Experiment::getKey, experiment -> new OptimizelyExperiment(
            experiment.getId(),
            experiment.getKey(),
            getVariationsMap(experiment.getVariations(), experiment.getId())
        )));
    }

    @VisibleForTesting
    Map<String, OptimizelyVariation> getVariationsMap(List<Variation> variations, String experimentId) {
        if(variations == null) {
            logger.warn("Returning empty map because there variations provided are null");
            return Collections.emptyMap();
        }
        Boolean isFeatureExperiment = this.getExperimentFeatureKey(experimentId) != null;
        logger.debug("Experiment id {} is a feature experiment: {}", experimentId, isFeatureExperiment);
        return variations.stream().collect(Collectors.toMap(Variation::getKey, variation -> new OptimizelyVariation(
            variation.getId(),
            variation.getKey(),
            isFeatureExperiment ? variation.getFeatureEnabled() : null,
            getMergedVariablesMap(variation, experimentId)
        )));
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
            logger.info("Merging the variables as feature key is available");
            // Generate temp map of all the available variable values from variation.
            Map<String, OptimizelyVariable> tempVariableIdMap = getFeatureVariableUsageInstanceMap(variation.getFeatureVariableUsageInstances());

            // Iterate over all the variables available in associated feature.
            // Use value from variation variable if variable is available in variation and feature is enabled, otherwise use defaultValue from feature variable.
            List<FeatureVariable> featureVariables = featureKeyToVariablesMap.get(featureKey);
            if (featureVariables == null) {
                logger.warn("Returning empty map as featureKeyToVariablesMap is null");
                return Collections.emptyMap();
            }

            return featureVariables.stream().collect(Collectors.toMap(FeatureVariable::getKey, featureVariable -> new OptimizelyVariable(
                featureVariable.getId(),
                featureVariable.getKey(),
                featureVariable.getType().getVariableType().toLowerCase(),
                variation.getFeatureEnabled() && tempVariableIdMap.get(featureVariable.getId()) != null
                    ? tempVariableIdMap.get(featureVariable.getId()).getValue()
                    : featureVariable.getDefaultValue()
            )));
        }
        return Collections.emptyMap();
    }

    @VisibleForTesting
    Map<String, OptimizelyVariable> getFeatureVariableUsageInstanceMap(List<FeatureVariableUsageInstance> featureVariableUsageInstances) {
        if(featureVariableUsageInstances == null) {
            logger.warn("Returning empty map because there FeatureVariableUsageInstance provided are null");
            return Collections.emptyMap();
        }
        return featureVariableUsageInstances.stream().collect(Collectors.toMap(FeatureVariableUsageInstance::getId, variable -> new OptimizelyVariable(
            variable.getId(),
            null,
            null,
            variable.getValue()
        )));
    }

    @VisibleForTesting
    Map<String, OptimizelyFeature> getFeaturesMap(Map<String, OptimizelyExperiment> allExperimentsMap) {
        List<FeatureFlag> featureFlags = projectConfig.getFeatureFlags();
        if(featureFlags == null) {
            logger.warn("Returning empty map because there are no feature flags in project config");
            return Collections.emptyMap();
        }
        return featureFlags.stream().collect(Collectors.toMap(FeatureFlag::getKey, featureFlag -> new OptimizelyFeature(
            featureFlag.getId(),
            featureFlag.getKey(),
            getExperimentsMapForFeature(featureFlag.getExperimentIds(), allExperimentsMap),
            getFeatureVariablesMap(featureFlag.getVariables())
        )));
    }

    @VisibleForTesting
    Map<String, OptimizelyExperiment> getExperimentsMapForFeature(List<String> experimentIds, Map<String, OptimizelyExperiment> allExperimentsMap) {
        if (experimentIds == null) {
            logger.warn("Returning empty map because the experiment id list is null");
            return Collections.emptyMap();
        }

        List<String> experimentKeys = experimentIds.stream()
            .map(experimentId -> projectConfig.getExperimentIdMapping().get(experimentId).getKey())
            .collect(Collectors.toList());

        return experimentKeys.stream().collect(Collectors.toMap(Function.identity(), key -> allExperimentsMap.get(key)));
    }

    @VisibleForTesting
    Map<String, OptimizelyVariable> getFeatureVariablesMap(List<FeatureVariable> featureVariables) {
        if (featureVariables == null) {
            logger.warn("Returning empty map because the feature variables list is null");
            return Collections.emptyMap();
        }
        return featureVariables.stream().collect(Collectors.toMap(FeatureVariable::getKey, featureVariable -> new OptimizelyVariable(
            featureVariable.getId(),
            featureVariable.getKey(),
            featureVariable.getType().getVariableType().toLowerCase(),
            featureVariable.getDefaultValue()
        )));
    }
}

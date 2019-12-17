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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OptimizelyConfigServiceTest {

    private ProjectConfig projectConfig;
    @Mock
    private OptimizelyConfigService optimizelyConfigService;

    @Before
    public void initialize() throws Exception {
        projectConfig = new DatafileProjectConfig.Builder().withDatafile(validConfigJsonV4()).build();
        optimizelyConfigService = OptimizelyConfigService.getInstance(projectConfig);
    }

    @Test
    public void testGetMergedVariablesMap() {
        List<Experiment> experiments = projectConfig.getExperiments();
        experiments.forEach(experiment ->
            experiment.getVariations().forEach(variation -> {
                // creating temporary variable map to get values while merging
                Map<String, OptimizelyVariable> tempVariableIdMap =
                    optimizelyConfigService.getFeatureVariableUsageInstanceMap(variation.getFeatureVariableUsageInstances());
                String featureKey = optimizelyConfigService.getExperimentFeatureKey(experiment.getId());
                if (featureKey != null) {
                    // keeping all the feature variables for asserting
                    List<FeatureVariable> featureVariables =
                        optimizelyConfigService.generateFeatureKeyToVariablesMap().get(featureKey);
                    Map<String, OptimizelyVariable> optimizelyVariableMap =
                        optimizelyConfigService.getMergedVariablesMap(variation, experiment.getId());
                    featureVariables.forEach(featureVariable -> {
                        OptimizelyVariable optimizelyVariable = optimizelyVariableMap.get(featureVariable.getKey());
                        assertEquals(optimizelyVariable.getKey(), featureVariable.getKey());
                        assertEquals(optimizelyVariable.getId(), featureVariable.getId());
                        assertEquals(optimizelyVariable.getType(), featureVariable.getType().getVariableType().toLowerCase());
                        // getting the expected value to assert after merging
                        String expectedValue = variation.getFeatureEnabled() && tempVariableIdMap.get(featureVariable.getId()) != null
                            ? tempVariableIdMap.get(featureVariable.getId()).getValue()
                            : featureVariable.getDefaultValue();
                        assertEquals(optimizelyVariable.getValue(), expectedValue);
                    });
                }
            })
        );
    }

    @Test
    public void testGetExperimentsMap() {
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = optimizelyConfigService.getExperimentsMap();
        assertEquals(optimizelyExperimentMap.size(), 13);

        List<Experiment> experiments = getAllExperimentsFromDatafile();
        experiments.forEach(experiment -> {
            OptimizelyExperiment optimizelyExperiment = optimizelyExperimentMap.get(experiment.getKey());
            assertEquals(optimizelyExperiment.getId(), experiment.getId());
            assertEquals(optimizelyExperiment.getKey(), experiment.getKey());

            Map<String, OptimizelyVariation> optimizelyVariationMap = optimizelyExperimentMap.get(experiment.getKey()).getVariationsMap();
            experiment.getVariations().forEach(variation -> {
                OptimizelyVariation optimizelyVariation = optimizelyVariationMap.get(variation.getKey());
                assertEquals(optimizelyVariation.getId(), variation.getId());
                assertEquals(optimizelyVariation.getKey(), variation.getKey());
            });
        });
    }

    @Test
    public void testRevision() {
        String revision = optimizelyConfigService.getOptimizelyConfig().getRevision();
        assertEquals(revision, projectConfig.getRevision());
    }

    @Test
    public void testGetFeaturesMap() {
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = optimizelyConfigService.getExperimentsMap();
        Map<String, OptimizelyFeature> optimizelyFeatureMap = optimizelyConfigService.getFeaturesMap(optimizelyExperimentMap);
        assertEquals(optimizelyFeatureMap.size(), 7);

        projectConfig.getFeatureFlags().forEach(featureFlag -> {
            List<String> experimentIds = featureFlag.getExperimentIds();
            experimentIds.forEach(experimentId -> {
                String experimentKey =  projectConfig.getExperimentIdMapping().get(experimentId).getKey();
                OptimizelyExperiment optimizelyExperiment
                    = optimizelyFeatureMap.get(featureFlag.getKey()).getExperimentsMap().get(experimentKey);
                assertNotNull(optimizelyExperiment);
            });

            OptimizelyFeature optimizelyFeature = optimizelyFeatureMap.get(featureFlag.getKey());
            assertEquals(optimizelyFeature.getId(), featureFlag.getId());
            assertEquals(optimizelyFeature.getKey(), featureFlag.getKey());

            Map<String, OptimizelyVariable> optimizelyVariableMap = optimizelyFeatureMap.get(featureFlag.getKey()).getVariablesMap();
            featureFlag.getVariables().forEach(variable -> {
                OptimizelyVariable optimizelyVariable = optimizelyVariableMap.get(variable.getKey());
                assertEquals(optimizelyVariable.getId(), variable.getId());
                assertEquals(optimizelyVariable.getKey(), variable.getKey());
                assertEquals(optimizelyVariable.getType(), variable.getType().getVariableType().toLowerCase());
                assertEquals(optimizelyVariable.getValue(), variable.getDefaultValue());
            });
        });
    }

    @Test
    public void testGetFeatureVariablesMap() {
        List<FeatureFlag> featureFlags = projectConfig.getFeatureFlags();
        featureFlags.forEach(featureFlag -> {
            Map<String, OptimizelyVariable> optimizelyVariableMap =
                optimizelyConfigService.getFeatureVariablesMap(featureFlag.getVariables());
            featureFlag.getVariables().forEach(variable -> {
                OptimizelyVariable optimizelyVariable = optimizelyVariableMap.get(variable.getKey());
                assertEquals(optimizelyVariable.getValue(), variable.getDefaultValue());
                assertEquals(optimizelyVariable.getId(), variable.getId());
                assertEquals(optimizelyVariable.getType(), variable.getType().getVariableType().toLowerCase());
            });
        });
    }

    @Test
    public void testGetExperimentsMapForFeature() {
        projectConfig.getFeatureFlags().forEach(featureFlag -> {
            List<String> experimentIds = featureFlag.getExperimentIds();
            Map<String, OptimizelyExperiment> optimizelyFeatureExperimentMap =
                optimizelyConfigService.getExperimentsMapForFeature(experimentIds, optimizelyConfigService.getExperimentsMap());
            assertEquals(optimizelyFeatureExperimentMap.size(), experimentIds.size());
            optimizelyFeatureExperimentMap.forEach((experimentKey, experiment) ->
                assertTrue(experimentIds.contains(experiment.getId()))
            );
        });
    }

    @Test
    public void testGetFeatureVariableUsageInstanceMap() {
        List<FeatureVariableUsageInstance> featureVariableUsageInstances =
            projectConfig.getExperiments().get(0).getVariations().get(0).getFeatureVariableUsageInstances();
        Map<String, OptimizelyVariable> optimizelyVariableMap =
            optimizelyConfigService.getFeatureVariableUsageInstanceMap(featureVariableUsageInstances);
        featureVariableUsageInstances.forEach(featureVariableUsageInstance -> {
            OptimizelyVariable optimizelyVariable = optimizelyVariableMap.get(featureVariableUsageInstance.getId());
            assertEquals(optimizelyVariable.getValue(), featureVariableUsageInstance.getValue());
            assertEquals(optimizelyVariable.getId(), featureVariableUsageInstance.getId());
        });
    }

    @Test
    public void testGetVariationsMap() {
        Experiment experiment = projectConfig.getExperiments().get(0);
        List<Variation> variations = experiment.getVariations();
        Map<String, OptimizelyVariation> optimizelyVariationMap =
            optimizelyConfigService.getVariationsMap(variations, experiment.getId());
        variations.forEach(variation -> {
            OptimizelyVariation optimizelyVariation = optimizelyVariationMap.get(variation.getKey());
            assertEquals(optimizelyVariation.getId(), variation.getId());
            assertEquals(optimizelyVariation.getKey(), variation.getKey());
            Map<String, OptimizelyVariable> optimizelyVariableMap =
                optimizelyConfigService.getMergedVariablesMap(variation, experiment.getId());
            optimizelyVariableMap.forEach((variableKey, variable) -> {
                OptimizelyVariable optimizelyVariable = optimizelyVariableMap.get(variableKey);
                assertEquals(variable.getValue(), optimizelyVariable.getValue());
                assertEquals(variable.getType(), optimizelyVariable.getType());
                assertEquals(variable.getId(), optimizelyVariable.getId());
                assertEquals(variable.getKey(), optimizelyVariable.getKey());
            });
        });
    }

    @Test
    public void testGetExperimentFeatureKey() {
        List<Experiment> experiments = projectConfig.getExperiments();
        experiments.forEach(experiment -> {
            String featureKey = optimizelyConfigService.getExperimentFeatureKey(experiment.getId());
            List<String> featureKeys = projectConfig.getExperimentFeatureKeyMapping().get(experiment.getId());
            if(featureKeys != null) {
                assertTrue(featureKeys.contains(featureKey));
            }
        });
    }

    @Test
    public void testGenerateFeatureKeyToVariablesMap() {
        Map<String, List<FeatureVariable>> featureKeyToVariableMap = optimizelyConfigService.generateFeatureKeyToVariablesMap();
        projectConfig.getFeatureFlags().forEach(featureFlag -> {
            List<FeatureVariable> featureVariables = featureKeyToVariableMap.get(featureFlag.getKey());
            assertEquals(featureVariables.size(), featureFlag.getVariables().size());
            featureVariables.forEach(featureVariable ->
                featureFlag.getVariables().forEach(variable -> {
                    if (variable.getKey().equals(featureVariable.getKey())) {
                        assertEquals(variable.getDefaultValue(), featureVariable.getDefaultValue());
                        assertEquals(variable.getType().getVariableType().toLowerCase(), featureVariable.getType().getVariableType().toLowerCase());
                        assertEquals(variable.getId(), featureVariable.getId());
                    }
                })
            );
        });
    }

    @Test
    public void testCachingOfOptimizelyConfig() throws Exception {
        OptimizelyConfigService mocked = mock(OptimizelyConfigService.class);
        when(mocked.getExperimentsMap()).thenReturn(optimizelyConfigService.getExperimentsMap());
        optimizelyConfigService = OptimizelyConfigService.getInstance(projectConfig);
        verify(mocked, times(0)).getExperimentsMap();
    }

    private List<Experiment> getAllExperimentsFromDatafile() {
        List<Experiment> experiments = new ArrayList<>();
        projectConfig.getGroups().forEach(group ->
            experiments.addAll(group.getExperiments())
        );
        experiments.addAll(projectConfig.getExperiments());
        return experiments;
    }
}

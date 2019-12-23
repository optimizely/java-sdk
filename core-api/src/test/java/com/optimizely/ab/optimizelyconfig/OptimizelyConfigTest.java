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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import static com.optimizely.ab.optimizelyconfig.OptimizelyExperimentTest.generateVariationMap;
import static com.optimizely.ab.optimizelyconfig.OptimizelyVariationTest.generateVariablesMap;
import static org.junit.Assert.assertEquals;

public class OptimizelyConfigTest {

    @Test
    public void testOptimizelyConfig() {
        OptimizelyConfig optimizelyConfig = new OptimizelyConfig(
            generateExperimntMap(),
            generateFeatureMap(),
            "101"
        );
        assertEquals("101", optimizelyConfig.getRevision());
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = generateExperimntMap();
        assertEquals(optimizelyExperimentMap.size(), optimizelyConfig.getExperimentsMap().size());
        // verify the experiments map
        optimizelyExperimentMap.forEach((experimentKey, experiment) -> {
           OptimizelyExperiment optimizelyExperiment = optimizelyConfig.getExperimentsMap().get(experimentKey);
           assertEquals(experiment.getId(), optimizelyExperiment.getId());
           assertEquals(experiment.getKey(), optimizelyExperiment.getKey());
           assertEquals(experiment.getVariationsMap().size(), optimizelyExperiment.getVariationsMap().size());
           // verify the variations map
           experiment.getVariationsMap().forEach((variationKey, variation) -> {
               OptimizelyVariation optimizelyVariation = optimizelyExperiment.getVariationsMap().get(variationKey);
               assertEquals(variation.getId(), optimizelyVariation.getId());
               assertEquals(variation.getKey(), optimizelyVariation.getKey());
               assertEquals(variation.getFeatureEnabled(), optimizelyVariation.getFeatureEnabled());
               assertEquals(variation.getVariablesMap().size(), optimizelyVariation.getVariablesMap().size());
               // verify the variables map
               variation.getVariablesMap().forEach((variableKey, variable) -> {
                   OptimizelyVariable optimizelyVariable = optimizelyVariation.getVariablesMap().get(variableKey);
                   assertEquals(variable.getId(), optimizelyVariable.getId());
                   assertEquals(variable.getKey(), optimizelyVariable.getKey());
                   assertEquals(variable.getType(), optimizelyVariable.getType());
                   assertEquals(variable.getValue(), optimizelyVariable.getValue());
               });
           });
        });

        Map<String, OptimizelyFeature> optimizelyFeatureMap = generateFeatureMap();
        // verifying feature map
        assertEquals(optimizelyFeatureMap.size(), optimizelyConfig.getFeaturesMap().size());
        optimizelyFeatureMap.forEach((featureKey, feature) -> {
            OptimizelyFeature optimizelyFeature = optimizelyConfig.getFeaturesMap().get(featureKey);
            assertEquals(feature.getId(), optimizelyFeature.getId());
            assertEquals(feature.getKey(), optimizelyFeature.getKey());
            // verifying variables map
            assertEquals(feature.getVariablesMap().size(), optimizelyFeature.getVariablesMap().size());
            feature.getVariablesMap().forEach((variableKey, variable) -> {
                OptimizelyVariable optimizelyVariable = optimizelyFeature.getVariablesMap().get(variableKey);
                assertEquals(variable.getId(), optimizelyVariable.getId());
                assertEquals(variable.getKey(), optimizelyVariable.getKey());
                assertEquals(variable.getType(), optimizelyVariable.getType());
                assertEquals(variable.getValue(), optimizelyVariable.getValue());
            });
            // verifying experiment map
            assertEquals(feature.getExperimentsMap().size(), optimizelyFeature.getExperimentsMap().size());
            feature.getExperimentsMap().forEach((experimentKey, experiment) -> {
                OptimizelyExperiment optimizelyExperiment = optimizelyFeature.getExperimentsMap().get(experimentKey);
                assertEquals(experiment.getId(), optimizelyExperiment.getId());
                assertEquals(experiment.getKey(), optimizelyExperiment.getKey());
                // verifying variation map
                experiment.getVariationsMap().forEach((variationKey, variation) -> {
                    OptimizelyVariation optimizelyVariation = optimizelyExperiment.getVariationsMap().get(variationKey);
                    assertEquals(variation.getId(), optimizelyVariation.getId());
                    assertEquals(variation.getKey(), optimizelyVariation.getKey());
                    assertEquals(variation.getFeatureEnabled(), optimizelyVariation.getFeatureEnabled());
                    // verifying variables map
                    variation.getVariablesMap().forEach((variableKey, variable) -> {
                        OptimizelyVariable optimizelyVariable = optimizelyVariation.getVariablesMap().get(variableKey);
                        assertEquals(variable.getId(), optimizelyVariable.getId());
                        assertEquals(variable.getKey(), optimizelyVariable.getKey());
                        assertEquals(variable.getType(), optimizelyVariable.getType());
                        assertEquals(variable.getType(), optimizelyVariable.getType());
                    });
                });
            });
        });
    }

    private Map<String, OptimizelyExperiment> generateExperimntMap() {
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = new HashMap<>();
        optimizelyExperimentMap.put("test_exp_1", new OptimizelyExperiment(
            "33",
            "test_exp_1",
            generateVariationMap()
        ));
        optimizelyExperimentMap.put("test_exp_2", new OptimizelyExperiment(
            "34",
            "test_exp_2",
            generateVariationMap()
        ));
        return optimizelyExperimentMap;
    }

    private Map<String, OptimizelyFeature> generateFeatureMap() {
        Map<String, OptimizelyFeature> optimizelyFeatureMap = new HashMap<>();
        optimizelyFeatureMap.put("test_feature_1", new OptimizelyFeature(
           "42",
           "test_feature_1",
            generateExperimntMap(),
            generateVariablesMap()
        ));
        return  optimizelyFeatureMap;
    }
}

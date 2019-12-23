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
import static com.optimizely.ab.optimizelyconfig.OptimizelyVariationTest.generateVariablesMap;
import static com.optimizely.ab.optimizelyconfig.OptimizelyExperimentTest.generateVariationMap;
import static org.junit.Assert.assertEquals;

public class OptimizelyFeatureTest {

    @Test
    public void testOptimizelyFeature() {
        OptimizelyFeature optimizelyFeature = new OptimizelyFeature(
            "41",
            "test_feature",
            generateExperimentMap(),
            generateVariablesMap()
        );
        // verifying feature
        assertEquals("41", optimizelyFeature.getId());
        assertEquals("test_feature", optimizelyFeature.getKey());
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = generateExperimentMap();
        assertEquals(optimizelyExperimentMap.size(), optimizelyFeature.getExperimentsMap().size());
        // verifying experiment map
        optimizelyExperimentMap.forEach((experimentKey, experiment) -> {
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

        Map<String, OptimizelyVariable> optimizelyVariableMap = generateVariablesMap();
        assertEquals(optimizelyVariableMap.size(), optimizelyFeature.getVariablesMap().size());
        optimizelyVariableMap.forEach((variableKey, variable) -> {
            OptimizelyVariable optimizelyVariable = optimizelyFeature.getVariablesMap().get(variableKey);
            assertEquals(variable.getId(), optimizelyVariable.getId());
            assertEquals(variable.getKey(), optimizelyVariable.getKey());
            assertEquals(variable.getType(), optimizelyVariable.getType());
            assertEquals(variable.getType(), optimizelyVariable.getType());
        });

    }

    static Map<String, OptimizelyExperiment> generateExperimentMap() {
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = new HashMap<>();
        optimizelyExperimentMap.put("test_exp_1", new OptimizelyExperiment (
            "32",
            "test_exp_1",
            generateVariationMap()
        ));
        optimizelyExperimentMap.put("test_exp_2", new OptimizelyExperiment (
            "33",
            "test_exp_2",
            generateVariationMap()
        ));
        return optimizelyExperimentMap;
    }
}

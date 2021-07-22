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

import org.junit.Test;

import java.util.Collections;
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
            generateVariablesMap(),
            Collections.emptyList(),
            Collections.emptyList()
        );
        assertEquals("41", optimizelyFeature.getId());
        assertEquals("test_feature", optimizelyFeature.getKey());
        // verifying experiments map
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = generateExperimentMap();
        assertEquals(optimizelyExperimentMap.size(), optimizelyFeature.getExperimentsMap().size());
        assertEquals(optimizelyExperimentMap, optimizelyFeature.getExperimentsMap());
        // verifying variables map
        Map<String, OptimizelyVariable> optimizelyVariableMap = generateVariablesMap();
        assertEquals(optimizelyVariableMap.size(), optimizelyFeature.getVariablesMap().size());
        assertEquals(optimizelyVariableMap, optimizelyFeature.getVariablesMap());
    }

    static Map<String, OptimizelyExperiment> generateExperimentMap() {
        Map<String, OptimizelyExperiment> optimizelyExperimentMap = new HashMap<>();
        optimizelyExperimentMap.put("test_exp_1", new OptimizelyExperiment (
            "32",
            "test_exp_1",
            generateVariationMap(),
            ""
        ));
        optimizelyExperimentMap.put("test_exp_2", new OptimizelyExperiment (
            "33",
            "test_exp_2",
            generateVariationMap(),
            ""
        ));
        return optimizelyExperimentMap;
    }
}

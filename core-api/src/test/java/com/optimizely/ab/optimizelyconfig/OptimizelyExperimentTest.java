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

import java.util.HashMap;
import java.util.Map;
import static com.optimizely.ab.optimizelyconfig.OptimizelyVariationTest.generateVariablesMap;
import static org.junit.Assert.assertEquals;

public class OptimizelyExperimentTest {

    @Test
    public void testOptimizelyExperiment() {
        OptimizelyExperiment optimizelyExperiment = new OptimizelyExperiment(
          "31",
          "test_exp",
           generateVariationMap(),
            ""
        );
        assertEquals("31", optimizelyExperiment.getId());
        assertEquals("test_exp", optimizelyExperiment.getKey());
        Map<String, OptimizelyVariation> optimizelyVariationMap = generateVariationMap();
        assertEquals(optimizelyVariationMap.size(), optimizelyExperiment.getVariationsMap().size());
        // verifying the variations
        assertEquals(optimizelyVariationMap, optimizelyExperiment.getVariationsMap());
    }

    static Map<String, OptimizelyVariation> generateVariationMap() {
        // now creating map of variations
        Map<String, OptimizelyVariation> optimizelyVariationMap = new HashMap<>();
        optimizelyVariationMap.put("test_var_key_1", new OptimizelyVariation(
            "13",
            "test_var_key_1",
            true,
            generateVariablesMap()
        ));
        optimizelyVariationMap.put("test_var_key_2", new OptimizelyVariation(
            "14",
            "test_var_key_2",
            false,
            generateVariablesMap()
        ));
        return optimizelyVariationMap;
    }
}

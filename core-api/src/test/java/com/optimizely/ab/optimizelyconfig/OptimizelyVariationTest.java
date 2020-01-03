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
import static org.junit.Assert.assertEquals;

public class OptimizelyVariationTest {

    @Test
    public void testOptimizelyVariation() {
        OptimizelyVariation optimizelyVariation = new OptimizelyVariation(
            "12",
            "test_var_key",
            false,
            generateVariablesMap()
        );
        assertEquals("12", optimizelyVariation.getId());
        assertEquals("test_var_key", optimizelyVariation.getKey());
        assertEquals(false, optimizelyVariation.getFeatureEnabled());

        Map<String, OptimizelyVariable> expectedoptimizelyVariableMap = generateVariablesMap();
        assertEquals(expectedoptimizelyVariableMap.size(), optimizelyVariation.getVariablesMap().size());
        assertEquals(expectedoptimizelyVariableMap, optimizelyVariation.getVariablesMap());
    }

    static Map<String, OptimizelyVariable> generateVariablesMap() {
        Map<String, OptimizelyVariable> optimizelyVariableMap = new HashMap<>();
        optimizelyVariableMap.put("test_variable_key_1", new OptimizelyVariable(
            "7",
            "test_variable_key_1",
            "integer",
            "10"
        ));
        optimizelyVariableMap.put("test_variable_key_2", new OptimizelyVariable(
            "8",
            "test_variable_key_2",
            "boolean",
            "true"
        ));
        return optimizelyVariableMap;
    }
}

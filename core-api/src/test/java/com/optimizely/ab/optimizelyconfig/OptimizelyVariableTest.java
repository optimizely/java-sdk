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

import static org.junit.Assert.assertEquals;

public class OptimizelyVariableTest {

    @Test
    public void testOptimizelyVariable() {
        OptimizelyVariable optimizelyVariable = new OptimizelyVariable(
            "7",
            "test_variable_key",
            "integer",
            "10"
            );
        assertEquals("7", optimizelyVariable.getId());
        assertEquals("test_variable_key", optimizelyVariable.getKey());
        assertEquals("integer", optimizelyVariable.getType());
        assertEquals("10", optimizelyVariable.getValue());
    }
}

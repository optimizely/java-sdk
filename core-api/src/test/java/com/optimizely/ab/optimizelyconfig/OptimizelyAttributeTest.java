/****************************************************************************
 * Copyright 2021, Optimizely, Inc. and contributors                        *
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

public class OptimizelyAttributeTest {

    @Test
    public void testOptimizelyAttribute() {
        OptimizelyAttribute optimizelyAttribute1 = new OptimizelyAttribute(
            "5",
            "test_attribute"
        );
        OptimizelyAttribute optimizelyAttribute2 = new OptimizelyAttribute(
            "5",
            "test_attribute"
        );
        assertEquals("5", optimizelyAttribute1.getId());
        assertEquals("test_attribute", optimizelyAttribute1.getKey());
        assertEquals(optimizelyAttribute1, optimizelyAttribute2);
        assertEquals(optimizelyAttribute1.hashCode(), optimizelyAttribute2.hashCode());
    }
}

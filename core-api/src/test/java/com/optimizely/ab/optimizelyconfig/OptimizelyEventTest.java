/****************************************************************************
 * Copyright 2020-2021, Optimizely, Inc. and contributors                   *
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
import static java.util.Arrays.asList;

public class OptimizelyEventTest {
    @Test
    public void testOptimizelyEvent() {
        OptimizelyEvent optimizelyEvent1 = new OptimizelyEvent(
            "5",
            "test_event",
            asList("123","234","345")
        );
        OptimizelyEvent optimizelyEvent2 = new OptimizelyEvent(
            "5",
            "test_event",
            asList("123","234","345")
        );
        assertEquals("5", optimizelyEvent1.getId());
        assertEquals("test_event", optimizelyEvent1.getKey());
        assertEquals(optimizelyEvent1, optimizelyEvent2);
        assertEquals(optimizelyEvent1.hashCode(), optimizelyEvent2.hashCode());
    }
}

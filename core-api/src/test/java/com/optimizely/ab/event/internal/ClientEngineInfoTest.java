/**
 *
 *    Copyright 2019, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.event.internal;

import com.optimizely.ab.event.internal.payload.EventBatch;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClientEngineInfoTest {

    @After
    public void tearDown() throws Exception {
        ClientEngineInfo.setClientEngine(ClientEngineInfo.DEFAULT);
    }

    @Test
    public void testSetAndGetClientEngine() {
        for (EventBatch.ClientEngine expected: EventBatch.ClientEngine.values()) {
            ClientEngineInfo.setClientEngine(expected);
            assertEquals(expected, ClientEngineInfo.getClientEngine());
        }
    }

    @Test
    public void testDefaultValue() {
        assertEquals(ClientEngineInfo.DEFAULT, ClientEngineInfo.getClientEngine());
    }
}

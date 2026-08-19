/**
 *
 *    Copyright 2026, Optimizely and contributors
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
package com.optimizely.ab.event.internal.serializer;

import org.junit.Test;
import static org.junit.Assert.*;

public class DefaultJsonSerializerTest {

    @Test
    public void getInstanceReturnsNonNull() {
        Serializer serializer = DefaultJsonSerializer.getInstance();
        assertNotNull("getInstance() should return a non-null serializer", serializer);
    }

    @Test
    public void getInstanceReturnsSameInstance() {
        Serializer first = DefaultJsonSerializer.getInstance();
        Serializer second = DefaultJsonSerializer.getInstance();
        assertSame("getInstance() should return the same singleton instance", first, second);
    }

    @Test
    public void instanceCanSerialize() {
        Serializer serializer = DefaultJsonSerializer.getInstance();
        String result = serializer.serialize(java.util.Collections.singletonMap("test_key", "test_value"));
        assertNotNull("Serializer should produce non-null output", result);
        assertTrue("Serialized output should contain the key", result.contains("test_key"));
        assertTrue("Serialized output should contain the value", result.contains("test_value"));
    }
}

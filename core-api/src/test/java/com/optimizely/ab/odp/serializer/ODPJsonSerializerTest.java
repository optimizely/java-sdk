/**
 *    Copyright 2022, Optimizely Inc. and contributors
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
package com.optimizely.ab.odp.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.odp.ODPEvent;
import com.optimizely.ab.odp.serializer.impl.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@RunWith(Parameterized.class)
public class ODPJsonSerializerTest {
    private final ODPJsonSerializer jsonSerializer;

    public ODPJsonSerializerTest(ODPJsonSerializer jsonSerializer) {
        super();
        this.jsonSerializer = jsonSerializer;
    }

    @Parameterized.Parameters
    public static List<ODPJsonSerializer> input() {
        return Arrays.asList(new GsonSerializer(), new JsonSerializer(), new JsonSimpleSerializer(), new JacksonSerializer());
    }

    @Test
    public void serializeValidEvents() throws JsonProcessingException {
        List<ODPEvent> events = Arrays.asList(
            createTestEvent("1"),
            createTestEvent("2"),
            createTestEvent("3")
        );

        ObjectMapper mapper = new ObjectMapper();

        String expectedResult = "[{\"type\":\"type-1\",\"action\":\"action-1\",\"identifiers\":{\"vuid-1-3\":\"fs-1-3\",\"vuid-1-1\":\"fs-1-1\",\"vuid-1-2\":\"fs-1-2\"},\"data\":{\"source\":\"java-sdk\",\"data-1\":\"data-value-1\"}},{\"type\":\"type-2\",\"action\":\"action-2\",\"identifiers\":{\"vuid-2-3\":\"fs-2-3\",\"vuid-2-2\":\"fs-2-2\",\"vuid-2-1\":\"fs-2-1\"},\"data\":{\"source\":\"java-sdk\",\"data-1\":\"data-value-2\"}},{\"type\":\"type-3\",\"action\":\"action-3\",\"identifiers\":{\"vuid-3-3\":\"fs-3-3\",\"vuid-3-2\":\"fs-3-2\",\"vuid-3-1\":\"fs-3-1\"},\"data\":{\"source\":\"java-sdk\",\"data-1\":\"data-value-3\"}}]";
        String serializedString = jsonSerializer.serializeEvents(events);
        System.out.println(serializedString);
        assertEquals(mapper.readTree(expectedResult), mapper.readTree(serializedString));
    }

    private static ODPEvent createTestEvent(String index) {
        Map<String, String> identifiers = new HashMap<>();
        identifiers.put("vuid-" + index + "-1", "fs-" + index + "-1");
        identifiers.put("vuid-" + index + "-2", "fs-" + index + "-2");
        identifiers.put("vuid-" + index + "-3", "fs-" + index + "-3");

        Map<String, String> data = new HashMap<>();
        data.put("source", "java-sdk");
        data.put("data-1", "data-value-" + index);

        return new ODPEvent("type-" + index, "action-" + index, identifiers, data);
    }
}

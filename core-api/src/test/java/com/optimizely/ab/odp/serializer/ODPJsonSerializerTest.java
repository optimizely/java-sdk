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
    public void serializeMultipleEvents() throws JsonProcessingException {
        List<ODPEvent> events = Arrays.asList(
            createTestEvent("1"),
            createTestEvent("2"),
            createTestEvent("3")
        );

        ObjectMapper mapper = new ObjectMapper();

        String expectedResult = "[{\"type\":\"type-1\",\"action\":\"action-1\",\"identifiers\":{\"vuid-1-3\":\"fs-1-3\",\"vuid-1-1\":\"fs-1-1\",\"vuid-1-2\":\"fs-1-2\"},\"data\":{\"source\":\"java-sdk\",\"data-1\":\"data-value-1\",\"data-num\":1,\"data-bool-true\":true,\"data-bool-false\":false,\"data-float\":2.33,\"data-null\":null}},{\"type\":\"type-2\",\"action\":\"action-2\",\"identifiers\":{\"vuid-2-3\":\"fs-2-3\",\"vuid-2-2\":\"fs-2-2\",\"vuid-2-1\":\"fs-2-1\"},\"data\":{\"source\":\"java-sdk\",\"data-1\":\"data-value-2\",\"data-num\":2,\"data-bool-true\":true,\"data-bool-false\":false,\"data-float\":2.33,\"data-null\":null}},{\"type\":\"type-3\",\"action\":\"action-3\",\"identifiers\":{\"vuid-3-3\":\"fs-3-3\",\"vuid-3-2\":\"fs-3-2\",\"vuid-3-1\":\"fs-3-1\"},\"data\":{\"source\":\"java-sdk\",\"data-1\":\"data-value-3\",\"data-num\":3,\"data-bool-true\":true,\"data-bool-false\":false,\"data-float\":2.33,\"data-null\":null}}]";
        String serializedString = jsonSerializer.serializeEvents(events);
        assertEquals(mapper.readTree(expectedResult), mapper.readTree(serializedString));
    }

    @Test
    public void serializeEmptyList() throws JsonProcessingException {
        List<ODPEvent> events = Collections.emptyList();
        String expectedResult = "[]";
        String serializedString = jsonSerializer.serializeEvents(events);
        assertEquals(expectedResult, serializedString);
    }

    private static ODPEvent createTestEvent(String index) {
        Map<String, String> identifiers = new HashMap<>();
        identifiers.put("vuid-" + index + "-1", "fs-" + index + "-1");
        identifiers.put("vuid-" + index + "-2", "fs-" + index + "-2");
        identifiers.put("vuid-" + index + "-3", "fs-" + index + "-3");

        Map<String, Object> data = new HashMap<>();
        data.put("source", "java-sdk");
        data.put("data-1", "data-value-" + index);
        data.put("data-num", Integer.parseInt(index));
        data.put("data-float", 2.33);
        data.put("data-bool-true", true);
        data.put("data-bool-false", false);
        data.put("data-null", null);


        return new ODPEvent("type-" + index, "action-" + index, identifiers, data);
    }
}

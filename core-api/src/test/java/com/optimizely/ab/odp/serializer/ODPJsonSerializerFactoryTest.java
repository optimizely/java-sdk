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

import com.optimizely.ab.internal.PropertyUtils;
import com.optimizely.ab.odp.serializer.impl.GsonSerializer;
import com.optimizely.ab.odp.serializer.impl.JsonSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ODPJsonSerializerFactoryTest {
    @Before
    @After
    public void clearParserSystemProperty() {
        PropertyUtils.clear("default_parser");
    }

    @Test
    public void getGsonSerializerWhenNoDefaultIsSet() {
        assertEquals(GsonSerializer.class, ODPJsonSerializerFactory.getSerializer().getClass());
    }

    @Test
    public void getCorrectSerializerWhenValidDefaultIsProvided() {
        PropertyUtils.set("default_parser", "JSON_CONFIG_PARSER");
        assertEquals(JsonSerializer.class, ODPJsonSerializerFactory.getSerializer().getClass());
    }

    @Test
    public void getGsonSerializerWhenGivenDefaultSerializerDoesNotExist() {
        PropertyUtils.set("default_parser", "GARBAGE_VALUE");
        assertEquals(GsonSerializer.class, ODPJsonSerializerFactory.getSerializer().getClass());
    }
}

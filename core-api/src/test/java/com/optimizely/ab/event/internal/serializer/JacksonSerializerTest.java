/**
 *
 *    Copyright 2016, Optimizely
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

import com.optimizely.ab.event.internal.payloadV2.Conversion;
import com.optimizely.ab.event.internal.payloadV2.Impression;

import org.junit.Test;

import java.io.IOException;

import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversion;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpression;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionJson;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionJson;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JacksonSerializerTest {

    private JacksonSerializer serializer = new JacksonSerializer();

    @Test
    public void serializeImpression() throws IOException {
        Impression impression = generateImpression();
        assertThat(serializer.serialize(impression), is(generateImpressionJson()));
    }

    @Test
    public void serializeConversion() throws IOException {
        Conversion conversion = generateConversion();
        assertThat(serializer.serialize(conversion), is(generateConversionJson()));
    }
}


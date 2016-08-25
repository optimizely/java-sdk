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


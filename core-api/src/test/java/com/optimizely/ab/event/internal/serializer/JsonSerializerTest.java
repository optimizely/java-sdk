package com.optimizely.ab.event.internal.serializer;

import com.optimizely.ab.event.internal.payloadV2.Conversion;
import com.optimizely.ab.event.internal.payloadV2.Impression;

import org.json.JSONObject;

import org.junit.Test;

import java.io.IOException;

import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversion;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpression;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionJson;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionJson;

import static org.junit.Assert.assertTrue;

public class JsonSerializerTest {

    private JsonSerializer serializer = new JsonSerializer();

    @Test
    public void serializeImpression() throws IOException {
        Impression impression = generateImpression();
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        JSONObject actual = new JSONObject(serializer.serialize(impression));
        JSONObject expected = new JSONObject(generateImpressionJson());

        assertTrue(actual.similar(expected));
    }

    @Test
    public void serializeConversion() throws IOException {
        Conversion conversion = generateConversion();
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        JSONObject actual = new JSONObject(serializer.serialize(conversion));
        JSONObject expected = new JSONObject(generateConversionJson());

        assertTrue(actual.similar(expected));
    }
}

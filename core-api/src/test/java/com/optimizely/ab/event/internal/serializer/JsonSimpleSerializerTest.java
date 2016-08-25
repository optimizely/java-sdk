package com.optimizely.ab.event.internal.serializer;

import com.optimizely.ab.event.internal.payloadV2.Conversion;
import com.optimizely.ab.event.internal.payloadV2.Impression;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.junit.Test;

import java.io.IOException;

import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversion;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpression;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionJson;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionJson;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JsonSimpleSerializerTest {

    private JsonSimpleSerializer serializer = new JsonSimpleSerializer();
    private JSONParser parser = new JSONParser();

    @Test
    public void serializeImpression() throws IOException, ParseException {
        Impression impression = generateImpression();
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        JSONObject actual = (JSONObject)parser.parse(serializer.serialize(impression));
        JSONObject expected = (JSONObject)parser.parse(generateImpressionJson());

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeConversion() throws IOException, ParseException {
        Conversion conversion = generateConversion();
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        JSONObject actual = (JSONObject)parser.parse(serializer.serialize(conversion));
        JSONObject expected = (JSONObject)parser.parse(generateConversionJson());

        assertThat(actual, is(expected));
    }
}

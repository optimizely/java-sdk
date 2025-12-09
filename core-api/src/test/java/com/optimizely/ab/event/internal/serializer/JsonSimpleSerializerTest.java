/**
 *
 *    Copyright 2016-2017, 2019, Optimizely and contributors
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

import com.optimizely.ab.event.internal.payload.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversion;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionJson;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionWithSessionId;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateConversionWithSessionIdJson;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpression;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionJson;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionWithSessionId;
import static com.optimizely.ab.event.internal.serializer.SerializerTestUtils.generateImpressionWithSessionIdJson;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class JsonSimpleSerializerTest {

    private JsonSimpleSerializer serializer = new JsonSimpleSerializer();
    private JSONParser parser = new JSONParser();

    @Test
    public void serializeImpression() throws IOException, ParseException {
        EventBatch impression = generateImpression();
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        JSONObject actual = (JSONObject) parser.parse(serializer.serialize(impression));
        JSONObject expected = (JSONObject) parser.parse(generateImpressionJson());

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeImpressionWithSessionId() throws IOException, ParseException {
        EventBatch impression = generateImpressionWithSessionId();
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        JSONObject actual = (JSONObject) parser.parse(serializer.serialize(impression));
        JSONObject expected = (JSONObject) parser.parse(generateImpressionWithSessionIdJson());

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeConversion() throws IOException, ParseException {
        EventBatch conversion = generateConversion();
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        JSONObject actual = (JSONObject) parser.parse(serializer.serialize(conversion));
        JSONObject expected = (JSONObject) parser.parse(generateConversionJson());

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeConversionWithSessionId() throws IOException, ParseException {
        EventBatch conversion = generateConversionWithSessionId();
        // can't compare JSON strings since orders could vary so compare JSONObjects instead
        JSONObject actual = (JSONObject) parser.parse(serializer.serialize(conversion));
        JSONObject expected = (JSONObject) parser.parse(generateConversionWithSessionIdJson());

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeDecisionMetadataWithCmabUuid() throws IOException, ParseException {
        String cmabUuid = "test-cmab-uuid-12345";
        DecisionMetadata metadata = new DecisionMetadata("test_flag", "test_rule", "feature-test", "variation_a", true, cmabUuid);

        Decision decision = new Decision.Builder()
            .setCampaignId("layerId")
            .setExperimentId("experimentId")
            .setVariationId("variationId")
            .setIsCampaignHoldback(false)
            .setMetadata(metadata)
            .build();

        Event event = new Event.Builder()
            .setTimestamp(12345L)
            .setUuid("event-uuid")
            .setEntityId("entityId")
            .setKey("test_event")
            .setType("test_event")
            .build();

        Snapshot snapshot = new Snapshot.Builder()
            .setDecisions(Collections.singletonList(decision))
            .setEvents(Collections.singletonList(event))
            .build();

        Visitor visitor = new Visitor.Builder()
            .setVisitorId("visitor123")
            .setAttributes(Collections.<Attribute>emptyList())
            .setSnapshots(Collections.singletonList(snapshot))
            .build();

        EventBatch eventBatch = new EventBatch.Builder()
            .setClientVersion("1.0.0")
            .setAccountId("accountId")
            .setVisitors(Collections.singletonList(visitor))
            .setAnonymizeIp(false)
            .setProjectId("projectId")
            .setRevision("1")
            .build();

        String serialized = serializer.serialize(eventBatch);
        System.out.println("serialized" + serialized);

        // Verify correct serialization
        assertTrue("Serialized JSON should contain 'cmab_uuid'", serialized.contains("\"cmab_uuid\""));
        assertTrue("Serialized JSON should contain the UUID value", serialized.contains(cmabUuid));
        assertFalse("Serialized JSON must NOT contain 'cmab_u_u_i_d'", serialized.contains("\"cmab_u_u_i_d\""));
    }
}

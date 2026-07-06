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

import com.fasterxml.jackson.databind.ObjectMapper;

import com.optimizely.ab.event.internal.payload.*;

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

public class JacksonSerializerTest {

    private JacksonSerializer serializer = new JacksonSerializer();
    private ObjectMapper mapper = JacksonSerializer.createMapper();

    @Test
    public void createMapperSucceeds() {
        // Verify that createMapper() successfully creates an ObjectMapper with snake_case naming
        // This tests that the reflection logic works for the current Jackson version
        ObjectMapper testMapper = JacksonSerializer.createMapper();
        assertNotNull("Mapper should be created successfully", testMapper);
        
        // Verify snake_case naming by serializing a simple object
        class TestObject {
            @SuppressWarnings("unused")
            public String getMyFieldName() { return "test"; }
        }
        
        try {
            String json = testMapper.writeValueAsString(new TestObject());
            assertTrue("Should use snake_case naming", json.contains("my_field_name"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize with snake_case naming", e);
        }
    }

    @Test
    public void serializeImpression() throws IOException {
        EventBatch impression = generateImpression();
        // can't compare JSON strings since orders could vary so compare objects instead
        EventBatch actual = mapper.readValue(serializer.serialize(impression), EventBatch.class);
        EventBatch expected = mapper.readValue(generateImpressionJson(), EventBatch.class);

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeImpressionWithSessionId() throws IOException {
        EventBatch impression = generateImpressionWithSessionId();
        // can't compare JSON strings since orders could vary so compare objects instead
        EventBatch actual = mapper.readValue(serializer.serialize(impression), EventBatch.class);
        EventBatch expected = mapper.readValue(generateImpressionWithSessionIdJson(), EventBatch.class);

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeConversion() throws IOException {
        EventBatch conversion = generateConversion();
        // can't compare JSON strings since orders could vary so compare objects instead
        EventBatch actual = mapper.readValue(serializer.serialize(conversion), EventBatch.class);
        EventBatch expected = mapper.readValue(generateConversionJson(), EventBatch.class);

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeConversionWithSessionId() throws IOException {
        EventBatch conversion = generateConversionWithSessionId();
        // can't compare JSON strings since orders could vary so compare objects instead
        EventBatch actual = mapper.readValue(serializer.serialize(conversion), EventBatch.class);
        EventBatch expected = mapper.readValue(generateConversionWithSessionIdJson(), EventBatch.class);

        assertThat(actual, is(expected));
    }

    @Test
    public void serializeDecisionMetadataWithCmabUuid() throws IOException {
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
        // Critical assertion: must be "cmab_uuid", NOT "cmab_u_u_i_d"
        assertTrue("Serialized JSON should contain 'cmab_uuid'", serialized.contains("\"cmab_uuid\""));
        assertTrue("Serialized JSON should contain the UUID value", serialized.contains(cmabUuid));
        assertFalse("Serialized JSON must NOT contain 'cmab_u_u_i_d'", serialized.contains("\"cmab_u_u_i_d\""));
    }

    @Test
    public void serializeDecisionWithNullVariationId_emitsExplicitJsonNull() {
        // JacksonSerializer sets Include.NON_NULL globally, so fields default to being
        // stripped when null. Decision.variation_id must override that with
        // @JsonInclude(ALWAYS) so the wire payload carries "variation_id": null instead
        // of dropping the key — required for cross-SDK byte-equivalence under FSSDK-12813.
        DecisionMetadata metadata = new DecisionMetadata.Builder()
            .setFlagKey("test_flag")
            .setRuleKey("test_rule")
            .setRuleType("holdout")
            .setVariationKey("ho_off_key")
            .setEnabled(false)
            .build();

        Decision decision = new Decision.Builder()
            .setCampaignId("12345")
            .setExperimentId("67890")
            .setVariationId(null)
            .setIsCampaignHoldback(false)
            .setMetadata(metadata)
            .build();

        Snapshot snapshot = new Snapshot.Builder()
            .setDecisions(Collections.singletonList(decision))
            .setEvents(Collections.<Event>emptyList())
            .build();

        Visitor visitor = new Visitor.Builder()
            .setVisitorId("visitor123")
            .setAttributes(Collections.<Attribute>emptyList())
            .setSnapshots(Collections.singletonList(snapshot))
            .build();

        EventBatch eventBatch = new EventBatch.Builder()
            .setAccountId("accountId")
            .setProjectId("projectId")
            .setRevision("1")
            .setVisitors(Collections.singletonList(visitor))
            .build();

        String serialized = serializer.serialize(eventBatch);
        assertTrue(
            "variation_id key must be present with explicit null when normalization "
                + "yields null: " + serialized,
            serialized.contains("\"variation_id\":null"));
    }
}

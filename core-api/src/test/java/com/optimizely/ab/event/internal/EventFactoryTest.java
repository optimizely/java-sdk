/**
 *
 *    Copyright 2016-2019, Optimizely and contributors
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
package com.optimizely.ab.event.internal;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.config.*;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.internal.ControlAttribute;
import com.optimizely.ab.internal.ReservedEventKey;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.DatafileProjectConfigTestUtils.validProjectConfigV4;
import static com.optimizely.ab.config.ValidProjectConfigV4.*;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class EventFactoryTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{
            {
                2,
                validProjectConfigV2()
            },
            {
                4,
                validProjectConfigV4()
            }
        });
    }

    private Gson gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

    private static String userId = "userId";
    private int datafileVersion;
    private ProjectConfig validProjectConfig;

    public EventFactoryTest(int datafileVersion,
                            ProjectConfig validProjectConfig) {
        this.datafileVersion = datafileVersion;
        this.validProjectConfig = validProjectConfig;
    }

    @After
    public void tearDown() {
        ClientEngineInfo.setClientEngine(EventBatch.ClientEngine.JAVA_SDK);
    }

    /**
     * Verify {@link com.optimizely.ab.event.internal.payload.EventBatch} event creation
     */
    @Test
    public void createImpressionEventPassingUserAgentAttribute() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        String userId = "userId";
        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put(attribute.getKey(), "value");
        attributeMap.put(ControlAttribute.USER_AGENT_ATTRIBUTE.toString(), "Chrome");

        Decision expectedDecision = new Decision.Builder()
            .setCampaignId(activatedExperiment.getLayerId())
            .setExperimentId(activatedExperiment.getId())
            .setVariationId(bucketedVariation.getId())
            .setIsCampaignHoldback(false)
            .build();

        com.optimizely.ab.event.internal.payload.Attribute feature = new com.optimizely.ab.event.internal.payload.Attribute.Builder()
            .setEntityId(attribute.getId())
            .setKey(attribute.getKey())
            .setType(com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE)
            .setValue("value")
            .build();

        com.optimizely.ab.event.internal.payload.Attribute userAgentFeature = new com.optimizely.ab.event.internal.payload.Attribute.Builder()
            .setEntityId(ControlAttribute.USER_AGENT_ATTRIBUTE.toString())
            .setKey(ControlAttribute.USER_AGENT_ATTRIBUTE.toString())
            .setType(com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE)
            .setValue("Chrome")
            .build();

        com.optimizely.ab.event.internal.payload.Attribute botFilteringFeature = getBotFilteringAttribute();

        List<com.optimizely.ab.event.internal.payload.Attribute> expectedUserFeatures;

        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))
            expectedUserFeatures = Arrays.asList(userAgentFeature, feature, botFilteringFeature);
        else
            expectedUserFeatures = Arrays.asList(userAgentFeature, feature);

        LogEvent impressionEvent = createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation,
            userId, attributeMap);

        // verify that request endpoint is correct
        assertThat(impressionEvent.getEndpointUrl(), is(EventFactory.EVENT_ENDPOINT));

        EventBatch eventBatch = gson.fromJson(impressionEvent.getBody(), EventBatch.class);

        // verify payload information
        assertThat(eventBatch.getVisitors().get(0).getVisitorId(), is(userId));
        assertThat((double) eventBatch.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTimestamp(), closeTo((double) System.currentTimeMillis(), 1000.0));
        assertFalse(eventBatch.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0).getIsCampaignHoldback());
        assertThat(eventBatch.getAnonymizeIp(), is(validProjectConfig.getAnonymizeIP()));
        assertTrue(eventBatch.getEnrichDecisions());
        assertThat(eventBatch.getProjectId(), is(validProjectConfig.getProjectId()));
        assertThat(eventBatch.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0), is(expectedDecision));
        assertThat(eventBatch.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0).getCampaignId(),
            is(activatedExperiment.getLayerId()));
        assertThat(eventBatch.getAccountId(), is(validProjectConfig.getAccountId()));
        assertThat(eventBatch.getVisitors().get(0).getAttributes(), is(expectedUserFeatures));
        assertThat(eventBatch.getClientName(), is(EventBatch.ClientEngine.JAVA_SDK.getClientEngineValue()));
        assertThat(eventBatch.getClientVersion(), is(BuildVersionInfo.VERSION));
        assertNull(eventBatch.getVisitors().get(0).getSessionId());
    }

    /**
     * Verify {@link com.optimizely.ab.event.internal.payload.EventBatch} event creation
     */
    @Test
    public void createImpressionEvent() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        Experiment activatedExperiment = validProjectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        String userId = "userId";
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");

        Decision expectedDecision = new Decision.Builder()
            .setCampaignId(activatedExperiment.getLayerId())
            .setExperimentId(activatedExperiment.getId())
            .setVariationId(bucketedVariation.getId())
            .setIsCampaignHoldback(false)
            .build();

        com.optimizely.ab.event.internal.payload.Attribute feature = new com.optimizely.ab.event.internal.payload.Attribute.Builder()
            .setEntityId(attribute.getId())
            .setKey(attribute.getKey())
            .setType(com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE)
            .setValue("value")
            .build();

        com.optimizely.ab.event.internal.payload.Attribute botFilteringFeature = getBotFilteringAttribute();

        List<com.optimizely.ab.event.internal.payload.Attribute> expectedUserFeatures;

        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()))
            expectedUserFeatures = Arrays.asList(feature, botFilteringFeature);
        else
            expectedUserFeatures = Arrays.asList(feature);

        LogEvent impressionEvent = createImpressionEvent(validProjectConfig, activatedExperiment, bucketedVariation,
            userId, attributeMap);

        // verify that request endpoint is correct
        assertThat(impressionEvent.getEndpointUrl(), is(EventFactory.EVENT_ENDPOINT));

        EventBatch eventBatch = gson.fromJson(impressionEvent.getBody(), EventBatch.class);

        // verify payload information
        assertThat(eventBatch.getVisitors().get(0).getVisitorId(), is(userId));
        assertThat((double) eventBatch.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTimestamp(), closeTo((double) System.currentTimeMillis(), 1000.0));
        assertFalse(eventBatch.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0).getIsCampaignHoldback());
        assertThat(eventBatch.getAnonymizeIp(), is(validProjectConfig.getAnonymizeIP()));
        assertTrue(eventBatch.getEnrichDecisions());
        assertThat(eventBatch.getProjectId(), is(validProjectConfig.getProjectId()));
        assertThat(eventBatch.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0), is(expectedDecision));
        assertThat(eventBatch.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0).getCampaignId(),
            is(activatedExperiment.getLayerId()));
        assertThat(eventBatch.getAccountId(), is(validProjectConfig.getAccountId()));
        assertThat(eventBatch.getVisitors().get(0).getAttributes(), is(expectedUserFeatures));
        assertThat(eventBatch.getClientName(), is(EventBatch.ClientEngine.JAVA_SDK.getClientEngineValue()));
        assertThat(eventBatch.getClientVersion(), is(BuildVersionInfo.VERSION));
        assertNull(eventBatch.getVisitors().get(0).getSessionId());
    }

    /**
     * Verify that passing through an unknown attribute causes that attribute to be ignored, rather than
     * causing an exception to be thrown.
     */
    @Test
    public void createImpressionEventIgnoresUnknownAttributes() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = validProjectConfig;
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        LogEvent impressionEvent =
            createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation, "userId",
                Collections.singletonMap("unknownAttribute", "blahValue"));

        EventBatch impression = gson.fromJson(impressionEvent.getBody(), EventBatch.class);

        // verify that no Feature is created for "unknownAtrribute" -> "blahValue"
        for (com.optimizely.ab.event.internal.payload.Attribute feature : impression.getVisitors().get(0).getAttributes()) {
            assertFalse(feature.getKey() == "unknownAttribute");
            assertFalse(feature.getValue() == "blahValue");
        }
    }

    /**
     * Verify that passing through an list value attribute causes that attribute to be ignored, rather than
     * causing an exception to be thrown and passing only the valid attributes.
     */
    @Test
    public void createConversionEventIgnoresInvalidAndAcceptsValidAttributes() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        EventType eventType = validProjectConfig.getEventTypes().get(0);

        Attribute attribute1 = validProjectConfig.getAttributes().get(0);
        Attribute attribute2 = validProjectConfig.getAttributes().get(1);
        Attribute doubleAttribute = validProjectConfig.getAttributes().get(5);
        Attribute integerAttribute = validProjectConfig.getAttributes().get(4);
        Attribute boolAttribute = validProjectConfig.getAttributes().get(3);
        Attribute emptyAttribute = validProjectConfig.getAttributes().get(6);

        BigInteger bigInteger = new BigInteger("12323");
        BigDecimal bigDecimal = new BigDecimal("123");
        double validDoubleAttribute = 13.1;
        int validIntegerAttribute = 12;
        boolean validBoolAttribute = true;

        Map<String, Object> eventTagMap = new HashMap<>();
        eventTagMap.put("boolean_param", false);
        eventTagMap.put("string_param", "123");

        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(attribute1.getKey(), bigInteger);
        attributes.put(attribute2.getKey(), bigDecimal);
        attributes.put(doubleAttribute.getKey(), validDoubleAttribute);
        attributes.put(integerAttribute.getKey(), validIntegerAttribute);
        attributes.put(boolAttribute.getKey(), validBoolAttribute);
        attributes.put(emptyAttribute.getKey(), validBoolAttribute);

        LogEvent conversionEvent = createConversionEvent(
                validProjectConfig,
                userId,
                eventType.getId(),
                eventType.getKey(),
                attributes,
                eventTagMap);

        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);

        //Check valid attributes are getting passed.
        assertEquals(conversion.getVisitors().get(0).getAttributes().get(0).getKey(), boolAttribute.getKey());
        assertEquals(conversion.getVisitors().get(0).getAttributes().get(0).getValue(), validBoolAttribute);

        assertEquals(conversion.getVisitors().get(0).getAttributes().get(1).getKey(), doubleAttribute.getKey());
        assertEquals(conversion.getVisitors().get(0).getAttributes().get(1).getValue(), validDoubleAttribute);

        assertEquals(conversion.getVisitors().get(0).getAttributes().get(2).getKey(), integerAttribute.getKey());
        assertEquals((int) ((double) conversion.getVisitors().get(0).getAttributes().get(2).getValue()), validIntegerAttribute);

        // verify that no Feature is created for attribute.getKey() -> invalidAttribute
        for (com.optimizely.ab.event.internal.payload.Attribute feature : conversion.getVisitors().get(0).getAttributes()) {
            assertNotSame(feature.getKey(), attribute1.getKey());
            assertNotSame(feature.getValue(), bigInteger);
            assertNotSame(feature.getKey(), attribute2.getKey());
            assertNotSame(feature.getValue(), bigDecimal);
            assertNotSame(feature.getKey(), emptyAttribute.getKey());
            assertNotSame(feature.getValue(), doubleAttribute);
        }
    }

    /**
     * Verify that passing through an list of invalid value attribute causes that attribute to be ignored, rather than
     * causing an exception to be thrown and passing only the valid attributes.
     */
    @Test
    public void createConversionEventIgnoresInvalidAcceptValidValOfValidAttr() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        EventType eventType = validProjectConfig.getEventTypes().get(0);

        Attribute validFloatAttribute = validProjectConfig.getAttributes().get(0);
        Attribute invalidFloatAttribute = validProjectConfig.getAttributes().get(1);
        Attribute doubleAttribute = validProjectConfig.getAttributes().get(5);
        Attribute integerAttribute = validProjectConfig.getAttributes().get(4);
        Attribute boolAttribute = validProjectConfig.getAttributes().get(3);
        Attribute emptyAttribute = validProjectConfig.getAttributes().get(6);

        float validFloatValue = 2.1f;
        float invalidFloatValue = (float) (Math.pow(2, 53) + 2000000000);
        double invalidDoubleAttribute = Math.pow(2, 53) + 2;
        long validLongAttribute = 12;
        boolean validBoolAttribute = true;

        Map<String, Object> eventTagMap = new HashMap<>();
        eventTagMap.put("boolean_param", false);
        eventTagMap.put("string_param", "123");

        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(validFloatAttribute.getKey(), validFloatValue);
        attributes.put(invalidFloatAttribute.getKey(), invalidFloatValue);
        attributes.put(doubleAttribute.getKey(), invalidDoubleAttribute);
        attributes.put(integerAttribute.getKey(), validLongAttribute);
        attributes.put(boolAttribute.getKey(), validBoolAttribute);
        attributes.put(emptyAttribute.getKey(), validBoolAttribute);

        LogEvent conversionEvent = createConversionEvent(
            validProjectConfig,
            userId,
            eventType.getId(),
            eventType.getKey(),
            attributes,
            eventTagMap);

        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);

        //Check valid attributes are getting passed.
        assertEquals(conversion.getVisitors().get(0).getAttributes().get(0).getKey(), boolAttribute.getKey());
        assertEquals(conversion.getVisitors().get(0).getAttributes().get(0).getValue(), validBoolAttribute);
        assertEquals(conversion.getVisitors().get(0).getAttributes().get(1).getKey(), validFloatAttribute.getKey());
        //In the condition below we are checking Value of float with double value because impression gets visitors from JSON so that converts it into double
        assertEquals(conversion.getVisitors().get(0).getAttributes().get(1).getValue(), 2.1);
        assertEquals(conversion.getVisitors().get(0).getAttributes().get(2).getKey(), integerAttribute.getKey());
        assertEquals((long) ((double) conversion.getVisitors().get(0).getAttributes().get(2).getValue()), validLongAttribute);

        // verify that no Feature is created for attribute.getKey() -> invalidAttribute
        for (com.optimizely.ab.event.internal.payload.Attribute feature : conversion.getVisitors().get(0).getAttributes()) {
            assertNotSame(feature.getKey(), invalidFloatAttribute.getKey());
            assertNotSame(feature.getValue(), invalidFloatValue);
            assertNotSame(feature.getKey(), doubleAttribute.getKey());
            assertNotSame(feature.getValue(), invalidDoubleAttribute);
            assertNotSame(feature.getKey(), emptyAttribute.getKey());
            assertNotSame(feature.getValue(), doubleAttribute);
        }
    }

    /**
     * Verify that passing through an list of -ve invalid attribute value causes that attribute to be ignored, rather than
     * causing an exception to be thrown and passing only the valid attributes.
     */
    @Test
    public void createConversionEventIgnoresNegativeInvalidAndAcceptsValidValueOfValidTypeAttributes() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));

        EventType eventType = validProjectConfig.getEventTypes().get(0);

        Attribute validFloatAttribute = validProjectConfig.getAttributes().get(0);
        Attribute invalidFloatAttribute = validProjectConfig.getAttributes().get(1);
        Attribute doubleAttribute = validProjectConfig.getAttributes().get(5);
        Attribute integerAttribute = validProjectConfig.getAttributes().get(4);
        Attribute emptyAttribute = validProjectConfig.getAttributes().get(6);

        float validFloatValue = -2.1f;
        float invalidFloatValue = -((float) (Math.pow(2, 53) + 2000000000));
        double invalidDoubleAttribute = -(Math.pow(2, 53) + 2);
        long validLongAttribute = -12;

        Map<String, Object> eventTagMap = new HashMap<>();
        eventTagMap.put("boolean_param", false);
        eventTagMap.put("string_param", "123");

        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(validFloatAttribute.getKey(), validFloatValue);
        attributes.put(invalidFloatAttribute.getKey(), invalidFloatValue);
        attributes.put(doubleAttribute.getKey(), invalidDoubleAttribute);
        attributes.put(integerAttribute.getKey(), validLongAttribute);

        LogEvent conversionEvent = createConversionEvent(
            validProjectConfig,
            userId,
            eventType.getId(),
            eventType.getKey(),
            attributes,
            eventTagMap);

        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);

        //Check valid attributes are getting passed.
        assertEquals(conversion.getVisitors().get(0).getAttributes().get(0).getKey(), validFloatAttribute.getKey());
        //In below condition I am checking Value of float with double value because impression gets visitors from json so that converts it into double
        assertEquals(conversion.getVisitors().get(0).getAttributes().get(0).getValue(), -2.1);
        assertEquals(conversion.getVisitors().get(0).getAttributes().get(1).getKey(), integerAttribute.getKey());
        assertEquals((long) ((double) conversion.getVisitors().get(0).getAttributes().get(1).getValue()), validLongAttribute);

        // verify that no Feature is created for attribute.getKey() -> invalidAttribute
        for (com.optimizely.ab.event.internal.payload.Attribute feature : conversion.getVisitors().get(0).getAttributes()) {
            assertNotSame(feature.getKey(), invalidFloatAttribute.getKey());
            assertNotSame(feature.getValue(), invalidFloatValue);
            assertNotSame(feature.getKey(), doubleAttribute.getKey());
            assertNotSame(feature.getValue(), invalidDoubleAttribute);
            assertNotSame(feature.getKey(), emptyAttribute.getKey());
            assertNotSame(feature.getValue(), doubleAttribute);
        }
    }

    /**
     * Verify that passing through an list value attribute causes that attribute to be ignored, rather than
     * causing an exception to be thrown.
     */
    @Test
    public void createImpressionEventIgnoresInvalidAttributes() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));
        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = validProjectConfig;
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute1 = validProjectConfig.getAttributes().get(0);
        Attribute attribute2 = validProjectConfig.getAttributes().get(1);
        BigInteger bigInteger = new BigInteger("12323");
        BigDecimal bigDecimal = new BigDecimal("123");

        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(attribute1.getKey(), bigInteger);
        attributes.put(attribute2.getKey(), bigDecimal);

        LogEvent impressionEvent =
            createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation, "userId",
                attributes);

        EventBatch impression = gson.fromJson(impressionEvent.getBody(), EventBatch.class);

        // verify that no Feature is created for attribute.getKey() -> invalidAttribute
        for (com.optimizely.ab.event.internal.payload.Attribute feature : impression.getVisitors().get(0).getAttributes()) {
            assertNotSame(feature.getKey(), attribute1.getKey());
            assertNotSame(feature.getValue(), bigInteger);
            assertNotSame(feature.getKey(), attribute2.getKey());
            assertNotSame(feature.getValue(), bigDecimal);
        }
    }

    /**
     * Verify that Integer, Decimal, Bool and String variables are allowed to pass.
     */
    @Test
    public void createImpressionEventWithIntegerDecimalBoolAndStringAttributes() {
        assumeTrue(datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString()));
        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = validProjectConfig;
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute doubleAttribute = validProjectConfig.getAttributes().get(5);
        Attribute integerAttribute = validProjectConfig.getAttributes().get(4);
        Attribute boolAttribute = validProjectConfig.getAttributes().get(3);
        Attribute stringAttribute = validProjectConfig.getAttributes().get(0);
        double validDoubleAttribute = 13.1;
        int validIntegerAttribute = 12;
        boolean validBoolAttribute = true;
        String validStringAttribute = "grayfindor";

        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(doubleAttribute.getKey(), validDoubleAttribute);
        attributes.put(integerAttribute.getKey(), validIntegerAttribute);
        attributes.put(boolAttribute.getKey(), validBoolAttribute);
        attributes.put(stringAttribute.getKey(), validStringAttribute);

        LogEvent impressionEvent =
            createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation, "userId",
                attributes);

        EventBatch impression = gson.fromJson(impressionEvent.getBody(), EventBatch.class);


        assertEquals(impression.getVisitors().get(0).getAttributes().get(0).getKey(), boolAttribute.getKey());
        assertEquals(impression.getVisitors().get(0).getAttributes().get(0).getValue(), validBoolAttribute);

        assertEquals(impression.getVisitors().get(0).getAttributes().get(1).getKey(), doubleAttribute.getKey());
        assertEquals(impression.getVisitors().get(0).getAttributes().get(1).getValue(), validDoubleAttribute);

        assertEquals(impression.getVisitors().get(0).getAttributes().get(2).getKey(), integerAttribute.getKey());
        assertEquals((int) ((double) impression.getVisitors().get(0).getAttributes().get(2).getValue()), validIntegerAttribute);

        assertEquals(impression.getVisitors().get(0).getAttributes().get(3).getKey(), stringAttribute.getKey());
        assertEquals(impression.getVisitors().get(0).getAttributes().get(3).getValue(), validStringAttribute);

    }

    /**
     * Verify that passing through an null value attribute causes that attribute to be ignored, rather than
     * causing an exception to be thrown.
     */
    @Test
    public void createImpressionEventIgnoresNullAttributes() {
        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = validProjectConfig;
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = validProjectConfig.getAttributes().get(0);

        LogEvent impressionEvent =
            createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation, "userId",
                Collections.singletonMap(attribute.getKey(), null));

        EventBatch impression = gson.fromJson(impressionEvent.getBody(), EventBatch.class);

        // verify that no Feature is created for attribute.getKey() -> null
        for (com.optimizely.ab.event.internal.payload.Attribute feature : impression.getVisitors().get(0).getAttributes()) {
            assertNotSame(feature.getKey(), attribute.getKey());
            assertNotSame(feature.getValue(), null);
        }
    }

    /**
     * Verify that supplying {@link ClientEngineInfo} with a custom client engine and client version results in impression
     * events being sent with the overriden values.
     */
    @Test
    public void createImpressionEventAndroidClientEngineClientVersion() throws Exception {
        ClientEngineInfo.setClientEngine(EventBatch.ClientEngine.ANDROID_SDK);
        ProjectConfig projectConfig = validProjectConfigV2();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = projectConfig.getAttributes().get(0);
        String userId = "userId";
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");

        LogEvent impressionEvent = createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
            userId, attributeMap);
        EventBatch impression = gson.fromJson(impressionEvent.getBody(), EventBatch.class);

        assertThat(impression.getClientName(), is(EventBatch.ClientEngine.ANDROID_SDK.getClientEngineValue()));
//        assertThat(impression.getClientVersion(), is("0.0.0"));
    }

    /**
     * Verify that supplying {@link ClientEngineInfo} with a custom Android TV client engine and client version
     * results in impression events being sent with the overriden values.
     */
    @Test
    public void createImpressionEventAndroidTVClientEngineClientVersion() throws Exception {
        String clientVersion = "0.0.0";
        ClientEngineInfo.setClientEngine(EventBatch.ClientEngine.ANDROID_TV_SDK);
        ProjectConfig projectConfig = validProjectConfigV2();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = projectConfig.getAttributes().get(0);
        String userId = "userId";
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");

        LogEvent impressionEvent = createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
            userId, attributeMap);
        EventBatch impression = gson.fromJson(impressionEvent.getBody(), EventBatch.class);

        assertThat(impression.getClientName(), is(EventBatch.ClientEngine.ANDROID_TV_SDK.getClientEngineValue()));
//        assertThat(impression.getClientVersion(), is(clientVersion));
    }

    /**
     * Verify {@link com.optimizely.ab.event.internal.payload.EventBatch} event creation
     */
    @Test
    public void createConversionEvent() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String userId = "userId";

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), AUDIENCE_GRYFFINDOR_VALUE);
        Map<String, Object> eventTagMap = new HashMap<String, Object>();
        eventTagMap.put("boolean_param", false);
        eventTagMap.put("string_param", "123");
        LogEvent conversionEvent = createConversionEvent(
                validProjectConfig,
                userId,
                eventType.getId(),
                eventType.getKey(),
                attributeMap,
                eventTagMap);

        // verify that the request endpoint is correct
        assertThat(conversionEvent.getEndpointUrl(), is(EventFactory.EVENT_ENDPOINT));

        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);

        // verify payload information
        assertThat(conversion.getVisitors().get(0).getVisitorId(), is(userId));
        assertThat((double) conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTimestamp(),
            closeTo((double) System.currentTimeMillis(), 120.0));
        assertThat(conversion.getProjectId(), is(validProjectConfig.getProjectId()));
        assertThat(conversion.getAccountId(), is(validProjectConfig.getAccountId()));

        com.optimizely.ab.event.internal.payload.Attribute feature = new com.optimizely.ab.event.internal.payload.Attribute.Builder()
            .setEntityId(attribute.getId()).setKey(attribute.getKey())
            .setType(com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE)
            .setValue(AUDIENCE_GRYFFINDOR_VALUE)
            .build();

        List<com.optimizely.ab.event.internal.payload.Attribute> expectedUserFeatures = new ArrayList<com.optimizely.ab.event.internal.payload.Attribute>();
        expectedUserFeatures.add(feature);

        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString())) {
            expectedUserFeatures.add(getBotFilteringAttribute());
        }

        assertEquals(conversion.getVisitors().get(0).getAttributes(), expectedUserFeatures);
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getEntityId(), eventType.getId());
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getKey(), eventType.getKey());
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getRevenue(), null);
        assertTrue(conversion.getVisitors().get(0).getAttributes().containsAll(expectedUserFeatures));
        assertTrue(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTags().equals(eventTagMap));
        assertEquals(conversion.getAnonymizeIp(), validProjectConfig.getAnonymizeIP());
        assertTrue(conversion.getEnrichDecisions());
        assertEquals(conversion.getClientName(), EventBatch.ClientEngine.JAVA_SDK.getClientEngineValue());
        assertEquals(conversion.getClientVersion(), BuildVersionInfo.VERSION);
    }

    /**
     * Verify {@link com.optimizely.ab.event.internal.payload.EventBatch} event creation
     * passing User Agent reserved attribute in attribute map and to check it exist in visitors.attributes
     */
    @Test
    public void createConversionEventPassingUserAgentAttribute() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String userId = "userId";

        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put(attribute.getKey(), AUDIENCE_GRYFFINDOR_VALUE);
        attributeMap.put(ControlAttribute.USER_AGENT_ATTRIBUTE.toString(), "Chrome");
        Map<String, Object> eventTagMap = new HashMap<String, Object>();
        eventTagMap.put("boolean_param", false);
        eventTagMap.put("string_param", "123");
        LogEvent conversionEvent = createConversionEvent(
                validProjectConfig,
                userId,
                eventType.getId(),
                eventType.getKey(),
                attributeMap,
                eventTagMap);

        // verify that the request endpoint is correct
        assertThat(conversionEvent.getEndpointUrl(), is(EventFactory.EVENT_ENDPOINT));

        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);

        // verify payload information
        assertThat(conversion.getVisitors().get(0).getVisitorId(), is(userId));
        assertThat(conversion.getProjectId(), is(validProjectConfig.getProjectId()));
        assertThat(conversion.getAccountId(), is(validProjectConfig.getAccountId()));

        com.optimizely.ab.event.internal.payload.Attribute feature = new com.optimizely.ab.event.internal.payload.Attribute.Builder()
            .setEntityId(attribute.getId()).setKey(attribute.getKey())
            .setType(com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE)
            .setValue(AUDIENCE_GRYFFINDOR_VALUE)
            .build();

        com.optimizely.ab.event.internal.payload.Attribute userAgentFeature = new com.optimizely.ab.event.internal.payload.Attribute.Builder()
            .setEntityId(ControlAttribute.USER_AGENT_ATTRIBUTE.toString())
            .setKey(ControlAttribute.USER_AGENT_ATTRIBUTE.toString())
            .setType(com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE)
            .setValue("Chrome")
            .build();

        List<com.optimizely.ab.event.internal.payload.Attribute> expectedUserFeatures = new ArrayList<com.optimizely.ab.event.internal.payload.Attribute>();
        expectedUserFeatures.add(userAgentFeature);
        expectedUserFeatures.add(feature);

        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString())) {
            expectedUserFeatures.add(getBotFilteringAttribute());
        }

        assertEquals(conversion.getVisitors().get(0).getAttributes(), expectedUserFeatures);
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getEntityId(), eventType.getId());
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getKey(), eventType.getKey());
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getRevenue(), null);
        assertTrue(conversion.getVisitors().get(0).getAttributes().containsAll(expectedUserFeatures));
        assertTrue(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTags().equals(eventTagMap));
        assertEquals(conversion.getAnonymizeIp(), validProjectConfig.getAnonymizeIP());
        assertTrue(conversion.getEnrichDecisions());
        assertEquals(conversion.getClientName(), EventBatch.ClientEngine.JAVA_SDK.getClientEngineValue());
        assertEquals(conversion.getClientVersion(), BuildVersionInfo.VERSION);
    }

    /**
     * Verify that "revenue" and "value" are properly recorded in a conversion request as {@link com.optimizely.ab.event.internal.payload.Event} objects.
     * "revenue" is fixed-point and "value" is floating-point.
     */
    @Test
    public void createConversionParamsWithEventMetrics() throws Exception {
        Long revenue = 1234L;
        Double value = 13.37;

        // use the "valid" project config and its associated experiment, variation, and attributes
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(0);

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);

        // Bucket to the first variation for all experiments.
        for (Experiment experiment : validProjectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId, validProjectConfig))
                .thenReturn(experiment.getVariations().get(0));
        }

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        Map<String, Object> eventTagMap = new HashMap<String, Object>();
        eventTagMap.put(ReservedEventKey.REVENUE.toString(), revenue);
        eventTagMap.put(ReservedEventKey.VALUE.toString(), value);

        LogEvent conversionEvent = createConversionEvent(
            validProjectConfig, 
            userId,
            eventType.getId(), 
            eventType.getKey(), 
            attributeMap, 
            eventTagMap);

        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);
        // we're not going to verify everything, only the event metrics
        assertThat(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getRevenue().longValue(), is(revenue));
        assertThat(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getValue().doubleValue(), is(value));
    }

    /**
     * Verify that conversion event is always created.
     */
    @Test
    public void createConversionEventForcedVariationBucketingPrecedesAudienceEval() {
        EventType eventType;
        String whitelistedUserId;
        if (datafileVersion == 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_BASIC_EVENT_KEY);
            whitelistedUserId = MULTIVARIATE_EXPERIMENT_FORCED_VARIATION_USER_ID_GRED;
        } else {
            eventType = validProjectConfig.getEventTypes().get(0);
            whitelistedUserId = "testUser1";
        }

        LogEvent conversionEvent = createConversionEvent(
                validProjectConfig,
                whitelistedUserId,
                eventType.getId(),
                eventType.getKey(),
                Collections.emptyMap(),
                Collections.emptyMap());
        
        assertNotNull(conversionEvent);
    }

    /**
     * Verify conversion event is always created.
     */
    @Test
    public void createConversionEventExperimentStatusPrecedesForcedVariation() {
        EventType eventType;
        if (datafileVersion == 4) {
            eventType = validProjectConfig.getEventNameMapping().get(EVENT_PAUSED_EXPERIMENT_KEY);
        } else {
            eventType = validProjectConfig.getEventTypes().get(3);
        }
        String whitelistedUserId = PAUSED_EXPERIMENT_FORCED_VARIATION_USER_ID_CONTROL;

        LogEvent conversionEvent = createConversionEvent(
            validProjectConfig,
            whitelistedUserId,
            eventType.getId(),
            eventType.getKey(),
            Collections.emptyMap(),
            Collections.emptyMap());

        assertNotNull(conversionEvent);
    }

    /**
     * Verify that supplying {@link ClientEngineInfo} with a custom client engine and client version results in conversion
     * events being sent with the overriden values.
     */
    @Test
    public void createConversionEventAndroidClientEngineClientVersion() throws Exception {
        ClientEngineInfo.setClientEngine(EventBatch.ClientEngine.ANDROID_SDK);
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(0);

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);
        for (Experiment experiment : validProjectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId, validProjectConfig))
                .thenReturn(experiment.getVariations().get(0));
        }

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");

        LogEvent conversionEvent = createConversionEvent(
            validProjectConfig,
            userId,
            eventType.getId(),
            eventType.getKey(),
            attributeMap,
            Collections.emptyMap());

        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);

        assertThat(conversion.getClientName(), is(EventBatch.ClientEngine.ANDROID_SDK.getClientEngineValue()));
//        assertThat(conversion.getClientVersion(), is("0.0.0"));
    }

    /**
     * Verify that supplying {@link ClientEngineInfo} with a Android TV client engine and client version results in
     * conversion events being sent with the overriden values.
     */
    @Test
    public void createConversionEventAndroidTVClientEngineClientVersion() throws Exception {
        String clientVersion = "0.0.0";
        ClientEngineInfo.setClientEngine(EventBatch.ClientEngine.ANDROID_TV_SDK);
        ProjectConfig projectConfig = validProjectConfigV2();
        Attribute attribute = projectConfig.getAttributes().get(0);
        EventType eventType = projectConfig.getEventTypes().get(0);
        String userId = "userId";

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);
        for (Experiment experiment : projectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId, validProjectConfig))
                .thenReturn(experiment.getVariations().get(0));
        }

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");

        LogEvent conversionEvent = createConversionEvent(
            projectConfig,
            userId,
            eventType.getId(),
            eventType.getKey(),
            attributeMap,
            Collections.emptyMap());
        
        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);

        assertThat(conversion.getClientName(), is(EventBatch.ClientEngine.ANDROID_TV_SDK.getClientEngineValue()));
//        assertThat(conversion.getClientVersion(), is(clientVersion));
    }

    /**
     * Verify that supplying an empty Experiment Variation map returns an Event {@link LogEvent}.
     */
    @Test
    public void createConversionEventReturnsNotNullWhenExperimentVariationMapIsEmpty() {
        EventType eventType = validProjectConfig.getEventTypes().get(0);

        LogEvent conversionEvent = createConversionEvent(
                validProjectConfig,
                userId,
                eventType.getId(),
                eventType.getKey(),
                Collections.<String, String>emptyMap(),
                Collections.<String, String>emptyMap()
        );

        assertNotNull(conversionEvent);
    }

    /**
     * Verify {@link com.optimizely.ab.event.internal.payload.EventBatch} event creation
     */
    @Test
    public void createImpressionEventWithBucketingId() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = validProjectConfig;
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = projectConfig.getAttributes().get(0);
        String userId = "userId";
        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put(attribute.getKey(), "value");

        attributeMap.put(ControlAttribute.BUCKETING_ATTRIBUTE.toString(), "variation");

        Decision expectedDecision = new Decision.Builder()
            .setCampaignId(activatedExperiment.getLayerId())
            .setExperimentId(activatedExperiment.getId())
            .setVariationId(bucketedVariation.getId())
            .setIsCampaignHoldback(false)
            .build();

        com.optimizely.ab.event.internal.payload.Attribute feature = new com.optimizely.ab.event.internal.payload.Attribute.Builder()
            .setEntityId(attribute.getId()).setKey(attribute.getKey())
            .setType(com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE)
            .setValue("value")
            .build();

        com.optimizely.ab.event.internal.payload.Attribute feature1 = new com.optimizely.ab.event.internal.payload.Attribute.Builder()
            .setEntityId(ControlAttribute.BUCKETING_ATTRIBUTE.toString())
            .setKey(ControlAttribute.BUCKETING_ATTRIBUTE.toString())
            .setType(com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE)
            .setValue("variation")
            .build();

        List<com.optimizely.ab.event.internal.payload.Attribute> expectedUserFeatures = new ArrayList<com.optimizely.ab.event.internal.payload.Attribute>();
        expectedUserFeatures.add(feature);
        expectedUserFeatures.add(feature1);

        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString())) {
            expectedUserFeatures.add(getBotFilteringAttribute());
        }

        LogEvent impressionEvent = createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
            userId, attributeMap);

        // verify that request endpoint is correct
        assertThat(impressionEvent.getEndpointUrl(), is(EventFactory.EVENT_ENDPOINT));

        EventBatch impression = gson.fromJson(impressionEvent.getBody(), EventBatch.class);

        // verify payload information
        assertThat(impression.getVisitors().get(0).getVisitorId(), is(userId));
        assertThat((double) impression.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTimestamp(), closeTo((double) System.currentTimeMillis(), 1000.0));
        assertFalse(impression.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0).getIsCampaignHoldback());
        assertThat(impression.getAnonymizeIp(), is(projectConfig.getAnonymizeIP()));
        assertTrue(impression.getEnrichDecisions());
        assertThat(impression.getProjectId(), is(projectConfig.getProjectId()));
        assertThat(impression.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0), is(expectedDecision));
        assertThat(impression.getVisitors().get(0).getSnapshots().get(0).getDecisions().get(0).getCampaignId(), is(activatedExperiment.getLayerId()));
        assertThat(impression.getAccountId(), is(projectConfig.getAccountId()));

        assertThat(impression.getVisitors().get(0).getAttributes(), is(expectedUserFeatures));
        assertThat(impression.getClientName(), is(EventBatch.ClientEngine.JAVA_SDK.getClientEngineValue()));
        assertThat(impression.getClientVersion(), is(BuildVersionInfo.VERSION));
        assertNull(impression.getVisitors().get(0).getSessionId());
    }

    /**
     * Verify {@link EventBatch} event creation
     */
    @Test
    public void createConversionEventWithBucketingId() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        Attribute attribute = validProjectConfig.getAttributes().get(0);
        EventType eventType = validProjectConfig.getEventTypes().get(0);
        String userId = "userId";
        String bucketingId = "bucketingId";

        Map<String, String> attributeMap = new java.util.HashMap<String, String>();
        attributeMap.put(attribute.getKey(), AUDIENCE_GRYFFINDOR_VALUE);
        attributeMap.put(ControlAttribute.BUCKETING_ATTRIBUTE.toString(), bucketingId);

        Map<String, Object> eventTagMap = new HashMap<String, Object>();
        eventTagMap.put("boolean_param", false);
        eventTagMap.put("string_param", "123");

        LogEvent conversionEvent = createConversionEvent(
                validProjectConfig,
                userId,
                eventType.getId(),
                eventType.getKey(),
                attributeMap,
                eventTagMap);

        // verify that the request endpoint is correct
        assertThat(conversionEvent.getEndpointUrl(), is(EventFactory.EVENT_ENDPOINT));

        EventBatch conversion = gson.fromJson(conversionEvent.getBody(), EventBatch.class);

        // verify payload information
        assertThat(conversion.getVisitors().get(0).getVisitorId(), is(userId));
        assertThat((double) conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTimestamp(), closeTo((double) System.currentTimeMillis(), 1000.0));
        assertThat(conversion.getProjectId(), is(validProjectConfig.getProjectId()));
        assertThat(conversion.getAccountId(), is(validProjectConfig.getAccountId()));

        com.optimizely.ab.event.internal.payload.Attribute attribute1 = new com.optimizely.ab.event.internal.payload.Attribute.Builder()
            .setEntityId(attribute.getId()).setKey(attribute.getKey())
            .setType(com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE)
            .setValue(AUDIENCE_GRYFFINDOR_VALUE)
            .build();

        com.optimizely.ab.event.internal.payload.Attribute attribute2 = new com.optimizely.ab.event.internal.payload.Attribute.Builder()
            .setEntityId(ControlAttribute.BUCKETING_ATTRIBUTE.toString())
            .setKey(ControlAttribute.BUCKETING_ATTRIBUTE.toString())
            .setType(com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE)
            .setValue(bucketingId)
            .build();

        List<com.optimizely.ab.event.internal.payload.Attribute> expectedUserFeatures = new ArrayList<com.optimizely.ab.event.internal.payload.Attribute>();
        expectedUserFeatures.add(attribute1);
        expectedUserFeatures.add(attribute2);

        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString())) {
            expectedUserFeatures.add(getBotFilteringAttribute());
        }

        assertEquals(conversion.getVisitors().get(0).getAttributes(), expectedUserFeatures);
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getEntityId(), eventType.getId());
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getType(), eventType.getKey());
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getKey(), eventType.getKey());
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getRevenue(), null);
        assertEquals(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getQuantity(), null);
        assertTrue(conversion.getVisitors().get(0).getSnapshots().get(0).getEvents().get(0).getTags().equals(eventTagMap));
        assertEquals(conversion.getAnonymizeIp(), validProjectConfig.getAnonymizeIP());
        assertTrue(conversion.getEnrichDecisions());
        assertEquals(conversion.getClientName(), EventBatch.ClientEngine.JAVA_SDK.getClientEngineValue());
        assertEquals(conversion.getClientVersion(), BuildVersionInfo.VERSION);
    }


    private com.optimizely.ab.event.internal.payload.Attribute getBotFilteringAttribute() {
        return new com.optimizely.ab.event.internal.payload.Attribute.Builder()
            .setEntityId(ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString())
            .setKey(ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString())
            .setType(com.optimizely.ab.event.internal.payload.Attribute.CUSTOM_ATTRIBUTE_TYPE)
            .setValue(validProjectConfig.getBotFiltering())
            .build();
    }

    /**
     * Helper method for generating an impression based LogEvent.
     */
    public static LogEvent createImpressionEvent(ProjectConfig projectConfig,
                                                 Experiment activatedExperiment,
                                                 Variation variation,
                                                 String userId,
                                                 Map<String, ?> attributes) {

        UserEvent userEvent = UserEventFactory.createImpressionEvent(
            projectConfig,
            activatedExperiment,
            variation,
            userId,
            attributes);

        return EventFactory.createLogEvent(userEvent);
        
    }

    /**
     * Helper method for generating a conversion based LogEvent.
     */
    private static LogEvent createConversionEvent(ProjectConfig projectConfig,
                                                  String userId,
                                                  String eventId,
                                                  String eventName,
                                                  Map<String, ?> attributes,
                                                  Map<String, ?> eventTags) {

        UserEvent userEvent = UserEventFactory.createConversionEvent(
            projectConfig,
            userId,
            eventId,
            eventName,
            attributes,
            eventTags);
        
        return EventFactory.createLogEvent(userEvent);
    }
}

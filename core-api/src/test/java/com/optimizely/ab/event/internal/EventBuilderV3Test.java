/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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

import com.google.gson.Gson;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.config.*;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.Event.ClientEngine;
import com.optimizely.ab.event.internal.payload.batch.*;
import com.optimizely.ab.internal.ProjectValidationUtils;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test for {@link EventBuilderV3}
 */
public class EventBuilderV3Test {

    private Gson gson = new Gson();
    private EventBuilderV3 builder = new EventBuilderV3();

    /**
     * Verify {@link Batch} event creation
     */
    @Test
    public void createImpressionEvent() throws Exception {
        ProjectConfig projectConfig = ProjectConfigTestUtils.noAudienceProjectConfigV4();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = projectConfig.getAttributes().get(0);
        String userId = "userId";
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        Decision expectedDecision = new Decision(
                activatedExperiment.getLayerId(),
                activatedExperiment.getId(),
                false,
                bucketedVariation.getId());

        com.optimizely.ab.event.internal.payload.batch.Attribute batchAttr =
                new com.optimizely.ab.event.internal.payload.batch.Attribute(
                        attribute.getId(),
                        attribute.getKey(),
                        com.optimizely.ab.event.internal.payload.batch.Attribute.CUSTOM_ATTRIBUTE_FEATURE_TYPE,
                        "value");

        List<com.optimizely.ab.event.internal.payload.batch.Attribute> expectedAttributes = Collections.singletonList(batchAttr);

        LogEvent impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
                userId, attributeMap);

        // verify that request endpoint is correct
        assertThat(impressionEvent.getEndpointUrl(), is(EventBuilderV3.BATCH_ENDPOINT));
        assertThat(impressionEvent.getRequestMethod(), is(LogEvent.RequestMethod.POST));

        // verify payload information

        Batch impression = gson.fromJson(impressionEvent.getBody(), Batch.class);

        assertNotNull(impression.getVisitors());
        assertThat(impression.getVisitors(), hasSize(1));
        Visitor visitor = impression.getVisitors().get(0);
        assertNull(visitor.getSessionId());
        assertThat(visitor.getAttributes(), is(expectedAttributes));

        assertNotNull(visitor.getSnapshots());
        assertThat(visitor.getSnapshots(), hasSize(1));
        Snapshot snapshot = visitor.getSnapshots().get(0);

        assertNotNull(snapshot.getEvents());
        assertThat(snapshot.getEvents(), hasSize(1));
        com.optimizely.ab.event.internal.payload.batch.Event event = snapshot.getEvents().get(0);
        assertThat(event.getKey(), is(EventBuilderV3.KEY_CAMPAIGN_ACTIVATED));
        assertThat(event.getEntityId(), is(activatedExperiment.getLayerId()));
        assertThat((System.currentTimeMillis() - event.getTimestamp()), lessThan((2L * 1000)));

        assertNotNull(snapshot.getDecisions());
        assertThat(snapshot.getDecisions(), hasSize(1));
        com.optimizely.ab.event.internal.payload.batch.Decision decision = snapshot.getDecisions().get(0);
        assertThat(decision.getCampaignId(), is(activatedExperiment.getLayerId()));
        assertThat(decision.getExperimentId(), is(expectedDecision.getExperimentId()));
        assertThat(decision.getVariationId(), is((expectedDecision.getVariationId())));

        assertThat(impression.getAccountId(), is(projectConfig.getAccountId()));
        assertThat(impression.getAnonymizeIp(), is(projectConfig.getAnonymizeIP()));
        assertThat(impression.getClientName(), is(ClientEngine.JAVA_SDK.getClientEngineValue()));
        assertThat(impression.getClientVersion(), is(BuildVersionInfo.VERSION));
        assertThat(impression.getProjectId(), is(projectConfig.getProjectId()));
    }

    /**
     * Verify {@link Batch} conversion event creation
     */
    @Test
    public void createConversionEvent() throws Exception {
        ProjectConfig projectConfig = ProjectConfigTestUtils.noAudienceProjectConfigV4();
        Attribute attribute = projectConfig.getAttributes().get(0);
        EventType eventType = projectConfig.getEventTypes().get(0);
        String visitorId = "visitorId";

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);

        List<Experiment> allExperiments = projectConfig.getExperiments();
        List<String> experimentIds = projectConfig.getExperimentIdsForGoal(eventType.getKey());

        // Bucket to the first variation for all experiments. However, only a subset of the experiments will actually
        // call the bucket function.
        for (Experiment experiment : allExperiments) {
            when(mockBucketAlgorithm.bucket(experiment, visitorId))
                .thenReturn(experiment.getVariations().get(0));
        }

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        Map<String, Object> eventTagMap = new HashMap<String, Object>();
        eventTagMap.put("boolean_param", false);
        eventTagMap.put("string_param", "123");
        LogEvent conversionEvent = builder.createConversionEvent(projectConfig, mockBucketAlgorithm, visitorId,
                                                                 eventType.getId(), eventType.getKey(), attributeMap, eventTagMap);

        List<Decision> expectedDecisions = new ArrayList<Decision>();
        for (Experiment experiment : allExperiments) {
            if (experimentIds.contains(experiment.getId()) &&
                    ProjectValidationUtils.validatePreconditions(projectConfig, experiment, visitorId, attributeMap)) {
                verify(mockBucketAlgorithm).bucket(experiment, visitorId);
                Decision decision = new Decision(experiment.getLayerId());
                decision.setExperimentId(experiment.getId());
                decision.setVariationId(experiment.getVariations().get(0).getId());
                expectedDecisions.add(decision);
            } else {
                verify(mockBucketAlgorithm, never()).bucket(experiment, visitorId);
            }
        }

        com.optimizely.ab.event.internal.payload.batch.Attribute batchAttr =
                new com.optimizely.ab.event.internal.payload.batch.Attribute(
                        attribute.getId(),
                        attribute.getKey(),
                        com.optimizely.ab.event.internal.payload.batch.Attribute.CUSTOM_ATTRIBUTE_FEATURE_TYPE,
                        "value");

        List<com.optimizely.ab.event.internal.payload.batch.Attribute> expectedAttributes = Collections.singletonList(batchAttr);

        // verify that the request endpoint is correct
        assertThat(conversionEvent.getEndpointUrl(), is(EventBuilderV3.BATCH_ENDPOINT));

        Batch conversion = gson.fromJson(conversionEvent.getBody(), Batch.class);

        assertThat(conversion.getProjectId(), is(projectConfig.getProjectId()));
        assertThat(conversion.getClientName(), is(ClientEngine.JAVA_SDK.getClientEngineValue()));
        assertThat(conversion.getClientVersion(), is(BuildVersionInfo.VERSION));
        assertThat(conversion.getAnonymizeIp(), is(projectConfig.getAnonymizeIP()));

        assertThat(conversion.getVisitors(), hasSize(1));
        List<Visitor> visitors = conversion.getVisitors();
        Visitor visitor = visitors.get(0);
        assertThat(visitor.getVisitorId(), is(visitorId));
        assertThat(visitor.getAttributes(), is(expectedAttributes));
        assertThat(visitor.getSnapshots(), hasSize(1));

        List<Snapshot> snapshots = Collections.singletonList(visitor.getSnapshots().get(0));
        Snapshot snapshot = snapshots.get(0);
        assertThat(snapshot.getDecisions(), hasSize(experimentIds.size()));
        assertThat(snapshot.getEvents(), hasSize(1));

        List<Decision> decisions = snapshot.getDecisions();
        assertThat(decisions, is(expectedDecisions));

        List<Event> events = snapshot.getEvents();
        Event event = events.get(0);
        // TODO I Don't think there is a way around this is Java 1.6
        Map<String, Object> tags = (Map<String, Object>) event.getTags();
        assertThat(tags, is(eventTagMap));
        assertThat(event.getKey(), is(eventType.getKey()));
        assertThat(event.getEntityId(), is(eventType.getId()));
        assertThat((System.currentTimeMillis() - event.getTimestamp()), lessThan((2L * 1000)));
    }


}

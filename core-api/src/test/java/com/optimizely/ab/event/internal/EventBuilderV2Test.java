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
package com.optimizely.ab.event.internal;

import com.google.gson.Gson;

import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.ProjectConfigTestUtils;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.internal.payload.Conversion;
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.EventMetric;
import com.optimizely.ab.event.internal.payload.Feature;
import com.optimizely.ab.event.internal.payload.Impression;
import com.optimizely.ab.event.internal.payload.LayerState;
import com.optimizely.ab.internal.ProjectValidationUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link EventBuilderV2}
 */
public class EventBuilderV2Test {

    private Gson gson = new Gson();
    private EventBuilderV2 builder = new EventBuilderV2();

    /**
     * Verify {@link Impression} event creation
     */
    @Test
    public void createImpressionEvent() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV2();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);
        Attribute attribute = projectConfig.getAttributes().get(0);
        String userId = "userId";
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        Decision expectedDecision = new Decision(bucketedVariation.getId(), false, activatedExperiment.getId());
        Feature feature = new Feature(attribute.getId(), attribute.getKey(), Feature.CUSTOM_ATTRIBUTE_FEATURE_TYPE,
                                      "value", false);
        List<Feature> expectedUserFeatures = Collections.singletonList(feature);

        LogEvent impressionEvent = builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation,
                                                                 userId, attributeMap);

        // verify that request endpoint is correct
        assertThat(impressionEvent.getEndpointUrl(), is(EventBuilderV2.IMPRESSION_ENDPOINT));

        Impression impression = gson.fromJson(impressionEvent.getBody(), Impression.class);

        // verify payload information
        assertThat(impression.getVisitorId(), is(userId));
        assertThat((double)impression.getTimestamp(), closeTo((double)System.currentTimeMillis(), 60.0));
        assertFalse(impression.getIsGlobalHoldback());
        assertThat(impression.getProjectId(), is(projectConfig.getProjectId()));
        assertThat(impression.getDecision(), is(expectedDecision));
        assertThat(impression.getLayerId(), is(activatedExperiment.getLayerId()));
        assertThat(impression.getAccountId(), is(projectConfig.getAccountId()));
        assertThat(impression.getUserFeatures(), is(expectedUserFeatures));
    }

    /**
     * Verify that passing through an unknown attribute causes that attribute to be ignored, rather than
     * causing an exception to be thrown.
     */
    @Test
    public void createImpressionEventIgnoresUnknownAttributes() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV2();
        Experiment activatedExperiment = projectConfig.getExperiments().get(0);
        Variation bucketedVariation = activatedExperiment.getVariations().get(0);

        LogEvent impressionEvent =
                builder.createImpressionEvent(projectConfig, activatedExperiment, bucketedVariation, "userId",
                                              Collections.singletonMap("unknownAttribute", "blahValue"));

        Impression impression = gson.fromJson(impressionEvent.getBody(), Impression.class);

        // verify that no Feature is created for "unknownAtrribute" -> "blahValue"
        for (Feature feature : impression.getUserFeatures()) {
            assertNotEquals(feature.getName(), "unknownAttribute");
            assertNotEquals(feature.getValue(), "blahValue");
        }
    }

    /**
     * Verify {@link Conversion} event creation
     */
    @Test
    public void createConversionEvent() throws Exception {
        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV2();
        Attribute attribute = projectConfig.getAttributes().get(0);
        EventType eventType = projectConfig.getEventTypes().get(0);
        String userId = "userId";

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);

        List<Experiment> allExperiments = projectConfig.getExperiments();
        List<String> experimentIds = projectConfig.getExperimentIdsForGoal(eventType.getKey());

        // Bucket to the first variation for all experiments. However, only a subset of the experiments will actually
        // call the bucket function.
        for (Experiment experiment : allExperiments) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                .thenReturn(experiment.getVariations().get(0));
        }

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        LogEvent conversionEvent = builder.createConversionEvent(projectConfig, mockBucketAlgorithm, userId,
                                                                 eventType.getId(), eventType.getKey(), attributeMap);

        List<LayerState> expectedLayerStates = new ArrayList<LayerState>();

        for (Experiment experiment : allExperiments) {
            if (experimentIds.contains(experiment.getId()) &&
                    ProjectValidationUtils.validatePreconditions(projectConfig, experiment, userId, attributeMap)) {
                verify(mockBucketAlgorithm).bucket(experiment, userId);
                LayerState layerState = new LayerState(experiment.getLayerId(),
                        new Decision(experiment.getVariations().get(0).getId(), false, experiment.getId()), true);
                expectedLayerStates.add(layerState);
            } else {
                verify(mockBucketAlgorithm, never()).bucket(experiment, userId);
            }
        }

        // verify that the request endpoint is correct
        assertThat(conversionEvent.getEndpointUrl(), is(EventBuilderV2.CONVERSION_ENDPOINT));

        Conversion conversion = gson.fromJson(conversionEvent.getBody(), Conversion.class);

        // verify payload information
        assertThat(conversion.getVisitorId(), is(userId));
        assertThat((double)conversion.getTimestamp(), closeTo((double)System.currentTimeMillis(), 60.0));
        assertThat(conversion.getProjectId(), is(projectConfig.getProjectId()));
        assertThat(conversion.getAccountId(), is(projectConfig.getAccountId()));

        Feature feature = new Feature(attribute.getId(), attribute.getKey(), Feature.CUSTOM_ATTRIBUTE_FEATURE_TYPE,
                                      "value", false);
        List<Feature> expectedUserFeatures = Collections.singletonList(feature);
        assertThat(conversion.getUserFeatures(), is(expectedUserFeatures));
        assertThat(conversion.getLayerStates(), is(expectedLayerStates));
        assertThat(conversion.getEventEntityId(), is(eventType.getId()));
        assertThat(conversion.getEventName(), is(eventType.getKey()));
        assertThat(conversion.getEventMetrics(), is(Collections.<EventMetric>emptyList()));
        assertThat(conversion.getEventFeatures(), is(Collections.<Feature>emptyList()));
        assertFalse(conversion.getIsGlobalHoldback());
    }

    /**
     * Verify that eventValue is properly recorded in a conversion request as an {@link EventMetric}
     */
    @Test
    public void createConversionParamsWithRevenue() throws Exception {
        long revenue = 1234L;

        // use the "valid" project config and its associated experiment, variation, and attributes
        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV2();
        Attribute attribute = projectConfig.getAttributes().get(0);
        EventType eventType = projectConfig.getEventTypes().get(0);

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);

        // Bucket to the first variation for all experiments.
        for (Experiment experiment : projectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, "userId"))
                .thenReturn(experiment.getVariations().get(0));
        }

        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "value");
        LogEvent conversionEvent = builder.createConversionEvent(projectConfig, mockBucketAlgorithm, "userId",
                                                                 eventType.getId(), eventType.getKey(), attributeMap,
                                                                 revenue);

        Conversion conversion = gson.fromJson(conversionEvent.getBody(), Conversion.class);

        // we're not going to verify everything, only revenue
        assertThat(conversion.getEventMetrics(),
                   is(Collections.singletonList(new EventMetric(EventMetric.REVENUE_METRIC_TYPE, revenue))));
    }

    /**
     * Verify that a {@link LayerState} isn't created if a user doesn't satisfy audience conditions for an experiment.
     */
    @Test
    public void createConversionParamsUserNotInAudience() throws Exception {
        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV2();
        Attribute attribute = projectConfig.getAttributes().get(0);
        EventType eventType = projectConfig.getEventTypes().get(2);
        String userId = "userId";

        Bucketer mockBucketAlgorithm = mock(Bucketer.class);

        // Bucket to the first variation for all experiments.
        for (Experiment experiment : projectConfig.getExperiments()) {
            when(mockBucketAlgorithm.bucket(experiment, userId))
                .thenReturn(experiment.getVariations().get(0));
        }

        // the audience for the experiments is "NOT firefox" so this user shouldn't satisfy audience conditions
        Map<String, String> attributeMap = Collections.singletonMap(attribute.getKey(), "firefox");
        LogEvent conversionEvent = builder.createConversionEvent(projectConfig, mockBucketAlgorithm, userId,
                                                                 eventType.getId(), eventType.getKey(), attributeMap);

        assertNull(conversionEvent);
    }
}

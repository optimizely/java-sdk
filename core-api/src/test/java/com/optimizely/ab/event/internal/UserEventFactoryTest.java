/**
 *
 *    Copyright 2019-2020, Optimizely and contributors
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

import com.google.common.collect.ImmutableMap;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.internal.payload.DecisionMetadata;
import com.optimizely.ab.internal.ReservedEventKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class UserEventFactoryTest {

    private static final String USER_ID = "USER_ID";
    private static final Map<String, ?> ATTRIBUTES = Collections.singletonMap("KEY", "VALUE");

    private static final String EVENT_ID     = "layerId";
    private static final String EVENT_KEY    = "experimentKey";
    private static final Number REVENUE      = 100L;
    private static final Number VALUE        = 9.99;
    private static final Map<String, ?> TAGS = ImmutableMap.of(
        "KEY", "VALUE",
        ReservedEventKey.REVENUE.toString(), REVENUE,
        ReservedEventKey.VALUE.toString(), VALUE
    );

    private static final String LAYER_ID       = "layerId";
    private static final String EXPERIMENT_ID  = "experimentId";
    private static final String EXPERIMENT_KEY = "experimentKey";
    private static final String VARIATION_ID   = "variationId";
    private static final String VARIATION_KEY  = "variationKey";

    @Mock
    private ProjectConfig projectConfig;

    private Experiment experiment;
    private Variation variation;
    private DecisionMetadata decisionMetadata;

    @Before
    public void setUp() {
        experiment = new Experiment(EXPERIMENT_ID, EXPERIMENT_KEY, LAYER_ID);
        variation = new Variation(VARIATION_ID, VARIATION_KEY);
        decisionMetadata = new DecisionMetadata("", EXPERIMENT_KEY, "experiment", VARIATION_KEY, true, null);
    }

    @Test
    public void createImpressionEventNull() {

        ImpressionEvent actual = UserEventFactory.createImpressionEvent(
            projectConfig,
            experiment,
            null,
            USER_ID,
            ATTRIBUTES,
            EXPERIMENT_KEY,
            "rollout",
            false,
            null
        );
        assertNull(actual);
    }

    @Test
    public void createImpressionEvent() {
        ImpressionEvent actual = UserEventFactory.createImpressionEvent(
            projectConfig,
            experiment,
            variation,
            USER_ID,
            ATTRIBUTES,
            "",
            "experiment",
            true,
            null
        );

        assertTrue(actual.getTimestamp() > 0);
        assertNotNull(actual.getUUID());

        assertSame(projectConfig, actual.getUserContext().getProjectConfig());

        assertEquals(USER_ID, actual.getUserContext().getUserId());
        assertEquals(ATTRIBUTES, actual.getUserContext().getAttributes());

        assertEquals(LAYER_ID, actual.getLayerId());
        assertEquals(EXPERIMENT_ID, actual.getExperimentId());
        assertEquals(EXPERIMENT_KEY, actual.getExperimentKey());
        assertEquals(VARIATION_ID, actual.getVariationId());
        assertEquals(VARIATION_KEY, actual.getVariationKey());
        assertEquals(decisionMetadata, actual.getMetadata());
    }

    @Test
    public void createConversionEvent() {
        ConversionEvent actual = UserEventFactory.createConversionEvent(
            projectConfig,
            USER_ID,
            EVENT_ID,
            EVENT_KEY,
            ATTRIBUTES,
            TAGS
        );

        assertTrue(actual.getTimestamp() > 0);
        assertNotNull(actual.getUUID());

        assertSame(projectConfig, actual.getUserContext().getProjectConfig());

        assertEquals(USER_ID, actual.getUserContext().getUserId());
        assertEquals(ATTRIBUTES, actual.getUserContext().getAttributes());

        assertEquals(EVENT_ID, actual.getEventId());
        assertEquals(EVENT_KEY, actual.getEventKey());
        assertEquals(REVENUE, actual.getRevenue());
        assertEquals(VALUE, actual.getValue());
        assertEquals(TAGS, actual.getTags());
    }
    @Test
    public void createImpressionEventWithCmabUuid() {
        // Arrange
        String userId = "testUser";
        String flagKey = "testFlag";
        String ruleType = "experiment";
        boolean enabled = true;
        String cmabUUID = "test-cmab-uuid-123";
        Map<String, Object> attributes = Collections.emptyMap();
        
        // Create mock objects
        ProjectConfig mockProjectConfig = mock(ProjectConfig.class);
        Experiment mockExperiment = mock(Experiment.class);
        Variation mockVariation = mock(Variation.class);
        
        // Setup mock behavior
        when(mockProjectConfig.getSendFlagDecisions()).thenReturn(true);
        when(mockExperiment.getLayerId()).thenReturn("layer123");
        when(mockExperiment.getId()).thenReturn("experiment123");
        when(mockExperiment.getKey()).thenReturn("experimentKey");
        when(mockVariation.getKey()).thenReturn("variationKey");
        when(mockVariation.getId()).thenReturn("variation123");
        
        // Act
        ImpressionEvent result = UserEventFactory.createImpressionEvent(
            mockProjectConfig,
            mockExperiment,
            mockVariation,
            userId,
            attributes,
            flagKey,
            ruleType,
            enabled,
            cmabUUID
        );
        
        // Assert
        assertNotNull(result);
        
        // Verify DecisionMetadata contains cmabUUID
        DecisionMetadata metadata = result.getMetadata();
        assertNotNull(metadata);
        assertEquals(cmabUUID, metadata.getCmabUuid());
        assertEquals(flagKey, metadata.getFlagKey());
        assertEquals("experimentKey", metadata.getRuleKey());
        assertEquals(ruleType, metadata.getRuleType());
        assertEquals("variationKey", metadata.getVariationKey());
        assertEquals(enabled, metadata.getEnabled());
        
        // Verify other fields
        assertEquals("layer123", result.getLayerId());
        assertEquals("experiment123", result.getExperimentId());
        assertEquals("experimentKey", result.getExperimentKey());
        assertEquals("variation123", result.getVariationId());
        assertEquals("variationKey", result.getVariationKey());
    }

    @Test
    public void createImpressionEventWithNullCmabUuid() {
        // Arrange
        String userId = "testUser";
        String flagKey = "testFlag";
        String ruleType = "experiment";
        boolean enabled = true;
        String cmabUUID = null;
        Map<String, Object> attributes = Collections.emptyMap();
        
        // Create mock objects (same setup as above)
        ProjectConfig mockProjectConfig = mock(ProjectConfig.class);
        Experiment mockExperiment = mock(Experiment.class);
        Variation mockVariation = mock(Variation.class);
        
        when(mockProjectConfig.getSendFlagDecisions()).thenReturn(true);
        when(mockExperiment.getLayerId()).thenReturn("layer123");
        when(mockExperiment.getId()).thenReturn("experiment123");
        when(mockExperiment.getKey()).thenReturn("experimentKey");
        when(mockVariation.getKey()).thenReturn("variationKey");
        when(mockVariation.getId()).thenReturn("variation123");
        
        // Act
        ImpressionEvent result = UserEventFactory.createImpressionEvent(
            mockProjectConfig,
            mockExperiment,
            mockVariation,
            userId,
            attributes,
            flagKey,
            ruleType,
            enabled,
            cmabUUID
        );
        
        // Assert
        assertNotNull(result);
        DecisionMetadata metadata = result.getMetadata();
        assertNotNull(metadata);
        assertNull(metadata.getCmabUuid());
    }
}

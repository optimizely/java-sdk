/**
 *
 *    Copyright 2019, Optimizely and contributors
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
import com.optimizely.ab.internal.ReservedEventKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;


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

    @Before
    public void setUp() {
        experiment = new Experiment(EXPERIMENT_ID, EXPERIMENT_KEY, LAYER_ID);
        variation = new Variation(VARIATION_ID, VARIATION_KEY);
    }

    @Test
    public void createImpressionEvent() {
        ImpressionEvent actual = UserEventFactory.createImpressionEvent(
            projectConfig,
            experiment,
            variation,
            USER_ID,
            ATTRIBUTES
        );

        assertTrue(actual.getUserContext().getTimestamp() > 0);
        assertNotNull(actual.getUserContext().getUUID());

        assertSame(projectConfig, actual.getUserContext().getProjectConfig());

        assertEquals(USER_ID, actual.getUserContext().getUserId());
        assertEquals(ATTRIBUTES, actual.getUserContext().getAttributes());

        assertEquals(LAYER_ID, actual.getLayerId());
        assertEquals(EXPERIMENT_ID, actual.getExperimentId());
        assertEquals(EXPERIMENT_KEY, actual.getExperimentKey());
        assertEquals(VARIATION_ID, actual.getVariationId());
        assertEquals(VARIATION_KEY, actual.getVariationKey());
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

        assertTrue(actual.getUserContext().getTimestamp() > 0);
        assertNotNull(actual.getUserContext().getUUID());

        assertSame(projectConfig, actual.getUserContext().getProjectConfig());

        assertEquals(USER_ID, actual.getUserContext().getUserId());
        assertEquals(ATTRIBUTES, actual.getUserContext().getAttributes());

        assertEquals(EVENT_ID, actual.getEventId());
        assertEquals(EVENT_KEY, actual.getEventKey());
        assertEquals(REVENUE, actual.getRevenue());
        assertEquals(VALUE, actual.getValue());
        assertEquals(TAGS, actual.getTags());
    }
}

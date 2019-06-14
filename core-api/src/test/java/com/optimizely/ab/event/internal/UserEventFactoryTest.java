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
        UserEvent actual = UserEventFactory.createImpressionEvent(
            projectConfig,
            experiment,
            variation,
            USER_ID,
            ATTRIBUTES
        );

        assertTrue(actual.getTimestamp() > 0);
        assertNotNull(actual.getUUID());

        assertSame(projectConfig, actual.getProjectConfig());

        assertEquals(USER_ID, actual.getUserId());
        assertEquals(ATTRIBUTES, actual.getAttributes());

        assertEquals(LAYER_ID, actual.getImpressionEvent().getLayerId());
        assertEquals(EXPERIMENT_ID, actual.getImpressionEvent().getExperimentId());
        assertEquals(EXPERIMENT_KEY, actual.getImpressionEvent().getExperimentKey());
        assertEquals(VARIATION_ID, actual.getImpressionEvent().getVariationId());
        assertEquals(VARIATION_KEY, actual.getImpressionEvent().getVariationKey());

        assertNull(actual.getConversionEvent());
    }

    @Test
    public void createConversionEvent() {
        UserEvent actual = UserEventFactory.createConversionEvent(
            projectConfig,
            USER_ID,
            EVENT_ID,
            EVENT_KEY,
            ATTRIBUTES,
            TAGS
        );

        assertTrue(actual.getTimestamp() > 0);
        assertNotNull(actual.getUUID());

        assertSame(projectConfig, actual.getProjectConfig());

        assertEquals(USER_ID, actual.getUserId());
        assertEquals(ATTRIBUTES, actual.getAttributes());

        assertEquals(EVENT_ID, actual.getConversionEvent().getEventId());
        assertEquals(EVENT_KEY, actual.getConversionEvent().getEventKey());
        assertEquals(REVENUE, actual.getConversionEvent().getRevenue());
        assertEquals(VALUE, actual.getConversionEvent().getValue());
        assertEquals(TAGS, actual.getConversionEvent().getTags());

        assertNull(actual.getImpressionEvent());
    }
}

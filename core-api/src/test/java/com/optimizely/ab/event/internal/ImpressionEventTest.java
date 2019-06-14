package com.optimizely.ab.event.internal;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ImpressionEventTest {

    private static final String LAYER_ID       = "layerId";
    private static final String EXPERIMENT_ID  = "experimentId";
    private static final String EXPERIMENT_KEY = "experimentKey";
    private static final String VARIATION_ID   = "variationId";
    private static final String VARIATION_KEY  = "variationKey";

    private ImpressionEvent impressionEvent;

    @Before
    public void setUp() throws Exception {
        impressionEvent = new ImpressionEvent.Builder()
            .withLayerId(LAYER_ID)
            .withExperimentId(EXPERIMENT_ID)
            .withExperimentKey(EXPERIMENT_KEY)
            .withVariationId(VARIATION_ID)
            .withVariationKey(VARIATION_KEY)
            .build();
    }

    @Test
    public void getLayerId() {
        assertSame(LAYER_ID, impressionEvent.getLayerId());
    }

    @Test
    public void getExperimentId() {
        assertSame(EXPERIMENT_ID, impressionEvent.getExperimentId());
    }

    @Test
    public void getExperimentKey() {
        assertSame(EXPERIMENT_KEY, impressionEvent.getExperimentKey());
    }

    @Test
    public void getVariationId() {
        assertSame(VARIATION_ID, impressionEvent.getVariationId());
    }

    @Test
    public void getVariationKey() {
        assertSame(VARIATION_KEY, impressionEvent.getVariationKey());
    }
}

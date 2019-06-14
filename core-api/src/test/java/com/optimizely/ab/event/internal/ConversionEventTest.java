package com.optimizely.ab.event.internal;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class ConversionEventTest {

    private static final String EVENT_ID     = "layerId";
    private static final String EVENT_KEY    = "experimentKey";
    private static final Number REVENUE      = 100;
    private static final Number VALUE        = 9.99;
    private static final Map<String, ?> TAGS = Collections.singletonMap("KEY", "VALUE");

    private ConversionEvent conversionEvent;

    @Before
    public void setUp() throws Exception {
        conversionEvent = new ConversionEvent.Builder()
            .withEventId(EVENT_ID)
            .withEventKey(EVENT_KEY)
            .withRevenue(REVENUE)
            .withValue(VALUE)
            .withTags(TAGS)
            .build();
    }

    @Test
    public void getEventId() {
        assertSame(EVENT_ID, conversionEvent.getEventId());
    }

    @Test
    public void getEventKey() {
        assertSame(EVENT_KEY, conversionEvent.getEventKey());
    }

    @Test
    public void getRevenue() {
        assertSame(REVENUE, conversionEvent.getRevenue());
    }

    @Test
    public void getValue() {
        assertSame(VALUE, conversionEvent.getValue());
    }

    @Test
    public void getTags() {
        assertSame(TAGS, conversionEvent.getTags());
    }
}

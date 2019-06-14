package com.optimizely.ab.event.internal;

import com.optimizely.ab.config.ProjectConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class UserEventTest {

    private static final String UUID    = "UUID";
    private static final long TIMESTAMP = 100L;
    private static final String USER_ID = "USER_ID";
    private static final Map<String, ?> ATTRIBUTES = Collections.singletonMap("KEY", "VALUE");
    private static final ConversionEvent CONVERSION = new ConversionEvent();
    private static final ImpressionEvent IMPRESSION = new ImpressionEvent();
    private static final ProjectConfig PROJECT_CONFIG = mock(ProjectConfig.class);

    private UserEvent userEvent;

    @Before
    public void setUp() {
        userEvent = new UserEvent.Builder()
            .withUUID(UUID)
            .withTimestamp(TIMESTAMP)
            .withUserId(USER_ID)
            .withAttributes(ATTRIBUTES)
            .withConversionEvent(CONVERSION)
            .withImpressionEvent(IMPRESSION)
            .withProjectConfig(PROJECT_CONFIG)
            .build();
    }

    @Test
    public void getProjectConfig() {
        assertSame(PROJECT_CONFIG, userEvent.getProjectConfig());
    }

    @Test
    public void getUUID() {
        assertSame(UUID, userEvent.getUUID());
    }

    @Test
    public void getTimestamp() {
        assertSame(TIMESTAMP, userEvent.getTimestamp());
    }

    @Test
    public void getUserId() {
        assertSame(USER_ID, userEvent.getUserId());
    }

    @Test
    public void getAttributes() {
        assertSame(ATTRIBUTES, userEvent.getAttributes());
    }

    @Test
    public void getConversionEvent() {
        assertSame(CONVERSION, userEvent.getConversionEvent());
    }

    @Test
    public void getImpressionEvent() {
        assertSame(IMPRESSION, userEvent.getImpressionEvent());
    }
}

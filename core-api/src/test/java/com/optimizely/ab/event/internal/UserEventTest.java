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

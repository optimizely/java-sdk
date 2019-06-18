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

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class ConversionEventTest {

    private static final UserContext USER_CONTEXT = mock(UserContext.class);
    private static final String EVENT_ID     = "layerId";
    private static final String EVENT_KEY    = "experimentKey";
    private static final Number REVENUE      = 100;
    private static final Number VALUE        = 9.99;
    private static final Map<String, ?> TAGS = Collections.singletonMap("KEY", "VALUE");

    private ConversionEvent conversionEvent;

    @Before
    public void setUp() throws Exception {
        conversionEvent = new ConversionEvent.Builder()
            .withUserContext(USER_CONTEXT)
            .withEventId(EVENT_ID)
            .withEventKey(EVENT_KEY)
            .withRevenue(REVENUE)
            .withValue(VALUE)
            .withTags(TAGS)
            .build();
    }

    @Test
    public void getUserContext() {
        assertSame(USER_CONTEXT, conversionEvent.getUserContext());
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

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
package com.optimizely.ab.notification;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class ActivateNotificationTest {

    private static final Experiment EXPERIMENT = mock(Experiment.class);
    private static final Variation VARIATION = mock(Variation.class);
    private static final String USER_ID = "userID";
    private static final Map<String, String> USER_ATTRIBUTES = Collections.singletonMap("user", "attr");
    private static final LogEvent LOG_EVENT = new LogEvent(
        LogEvent.RequestMethod.POST,
        "endpoint",
        Collections.emptyMap(),
        null
    );

    private ActivateNotification activateNotification;

    @Before
    public void setUp() throws Exception {
        activateNotification = new ActivateNotification(EXPERIMENT, USER_ID, USER_ATTRIBUTES, VARIATION, LOG_EVENT);
    }

    @Test
    public void testGetExperiment() {
        assertEquals(EXPERIMENT, activateNotification.getExperiment());
    }

    @Test
    public void testGetUserId() {
        assertEquals(USER_ID, activateNotification.getUserId());
    }

    @Test
    public void testGetAttributes() {
        assertEquals(USER_ATTRIBUTES, activateNotification.getAttributes());
    }

    @Test
    public void testGetVariation() {
        assertEquals(VARIATION, activateNotification.getVariation());
    }

    @Test
    public void testGetEvent() {
        assertEquals(LOG_EVENT, activateNotification.getEvent());
    }
}

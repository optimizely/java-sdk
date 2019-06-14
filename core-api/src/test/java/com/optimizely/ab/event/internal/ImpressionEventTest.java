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

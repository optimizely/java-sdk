/**
 *
 *    Copyright 2025, Optimizely and contributors
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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for EventEndpoints class to test event endpoints
 */
public class EventEndpointsTest {

    @Test
    public void testGetEndpointForUSRegion() {
        String endpoint = EventEndpoints.getEndpointForRegion("US");
        assertEquals("https://logx.optimizely.com/v1/events", endpoint);
    }

    @Test
    public void testGetEndpointForEURegion() {
        String endpoint = EventEndpoints.getEndpointForRegion("EU");
        assertEquals("https://eu.logx.optimizely.com/v1/events", endpoint);
    }

    @Test
    public void testGetDefaultEndpoint() {
        String defaultEndpoint = EventEndpoints.getEndpointForRegion("US");
        assertEquals("https://logx.optimizely.com/v1/events", defaultEndpoint);
    }

    @Test
    public void testGetEndpointForNullRegion() {
        String endpoint = EventEndpoints.getEndpointForRegion(null);
        assertEquals("https://logx.optimizely.com/v1/events", endpoint);
    }

    @Test
    public void testGetEndpointForInvalidRegion() {
        String endpoint = EventEndpoints.getEndpointForRegion("ZZ");
        assertEquals("https://logx.optimizely.com/v1/events", endpoint);
    }

    @Test
    public void testDefaultBehaviorAlwaysReturnsUS() {
        // Test that both null region and default endpoint return the same US endpoint
        String nullRegionEndpoint = EventEndpoints.getEndpointForRegion(null);
        String defaultEndpoint = EventEndpoints.getEndpointForRegion("US");
        String usEndpoint = EventEndpoints.getEndpointForRegion("US");
        
        assertEquals("All should return US endpoint", usEndpoint, nullRegionEndpoint);
        assertEquals("All should return US endpoint", usEndpoint, defaultEndpoint);
        assertEquals("Should be US endpoint", "https://logx.optimizely.com/v1/events", nullRegionEndpoint);
    }
}

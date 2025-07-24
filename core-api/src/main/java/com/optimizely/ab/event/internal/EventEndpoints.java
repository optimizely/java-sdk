/**
 *
 *    Copyright 2016-2020, 2022, Optimizely and contributors
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

import com.optimizely.ab.config.ProjectConfig.Region;
import java.util.HashMap;
import java.util.Map;

/**
 * EventEndpoints provides region-specific endpoint URLs for Optimizely events.
 * Similar to the TypeScript logxEndpoint configuration.
 */
public class EventEndpoints {
    
    private static final Map<Region, String> LOGX_ENDPOINTS = new HashMap<>();
    
    static {
        LOGX_ENDPOINTS.put(Region.US, "https://logx.optimizely.com/v1/events");
        LOGX_ENDPOINTS.put(Region.EU, "https://eu.logx.optimizely.com/v1/events");
    }
    
    /**
     * Get the event endpoint URL for the specified region.
     * Defaults to US region endpoint if region is null.
     *
     * @param region the region for which to get the endpoint
     * @return the endpoint URL for the specified region, or US endpoint if region is null
     */
    public static String getEndpointForRegion(Region region) {
        if (region == null) {
            return LOGX_ENDPOINTS.get(Region.US);
        }
        return LOGX_ENDPOINTS.get(region);
    }
    
    /**
     * Get the default event endpoint URL (US region).
     *
     * @return the default endpoint URL
     */
    public static String getDefaultEndpoint() {
        return LOGX_ENDPOINTS.get(Region.US);
    }
}

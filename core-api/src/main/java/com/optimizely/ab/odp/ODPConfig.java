/**
 *
 *    Copyright 2022, Optimizely
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
package com.optimizely.ab.odp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ODPConfig {

    private String apiKey;

    private String apiHost;

    private List<String> allSegments;

    public ODPConfig(String apiKey, String apiHost, List<String> allSegments) {
        this.apiKey = apiKey;
        this.apiHost = apiHost;
        this.allSegments = allSegments;
    }

    public ODPConfig(String apiKey, String apiHost) {
        this(apiKey, apiHost, Collections.emptyList());
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setApiHost(String apiHost) {
        this.apiHost = apiHost;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiHost() {
        return apiHost;
    }

    public List<String> getAllSegments() {
        return allSegments;
    }

    public void setAllSegments(List<String> allSegments) {
        this.allSegments = allSegments;
    }
}

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

import java.util.Collections;
import java.util.Set;

public class ODPConfig {

    private String apiKey;

    private String apiHost;

    private Set<String> allSegments;

    public ODPConfig(String apiKey, String apiHost, Set<String> allSegments) {
        this.apiKey = apiKey;
        this.apiHost = apiHost;
        this.allSegments = allSegments;
    }

    public ODPConfig(String apiKey, String apiHost) {
        this(apiKey, apiHost, Collections.emptySet());
    }

    public synchronized Boolean isReady() {
        return !(
            this.apiKey == null || this.apiKey.isEmpty()
            || this.apiHost == null || this.apiHost.isEmpty()
        );
    }

    public synchronized Boolean hasSegments() {
        return allSegments != null && !allSegments.isEmpty();
    }

    public synchronized void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public synchronized void setApiHost(String apiHost) {
        this.apiHost = apiHost;
    }

    public synchronized String getApiKey() {
        return apiKey;
    }

    public synchronized String getApiHost() {
        return apiHost;
    }

    public synchronized Set<String> getAllSegments() {
        return allSegments;
    }

    public synchronized void setAllSegments(Set<String> allSegments) {
        this.allSegments = allSegments;
    }

    public Boolean equals(ODPConfig toCompare) {
        return getApiHost().equals(toCompare.getApiHost()) && getApiKey().equals(toCompare.getApiKey()) && getAllSegments().equals(toCompare.allSegments);
    }

    public synchronized ODPConfig getClone() {
        return new ODPConfig(apiKey, apiHost, allSegments);
    }
}

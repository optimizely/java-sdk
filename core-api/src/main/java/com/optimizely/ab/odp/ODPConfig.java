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
import java.util.concurrent.locks.ReentrantLock;

public class ODPConfig {

    private String apiKey;

    private String apiHost;

    private Set<String> allSegments;

    private final ReentrantLock lock = new ReentrantLock();

    public ODPConfig(String apiKey, String apiHost, Set<String> allSegments) {
        this.apiKey = apiKey;
        this.apiHost = apiHost;
        this.allSegments = allSegments;
    }

    public ODPConfig(String apiKey, String apiHost) {
        this(apiKey, apiHost, Collections.emptySet());
    }

    public Boolean isReady() {
        lock.lock();
        try {
            return !(
                this.apiKey == null || this.apiKey.isEmpty()
                    || this.apiHost == null || this.apiHost.isEmpty()
            );
        } finally {
            lock.unlock();
        }
    }

    public Boolean hasSegments() {
        lock.lock();
        try {
            return allSegments != null && !allSegments.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    public void setApiKey(String apiKey) {
        lock.lock();
        try {
            this.apiKey = apiKey;
        } finally {
            lock.unlock();
        }
    }

    public void setApiHost(String apiHost) {
        lock.lock();
        try {
            this.apiHost = apiHost;
        } finally {
            lock.unlock();
        }
    }

    public String getApiKey() {
        lock.lock();
        try {
            return apiKey;
        } finally {
            lock.unlock();
        }
    }

    public String getApiHost() {
        lock.lock();
        try {
            return apiHost;
        } finally {
            lock.unlock();
        }
    }

    public Set<String> getAllSegments() {
        lock.lock();
        try {
            return allSegments;
        } finally {
            lock.unlock();
        }
    }

    public void setAllSegments(Set<String> allSegments) {
        lock.lock();
        try {
            this.allSegments = allSegments;
        } finally {
            lock.unlock();
        }
    }

    public Boolean equals(ODPConfig toCompare) {
        return getApiHost().equals(toCompare.getApiHost()) && getApiKey().equals(toCompare.getApiKey()) && getAllSegments().equals(toCompare.allSegments);
    }

    public ODPConfig getClone() {
        lock.lock();
        try {
            return new ODPConfig(apiKey, apiHost, allSegments);
        } finally {
            lock.unlock();
        }
    }
}

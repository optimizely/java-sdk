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
package com.optimizely.ab.internal;

import com.optimizely.ab.annotations.VisibleForTesting;

import java.util.*;

public class DefaultLRUCache<T> implements Cache<T> {

    private final Object lock = new Object();

    private final Integer maxSize;

    private final Long timeoutMillis;
    
    @VisibleForTesting
    final LinkedHashMap<String, CacheEntity> linkedHashMap = new LinkedHashMap<String, CacheEntity>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntity> eldest) {
            return this.size() > maxSize;
        }
    };

    public DefaultLRUCache() {
        this(DEFAULT_MAX_SIZE, DEFAULT_TIMEOUT_SECONDS);
    }

    public DefaultLRUCache(Integer maxSize, Integer timeoutSeconds) {
        this.maxSize = maxSize < 0 ? 0 : maxSize;
        this.timeoutMillis =  (timeoutSeconds < 0) ? 0 : (timeoutSeconds * 1000L);
    }

    public void save(String key, T value) {
        if (maxSize == 0) {
            // Cache is disabled when maxSize = 0
            return;
        }

        synchronized (lock) {
            linkedHashMap.put(key, new CacheEntity(value));
        }
    }

    public T lookup(String key) {
        if (maxSize == 0) {
            // Cache is disabled when maxSize = 0
            return null;
        }

        synchronized (lock) {
            if (linkedHashMap.containsKey(key)) {
                CacheEntity entity = linkedHashMap.get(key);
                Long nowMs = new Date().getTime();

                // ttl = 0 means entities never expire.
                if (timeoutMillis == 0 || (nowMs - entity.timestamp < timeoutMillis)) {
                    return entity.value;
                }

                linkedHashMap.remove(key);
            }
            return null;
        }
    }

    public void reset() {
        synchronized (lock) {
            linkedHashMap.clear();
        }
    }

    private class CacheEntity {
        public T value;
        public Long timestamp;

        public CacheEntity(T value) {
            this.value = value;
            this.timestamp = new Date().getTime();
        }
    }
}

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
import com.optimizely.ab.config.parser.DefaultConfigParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DefaultLRUCache<T> implements Cache<T> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultLRUCache.class);

    private final Object lock = new Object();

    private Integer maxSize;

    private Long timeoutMillis;
    @VisibleForTesting
    final LinkedHashMap<String, ItemWrapper> linkedHashMap = new LinkedHashMap<String, ItemWrapper>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ItemWrapper> eldest) {
            return this.size() > maxSize;
        }
    };

    public DefaultLRUCache() {
        this.maxSize = DEFAULT_MAX_SIZE;
        this.timeoutMillis = (long) (DEFAULT_TIMEOUT_SECONDS * 1000);
    }

    public void setMaxSize(Integer size) {
        if (linkedHashMap.size() > 0) {
            if (size >= linkedHashMap.size()) {
                maxSize = size;
            } else {
                logger.warn("Cannot set max cache size less than current size.");
            }
        } else {
            Integer sizeToSet = size;
            if (size < 0) {
                sizeToSet = 0;
            }
            maxSize = sizeToSet;
        }
    }

    public void setTimeout(Long timeoutSeconds) {
        this.timeoutMillis = timeoutSeconds * 1000;
    }

    public void save(String key, T value) {
        if (maxSize == 0) {
            // Cache is disabled when maxSize = 0
            return;
        }

        synchronized (lock) {
            linkedHashMap.put(key, new ItemWrapper(value));
        }
    }

    public T lookup(String key) {
        if (maxSize == 0) {
            // Cache is disabled when maxSize = 0
            return null;
        }

        synchronized (lock) {
            if (linkedHashMap.containsKey(key)) {
                ItemWrapper item = linkedHashMap.get(key);
                Long nowMs = new Date().getTime();

                // ttl = 0 means items never expire.
                if (timeoutMillis == 0 || (nowMs - item.timestamp < timeoutMillis)) {
                    return item.value;
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

    private class ItemWrapper {
        public T value;
        public Long timestamp;

        public ItemWrapper(T value) {
            this.value = value;
            this.timestamp = new Date().getTime();
        }
    }
}

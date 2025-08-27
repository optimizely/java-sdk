/**
 * Copyright 2025, Optimizely
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.cmab.service;

import org.slf4j.Logger;

import com.optimizely.ab.cmab.client.CmabClient;
import com.optimizely.ab.internal.DefaultLRUCache;

public class CmabServiceOptions {
    private final Logger logger;
    private final DefaultLRUCache<CmabCacheValue> cmabCache;
    private final CmabClient cmabClient;

    public CmabServiceOptions(DefaultLRUCache<CmabCacheValue> cmabCache, CmabClient cmabClient) {
        this(null, cmabCache, cmabClient);
    }

    public CmabServiceOptions(Logger logger, DefaultLRUCache<CmabCacheValue> cmabCache, CmabClient cmabClient) {
        this.logger = logger;
        this.cmabCache = cmabCache;
        this.cmabClient = cmabClient;
    }

    public Logger getLogger() {
        return logger;
    }

    public DefaultLRUCache<CmabCacheValue> getCmabCache() {
        return cmabCache;
    }

    public CmabClient getCmabClient() {
        return cmabClient;
    }
}
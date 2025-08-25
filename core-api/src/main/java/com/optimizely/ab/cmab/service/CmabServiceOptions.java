package com.optimizely.ab.cmab.service;

import com.optimizely.ab.cmab.client.CmabClient;
import com.optimizely.ab.internal.DefaultLRUCache;
import org.slf4j.Logger;

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
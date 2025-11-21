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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.bucketing.internal.MurmurHash3;
import com.optimizely.ab.cmab.client.CmabClient;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.internal.Cache;
import com.optimizely.ab.internal.DefaultLRUCache;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;

public class DefaultCmabService implements CmabService {
    public static final int DEFAULT_CMAB_CACHE_SIZE = 10000;
    public static final int DEFAULT_CMAB_CACHE_TIMEOUT_SECS = 30*60; // 30 minutes
    private static final int NUM_LOCK_STRIPES = 1000;
    
    private final Cache<CmabCacheValue> cmabCache;
    private final CmabClient cmabClient;
    private final Logger logger;
    private final ReentrantLock[] locks;

    public DefaultCmabService(CmabClient cmabClient, Cache<CmabCacheValue> cmabCache) {
        this(cmabClient, cmabCache, null);
    }

    public DefaultCmabService(CmabClient cmabClient, Cache<CmabCacheValue> cmabCache, Logger logger) {
        this.cmabCache = cmabCache;
        this.cmabClient = cmabClient;
        this.logger = logger != null ? logger : LoggerFactory.getLogger(DefaultCmabService.class);
        this.locks = new ReentrantLock[NUM_LOCK_STRIPES];
        for (int i = 0; i < NUM_LOCK_STRIPES; i++) {
            this.locks[i] = new ReentrantLock();
        }
    }

    @Override
    public CmabDecision getDecision(ProjectConfig projectConfig, OptimizelyUserContext userContext, String ruleId, List<OptimizelyDecideOption> options) {
        options = options == null ? Collections.emptyList() : options;
        String userId = userContext.getUserId();

        int lockIndex = getLockIndex(userId, ruleId);
        ReentrantLock lock = locks[lockIndex];
        lock.lock();
        try {
            Map<String, Object> filteredAttributes = filterAttributes(projectConfig, userContext, ruleId);

            if (options.contains(OptimizelyDecideOption.IGNORE_CMAB_CACHE)) {
                logger.debug("Ignoring CMAB cache for user '{}' and rule '{}'", userId, ruleId);
                return fetchDecision(ruleId, userId, filteredAttributes);
            }

            if (options.contains(OptimizelyDecideOption.RESET_CMAB_CACHE)) {
                logger.debug("Resetting CMAB cache for user '{}' and rule '{}'", userId, ruleId);
                cmabCache.reset();
            }

            String cacheKey = getCacheKey(userContext.getUserId(), ruleId);
            if (options.contains(OptimizelyDecideOption.INVALIDATE_USER_CMAB_CACHE)) {
                logger.debug("Invalidating CMAB cache for user '{}' and rule '{}'", userId, ruleId);
                cmabCache.remove(cacheKey);
            }

            CmabCacheValue cachedValue = cmabCache.lookup(cacheKey);

            String attributesHash = hashAttributes(filteredAttributes);

            if (cachedValue != null) {
                if (cachedValue.getAttributesHash().equals(attributesHash)) {
                    logger.debug("CMAB cache hit for user '{}' and rule '{}'", userId, ruleId);
                    return new CmabDecision(cachedValue.getVariationId(), cachedValue.getCmabUuid());
                } else {
                    logger.debug("CMAB cache attributes mismatch for user '{}' and rule '{}', fetching new decision", userId, ruleId);
                    cmabCache.remove(cacheKey);
                }
            } else {
                logger.debug("CMAB cache miss for user '{}' and rule '{}'", userId, ruleId);
            }

            CmabDecision cmabDecision = fetchDecision(ruleId, userId, filteredAttributes);
            logger.debug("CMAB decision is {}", cmabDecision);

            cmabCache.save(cacheKey, new CmabCacheValue(attributesHash, cmabDecision.getVariationId(), cmabDecision.getCmabUUID()));

            return cmabDecision;
        } finally {
            lock.unlock();
        }
    }

    private CmabDecision fetchDecision(String ruleId, String userId, Map<String, Object> attributes) {
        String cmabUuid = java.util.UUID.randomUUID().toString();
        String variationId = cmabClient.fetchDecision(ruleId, userId, attributes, cmabUuid);
        return new CmabDecision(variationId, cmabUuid);
    }

    private Map<String, Object> filterAttributes(ProjectConfig projectConfig, OptimizelyUserContext userContext, String ruleId) {
        Map<String, Object> userAttributes = userContext.getAttributes();
        Map<String, Object> filteredAttributes = new HashMap<>();

        // Get experiment by rule ID
        Experiment experiment = projectConfig.getExperimentIdMapping().get(ruleId);
        if (experiment == null) {
            logger.debug("Experiment not found for rule ID: {}", ruleId);
            return filteredAttributes;
        }

        // Check if experiment has CMAB configuration
        if (experiment.getCmab() == null) {
            logger.debug("No CMAB configuration found for experiment: {}", ruleId);
            return filteredAttributes;
        }

        List<String> cmabAttributeIds = experiment.getCmab().getAttributeIds();
        if (cmabAttributeIds == null || cmabAttributeIds.isEmpty()) {
            return filteredAttributes;
        }

        Map<String, Attribute> attributeIdMapping = projectConfig.getAttributeIdMapping();
        if (attributeIdMapping == null) {
            logger.debug("No attribute mapping found in project config for rule ID: {}", ruleId);
            return filteredAttributes;
        }

        // Filter attributes based on CMAB configuration
        for (String attributeId : cmabAttributeIds) {
            Attribute attribute = attributeIdMapping.get(attributeId);
            if (attribute != null) {
                if (userAttributes.containsKey(attribute.getKey())) {
                    filteredAttributes.put(attribute.getKey(), userAttributes.get(attribute.getKey()));
                } else {
                    logger.debug("User attribute '{}' not found for attribute ID '{}'", attribute.getKey(), attributeId);
                }
            } else {
                logger.debug("Attribute configuration not found for ID: {}", attributeId);
            }
        }

        return filteredAttributes;
    }

    private String getCacheKey(String userId, String ruleId) {
        return userId.length() + "-" + userId + "-" + ruleId;
    }

    private String hashAttributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "empty";
        }
        
        // Sort attributes to ensure consistent hashing
        TreeMap<String, Object> sortedAttributes = new TreeMap<>(attributes);

        // Create a simple string representation
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : sortedAttributes.entrySet()) {
            if (entry.getKey() == null) continue; // Skip null keys
            
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value.toString());
            }
            first = false;
        }
        sb.append("}");

        String attributesString = sb.toString();
        int hash = MurmurHash3.murmurhash3_x86_32(attributesString, 0, attributesString.length(), 0);
        
        // Convert to hex string to match your existing pattern
        return Integer.toHexString(hash);
    }

    private int getLockIndex(String userId, String ruleId) {
        // Create a hash of userId + ruleId for consistent lock selection
        String combined = userId + ruleId;
        int hash = MurmurHash3.murmurhash3_x86_32(combined, 0, combined.length(), 0);
        return Math.abs(hash) % NUM_LOCK_STRIPES;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int cmabCacheSize = DEFAULT_CMAB_CACHE_SIZE;
        private int cmabCacheTimeoutInSecs = DEFAULT_CMAB_CACHE_TIMEOUT_SECS;
        private Cache<CmabCacheValue> customCache;
        private CmabClient client;

        /**
         * Set the maximum size of the CMAB cache.
         * 
         * Default value is 10000 entries.
         *
         * @param cacheSize The maximum number of entries to store in the cache
         * @return Builder instance
         */
        public Builder withCmabCacheSize(int cacheSize) {
            this.cmabCacheSize = cacheSize;
            return this;
        }
        
        /**
         * Set the timeout duration for cached CMAB decisions.
         * 
         * Default value is 30 * 60 seconds (30 minutes).
         *
         * @param timeoutInSecs The timeout in seconds before cached entries expire
         * @return Builder instance
         */
        public Builder withCmabCacheTimeoutInSecs(int timeoutInSecs) {
            this.cmabCacheTimeoutInSecs = timeoutInSecs;
            return this;
        }

        /**
         * Provide a custom {@link CmabClient} instance which makes HTTP calls to fetch CMAB decisions.
         *
         * A Default CmabClient implementation is required for CMAB functionality.
         *
         * @param client The implementation of {@link CmabClient}
         * @return Builder instance
         */
        public Builder withClient(CmabClient client) {
            this.client = client;
            return this;
        }

        /**
         * Provide a custom {@link Cache} instance for caching CMAB decisions.
         *
         * If provided, this will override the cache size and timeout settings.
         *
         * @param cache The custom cache instance implementing {@link Cache}
         * @return Builder instance
         */
        public Builder withCustomCache(Cache<CmabCacheValue> cache) {
            this.customCache = cache;
            return this;
        }

        public DefaultCmabService build() {
            if (client == null) {
                throw new IllegalStateException("CmabClient is required");
            }

            Cache<CmabCacheValue> cache = customCache != null ? customCache : 
                new DefaultLRUCache<>(cmabCacheSize, cmabCacheTimeoutInSecs);

            return new DefaultCmabService(client, cache);
        }
    }
}

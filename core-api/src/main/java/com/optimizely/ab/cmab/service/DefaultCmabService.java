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
    public static final int DEFAULT_CMAB_CACHE_SIZE = 1000;
    public static final int DEFAULT_CMAB_CACHE_TIMEOUT_SECS = 300; // 5 minutes
    
    private final Cache<CmabCacheValue> cmabCache;
    private final CmabClient cmabClient;
    private final Logger logger;

    // public DefaultCmabService(CmabClient cmabClient, DefaultLRUCache<CmabCacheValue> cmabCache, Logger logger) {
    //     this.cmabCache = cmabCache;
    //     this.cmabClient = cmabClient;
    //     this.logger = logger;
    // }

    public DefaultCmabService(CmabClient cmabClient, Cache<CmabCacheValue> cmabCache, Logger logger) {
        this.cmabCache = cmabCache;
        this.cmabClient = cmabClient;
        this.logger = logger;
    }

    @Override
    public CmabDecision getDecision(ProjectConfig projectConfig, OptimizelyUserContext userContext, String ruleId, List<OptimizelyDecideOption> options) {
        options = options == null ? Collections.emptyList() : options;
        String userId = userContext.getUserId();
        Map<String, Object> filteredAttributes = filterAttributes(projectConfig, userContext, ruleId);

        if (options.contains(OptimizelyDecideOption.IGNORE_CMAB_CACHE)) {
            return fetchDecision(ruleId, userId, filteredAttributes);
        }

        if (options.contains(OptimizelyDecideOption.RESET_CMAB_CACHE)) {
            cmabCache.reset();
        }

        String cacheKey = getCacheKey(userContext.getUserId(), ruleId);
        if (options.contains(OptimizelyDecideOption.INVALIDATE_USER_CMAB_CACHE)) {
            cmabCache.remove(cacheKey);
        }

        CmabCacheValue cachedValue = cmabCache.lookup(cacheKey);

        String attributesHash = hashAttributes(filteredAttributes);

        if (cachedValue != null) {
            if (cachedValue.getAttributesHash().equals(attributesHash)) {
                return new CmabDecision(cachedValue.getVariationId(), cachedValue.getCmabUuid());
            } else {
                cmabCache.remove(cacheKey);
            }
        }

        CmabDecision cmabDecision = fetchDecision(ruleId, userId, filteredAttributes);
        cmabCache.save(cacheKey, new CmabCacheValue(attributesHash, cmabDecision.getVariationId(), cmabDecision.getCmabUUID()));

        return cmabDecision;
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
            if (logger != null) {
                logger.debug("Experiment not found for rule ID: {}", ruleId);
            }
            return filteredAttributes;
        }

        // Check if experiment has CMAB configuration
        // Add null check for getCmab()
        if (experiment.getCmab() == null) {
            if (logger != null) {
                logger.debug("No CMAB configuration found for experiment: {}", ruleId);
            }
            return filteredAttributes;
        }

        List<String> cmabAttributeIds = experiment.getCmab().getAttributeIds();
        if (cmabAttributeIds == null || cmabAttributeIds.isEmpty()) {
            return filteredAttributes;
        }

        Map<String, Attribute> attributeIdMapping = projectConfig.getAttributeIdMapping();
        // Add null check for attributeIdMapping
        if (attributeIdMapping == null) {
            if (logger != null) {
                logger.debug("No attribute mapping found in project config for rule ID: {}", ruleId);
            }
            return filteredAttributes;
        }

        // Filter attributes based on CMAB configuration
        for (String attributeId : cmabAttributeIds) {
            Attribute attribute = attributeIdMapping.get(attributeId);
            if (attribute != null) {
                if (userAttributes.containsKey(attribute.getKey())) {
                    filteredAttributes.put(attribute.getKey(), userAttributes.get(attribute.getKey()));
                } else if (logger != null) {
                    logger.debug("User attribute '{}' not found for attribute ID '{}'", attribute.getKey(), attributeId);
                }
            } else if (logger != null) {
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int cmabCacheSize = DEFAULT_CMAB_CACHE_SIZE;
        private int cmabCacheTimeoutInSecs = DEFAULT_CMAB_CACHE_TIMEOUT_SECS;
        private Cache<CmabCacheValue> customCache;
        private CmabClient client;
        private Logger logger;

        /**
         * Set the maximum size of the CMAB cache.
         * 
         * Default value is 1000 entries.
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
         * Default value is 300 seconds (5 minutes).
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

        /**
         * Provide a custom {@link Logger} instance for logging CMAB service operations.
         *
         * If not provided, a default SLF4J logger will be used.
         *
         * @param logger The logger instance
         * @return Builder instance
         */
        public Builder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public DefaultCmabService build() {
            if (client == null) {
                throw new IllegalStateException("CmabClient is required");
            }

            if (logger == null) {
                logger = LoggerFactory.getLogger(DefaultCmabService.class);
            }

            Cache<CmabCacheValue> cache = customCache != null ? customCache : 
                new DefaultLRUCache<>(cmabCacheSize, cmabCacheTimeoutInSecs);


            return new DefaultCmabService(client, cache, logger);
        }
    }
}

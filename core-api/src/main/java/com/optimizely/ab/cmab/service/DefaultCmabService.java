package com.optimizely.ab.cmab.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;

import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.bucketing.internal.MurmurHash3;
import com.optimizely.ab.cmab.client.CmabClient;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.internal.DefaultLRUCache;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;

public class DefaultCmabService implements CmabService {

    private final DefaultLRUCache<CmabCacheValue> cmabCache;
    private final CmabClient cmabClient;
    private final Logger logger;

    public DefaultCmabService(CmabServiceOptions options) {
        this.cmabCache = options.getCmabCache();
        this.cmabClient = options.getCmabClient();
        this.logger = options.getLogger();
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
}

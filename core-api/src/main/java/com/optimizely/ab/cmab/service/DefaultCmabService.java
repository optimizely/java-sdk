package com.optimizely.ab.cmab.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;

import com.optimizely.ab.OptimizelyUserContext;
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
        return null;
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
        
        Map<String,Attribute> attributeIdMapping = projectConfig.getAttributeIdMapping();
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
    
    /**
     * Hash attributes using MD5
     */
    private String hashAttributes(Map<String, Object> attributes) {
        try {
            // Sort attributes to ensure consistent hashing
            TreeMap<String, Object> sortedAttributes = new TreeMap<>(attributes);
            
            // Create a simple string representation
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : sortedAttributes.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                sb.append("\"").append(entry.getKey()).append("\":");
                if (entry.getValue() instanceof String) {
                    sb.append("\"").append(entry.getValue()).append("\"");
                } else {
                    sb.append(entry.getValue());
                }
                first = false;
            }
            sb.append("}");
            
            // Generate MD5 hash
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sb.toString().getBytes());
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            if (logger != null) {
                logger.warn("Failed to hash attributes", e);
            }
            return "";
        }
    }
}

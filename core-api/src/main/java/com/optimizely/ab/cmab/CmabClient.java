package com.optimizely.ab.cmab;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface CmabClient {
    /**
     * Fetches a decision from the CMAB prediction service.
     *
     * @param ruleId     The rule/experiment ID
     * @param userId     The user ID
     * @param attributes User attributes
     * @param cmabUuid   The CMAB UUID
     * @return CompletableFuture containing the variation ID as a String
     */
    CompletableFuture<String> fetchDecision(String ruleId, String userId, Map<String, Object> attributes, String cmabUuid);
}

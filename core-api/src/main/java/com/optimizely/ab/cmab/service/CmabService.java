package com.optimizely.ab.cmab.service;

import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;

import java.util.List;

public interface CmabService {
    /**
     * Get variation id for the user
     * @param projectConfig the project configuration
     * @param userContext   the user context
     * @param ruleId        the rule identifier
     * @param options       list of decide options
     * @return CompletableFuture containing the CMAB decision
     */
    CmabDecision getDecision(
        ProjectConfig projectConfig,
        OptimizelyUserContext userContext,
        String ruleId,
        List<OptimizelyDecideOption> options
    );
}

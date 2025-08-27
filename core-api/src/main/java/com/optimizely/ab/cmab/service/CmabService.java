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

import java.util.List;

import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;

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

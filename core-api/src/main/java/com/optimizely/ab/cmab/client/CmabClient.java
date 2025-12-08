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
package com.optimizely.ab.cmab.client;

import java.util.Map;

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
    String fetchDecision(String ruleId, String userId, Map<String, Object> attributes, String cmabUuid);
}

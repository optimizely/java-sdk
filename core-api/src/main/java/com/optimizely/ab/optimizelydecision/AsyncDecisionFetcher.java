/**
 * Copyright 2025, Optimizely and contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.optimizelydecision;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.optimizely.ab.OptimizelyUserContext;

/**
 * AsyncDecisionFetcher handles asynchronous decision fetching for single or multiple flag keys.
 * This class follows the same pattern as ODP's async segment fetching.
 */
public class AsyncDecisionFetcher extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(AsyncDecisionFetcher.class);

    private final String singleKey;
    private final List<String> keys;
    private final List<OptimizelyDecideOption> options;
    private final OptimizelyDecisionCallback singleCallback;
    private final OptimizelyDecisionsCallback multipleCallback;
    private final OptimizelyUserContext userContext;
    private final boolean decideAll;
    private final FetchType fetchType;

    private enum FetchType {
        SINGLE_DECISION,
        MULTIPLE_DECISIONS,
        ALL_DECISIONS
    }

    /**
     * Constructor for async single decision fetching.
     *
     * @param userContext The user context to make decisions for
     * @param key The flag key to decide on
     * @param options Decision options
     * @param callback Callback to invoke when decision is ready
     */
    public AsyncDecisionFetcher(@Nonnull OptimizelyUserContext userContext,
                                @Nonnull String key,
                                @Nonnull List<OptimizelyDecideOption> options,
                                @Nonnull OptimizelyDecisionCallback callback) {
        this.userContext = userContext;
        this.singleKey = key;
        this.keys = null;
        this.options = options;
        this.singleCallback = callback;
        this.multipleCallback = null;
        this.decideAll = false;
        this.fetchType = FetchType.SINGLE_DECISION;

        setName("AsyncDecisionFetcher-" + key);
        setDaemon(true);
    }

    /**
     * Constructor for deciding on specific keys.
     *
     * @param userContext The user context to make decisions for
     * @param keys        List of flag keys to decide on
     * @param options     Decision options
     * @param callback    Callback to invoke when decisions are ready
     */
    public AsyncDecisionFetcher(@Nonnull OptimizelyUserContext userContext,
                                @Nonnull List<String> keys,
                                @Nonnull List<OptimizelyDecideOption> options,
                                @Nonnull OptimizelyDecisionsCallback callback) {
        this.userContext = userContext;
        this.singleKey = null;
        this.keys = keys;
        this.options = options;
        this.singleCallback = null;
        this.multipleCallback = callback;
        this.decideAll = false;
        this.fetchType = FetchType.MULTIPLE_DECISIONS;

        setName("AsyncDecisionFetcher-keys");
        setDaemon(true);
    }

    /**
     * Constructor for deciding on all flags.
     *
     * @param userContext The user context to make decisions for
     * @param options     Decision options
     * @param callback    Callback to invoke when decisions are ready
     */
    public AsyncDecisionFetcher(@Nonnull OptimizelyUserContext userContext,
                                @Nonnull List<OptimizelyDecideOption> options,
                                @Nonnull OptimizelyDecisionsCallback callback) {
        this.userContext = userContext;
        this.singleKey = null;
        this.keys = null;
        this.options = options;
        this.singleCallback = null;
        this.multipleCallback = callback;
        this.decideAll = true;
        this.fetchType = FetchType.ALL_DECISIONS;

        setName("AsyncDecisionFetcher-all");
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            switch (fetchType) {
                case SINGLE_DECISION:
                    handleSingleDecision();
                    break;
                case MULTIPLE_DECISIONS:
                    handleMultipleDecisions();
                    break;
                case ALL_DECISIONS:
                    handleAllDecisions();
                    break;
            }
        } catch (Exception e) {
            logger.error("Error in async decision fetching", e);
            handleError(e);
        }
    }

    private void handleSingleDecision() {
        OptimizelyDecision decision = userContext.decide(singleKey, options);
        singleCallback.onCompleted(decision);
    }

    private void handleMultipleDecisions() {
        Map<String, OptimizelyDecision> decisions = userContext.decideForKeys(keys, options);
        multipleCallback.onCompleted(decisions);
    }

    private void handleAllDecisions() {
        Map<String, OptimizelyDecision> decisions = userContext.decideAll(options);
        multipleCallback.onCompleted(decisions);
    }

    private void handleError(Exception e) {
        switch (fetchType) {
            case SINGLE_DECISION:
                OptimizelyDecision errorDecision = createErrorDecision(singleKey, e.getMessage());
                singleCallback.onCompleted(errorDecision);
                break;
            case MULTIPLE_DECISIONS:
            case ALL_DECISIONS:
                // Return empty map on error - this follows the pattern of sync methods
                multipleCallback.onCompleted(Collections.emptyMap());
                break;
        }
    }

    /**
     * Creates an error decision when async operation fails.
     * This follows the same pattern as sync methods - return a decision with error info.
     *
     * @param key The flag key that failed
     * @param errorMessage The error message
     * @return An OptimizelyDecision with error information
     */
    private OptimizelyDecision createErrorDecision(String key, String errorMessage) {
        // We'll create a decision with null variation and include the error in reasons
        // This mirrors how the sync methods handle errors
        return OptimizelyDecision.newErrorDecision(key, userContext, "Async decision error: " + errorMessage);
    }
}

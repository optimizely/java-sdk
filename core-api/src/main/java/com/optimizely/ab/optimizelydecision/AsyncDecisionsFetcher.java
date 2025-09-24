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

import com.optimizely.ab.OptimizelyUserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AsyncDecisionsFetcher handles asynchronous decision fetching for multiple flag keys.
 * This class follows the same pattern as ODP's async segment fetching.
 */
public class AsyncDecisionsFetcher extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(AsyncDecisionsFetcher.class);

    private final List<String> keys;
    private final List<OptimizelyDecideOption> options;
    private final OptimizelyDecisionsCallback callback;
    private final OptimizelyUserContext userContext;
    private final boolean decideAll;

    /**
     * Constructor for deciding on specific keys.
     *
     * @param userContext The user context to make decisions for
     * @param keys        List of flag keys to decide on
     * @param options     Decision options
     * @param callback    Callback to invoke when decisions are ready
     */
    public AsyncDecisionsFetcher(@Nonnull OptimizelyUserContext userContext,
                                 @Nonnull List<String> keys,
                                 @Nonnull List<OptimizelyDecideOption> options,
                                 @Nonnull OptimizelyDecisionsCallback callback) {
        this(userContext, keys, options, callback, false);
    }

    /**
     * Constructor for deciding on all flags or specific keys.
     *
     * @param userContext The user context to make decisions for
     * @param keys        List of flag keys to decide on (null for decideAll)
     * @param options     Decision options
     * @param callback    Callback to invoke when decisions are ready
     * @param decideAll   Whether to decide for all active flags
     */
    public AsyncDecisionsFetcher(@Nonnull OptimizelyUserContext userContext,
                                 @Nullable List<String> keys,
                                 @Nonnull List<OptimizelyDecideOption> options,
                                 @Nonnull OptimizelyDecisionsCallback callback,
                                 boolean decideAll) {
        this.userContext = userContext;
        this.keys = keys;
        this.options = options;
        this.callback = callback;
        this.decideAll = decideAll;

        // Set thread name for debugging
        String threadName = decideAll ? "AsyncDecisionsFetcher-all" : "AsyncDecisionsFetcher-keys";
        setName(threadName);

        // Set as daemon thread so it doesn't prevent JVM shutdown
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            Map<String, OptimizelyDecision> decisions;
            if (decideAll) {
                decisions = userContext.decideAll(options);
            } else {
                decisions = userContext.decideForKeys(keys, options);
            }
            callback.onCompleted(decisions);
        } catch (Exception e) {
            logger.error("Error in async decisions fetching", e);
            // Return empty map on error - this follows the pattern of sync methods
            callback.onCompleted(Collections.emptyMap());
        }
    }
}
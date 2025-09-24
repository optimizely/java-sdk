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
import java.util.List;

/**
 * AsyncDecisionFetcher handles asynchronous decision fetching for a single flag key.
 * This class follows the same pattern as ODP's async segment fetching.
 */
public class AsyncDecisionFetcher extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(AsyncDecisionFetcher.class);

    private final String key;
    private final List<OptimizelyDecideOption> options;
    private final OptimizelyDecisionCallback callback;
    private final OptimizelyUserContext userContext;

    /**
     * Constructor for async decision fetching.
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
        this.key = key;
        this.options = options;
        this.callback = callback;

        // Set thread name for debugging
        setName("AsyncDecisionFetcher-" + key);

        // Set as daemon thread so it doesn't prevent JVM shutdown
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            OptimizelyDecision decision = userContext.decide(key, options);
            callback.onCompleted(decision);
        } catch (Exception e) {
            logger.error("Error in async decision fetching for key: " + key, e);
            // Create an error decision and pass it to the callback
            OptimizelyDecision errorDecision = createErrorDecision(key, e.getMessage());
            callback.onCompleted(errorDecision);
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
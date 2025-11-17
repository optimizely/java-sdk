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

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Callback interface for async multiple decisions operations.
 */
@FunctionalInterface
public interface OptimizelyDecisionsCallback {
    /**
     * Called when an async multiple decisions operation completes.
     *
     * @param decisions Map of flag keys to decision results
     */
    void onCompleted(@Nonnull Map<String, OptimizelyDecision> decisions);
}

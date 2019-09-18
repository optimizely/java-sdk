/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/
package com.optimizely.ab.decision.bucketer;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;

import javax.annotation.Nonnull;

/**
 * Default Optimizely bucketing algorithm that evenly distributes users using the Murmur3 hash of some provided
 * identifier.
 */
public interface IBucketer {
    /**
     * Assign a {@link Variation} of an {@link Experiment} to a user based on hashed value from murmurhash3.
     *
     * @param experiment  The Experiment the user will be bucketed into.
     * @param bucketingId String A customer-assigned value used to create the key for the murmur hash.
     * @return {@link Variation} the user is bucketed into or null.
     */
    Variation bucket(@Nonnull Experiment experiment,
                     @Nonnull String bucketingId,
                     @Nonnull ProjectConfig projectConfig);
}
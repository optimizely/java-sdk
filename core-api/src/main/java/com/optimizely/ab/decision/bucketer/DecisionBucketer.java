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

import com.optimizely.ab.annotations.VisibleForTesting;

/**
 * Default Optimizely bucketing algorithm that evenly distributes users using the Murmur3 hash of some provided
 * identifier.
 */
public class DecisionBucketer implements Bucketer {

    /**
     * The maximum bucket value (represents 100 Basis Points).
     */
    @VisibleForTesting
    static final int MAX_TRAFFIC_VALUE = 10000;

    /**
     * Map the given 32-bit hashcode into the range [0, MAX_TRAFFIC_VALUE]).
     *
     * @param hashCode the provided hashcode
     * @return a value in the range closed-open range, [0, MAX_TRAFFIC_VALUE])
     */
    @Override
    public int generateBucketValue(int hashCode) {
        // map the hashCode into the range [0, BucketAlgorithm.MAX_TRAFFIC_VALUE)
        double ratio = (double) (hashCode & 0xFFFFFFFFL) / Math.pow(2, 32);
        return (int) Math.floor(MAX_TRAFFIC_VALUE * ratio);
    }
}
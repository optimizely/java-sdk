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

import com.optimizely.ab.bucketing.internal.MurmurHash3;
import com.optimizely.ab.categories.ExhaustiveTest;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class MurmurhashBucketerTest {

    private Bucketer algorithm;

    @Before
    public void setUp() {
        algorithm = new MurmurhashBucketer();
    }

    /**
     * Verify that {@link MurmurhashBucketer#generateBucketValue(int)} correctly handles negative hashCodes.
     */
    @Test
    public void generateBucketValueForNegativeHashCodes() throws Exception {
        int actual = algorithm.generateBucketValue(-1);
        assertTrue("generated bucket value is not in range: " + actual,
            actual > 0 && actual < MurmurhashBucketer.MAX_TRAFFIC_VALUE);
    }

    /**
     * Verify that across the entire 32-bit hashCode space, all generated bucket values fall within the range
     * [0, {@link MurmurhashBucketer#MAX_TRAFFIC_VALUE}) and that there's an even distribution over 50/50 split.
     */
    @Test
    @Category(ExhaustiveTest.class)
    public void generateBucketValueDistribution() throws Exception {
        Assume.assumeTrue(Boolean.valueOf(System.getenv("CI")));
        long lowerHalfCount = 0;
        long totalCount = 0;
        int outOfRangeCount = 0;

        for (int i = Integer.MIN_VALUE; i < Integer.MAX_VALUE; i++) {
            int bucketValue = algorithm.generateBucketValue(i);

            totalCount++;
            if (bucketValue < (MurmurhashBucketer.MAX_TRAFFIC_VALUE / 2)) {
                lowerHalfCount++;
            }
            if (bucketValue < 0 || bucketValue >= MurmurhashBucketer.MAX_TRAFFIC_VALUE) {
                outOfRangeCount++;
            }
        }

        // verify that all values are in the expected range and that 50% of the values are in the lower half
        assertThat(outOfRangeCount, is(0));
        assertThat(Math.round(((double) lowerHalfCount / totalCount) * 100), is(50L));
    }

    /**
     * Verify that generated bucket values match expected output.
     */
    @Test
    public void bucketNumberGeneration() throws Exception {
        int MURMUR_HASH_SEED = 1;
        int experimentId = 1886780721;
        int hashCode;

        String combinedBucketId;

        combinedBucketId = "ppid1" + experimentId;
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length(), MURMUR_HASH_SEED);
        assertThat(algorithm.generateBucketValue(hashCode), is(5254));

        combinedBucketId = "ppid2" + experimentId;
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length(), MURMUR_HASH_SEED);
        assertThat(algorithm.generateBucketValue(hashCode), is(4299));

        combinedBucketId = "ppid2" + (experimentId + 1);
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length(), MURMUR_HASH_SEED);
        assertThat(algorithm.generateBucketValue(hashCode), is(2434));

        combinedBucketId = "ppid3" + experimentId;
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length(), MURMUR_HASH_SEED);
        assertThat(algorithm.generateBucketValue(hashCode), is(5439));

        combinedBucketId = "a very very very very very very very very very very very very very very very long ppd " +
            "string" + experimentId;
        hashCode = MurmurHash3.murmurhash3_x86_32(combinedBucketId, 0, combinedBucketId.length(), MURMUR_HASH_SEED);
        assertThat(algorithm.generateBucketValue(hashCode), is(6128));
    }
}
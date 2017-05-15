/**
 *
 *    Copyright 2017, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.bucketing;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Class encapsulating user profile service functionality.
 *
 * Override with your own implementation for storing and retrieving the user profile.
 */
public interface UserProfileService {

    /**
     * A class representing a user's profile.
     */
    class UserProfile {

        static class Decision {
            @Nonnull public String variationId;

            public Decision(@Nonnull String variationId) {
                this.variationId = variationId;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Decision decision = (Decision) o;

                return variationId.equals(decision.variationId);
            }

            @Override
            public int hashCode() {
                return variationId.hashCode();
            }
        }

        /** A user's ID. */
        @Nonnull public final String userId;
        /** The bucketing experimentBucketMap of the user. */
        @Nonnull public final Map<String, Decision> experimentBucketMap;

        /**
         * Construct a User Profile instance from explicit components.
         * @param userId The ID of the user.
         * @param experimentBucketMap The bucketing experimentBucketMap of the user.
         */
        public UserProfile(@Nonnull String userId, @Nonnull Map<String, Decision> experimentBucketMap) {
            this.userId = userId;
            this.experimentBucketMap = experimentBucketMap;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UserProfile that = (UserProfile) o;

            if (!userId.equals(that.userId)) return false;
            return experimentBucketMap.equals(that.experimentBucketMap);
        }

        @Override
        public int hashCode() {
            int result = userId.hashCode();
            result = 31 * result + experimentBucketMap.hashCode();
            return result;
        }
    }

    /**
     * Fetch the user profile map for the user ID.
     *
     * @param userId The ID of the user whose profile will be retrieved.
     * @return The associated {@link UserProfile}
     * @throws Exception Passes on whatever exceptions the implementation may throw.
     */
    UserProfile lookup(String userId) throws Exception;

    /**
     * Save the user profile Map sent to this method.
     *
     * @param userProfile The Map representing the user's profile.
     * @throws Exception Can throw an exception if the user profile was not saved properly.
     */
    void save(UserProfile userProfile) throws Exception;
}

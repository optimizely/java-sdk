/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
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
package com.optimizely.ab.bucketing;

import com.optimizely.ab.faultinjection.ExceptionSpot;
import com.optimizely.ab.faultinjection.FaultInjectionManager;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A class representing a user's profile.
 */
public class UserProfile {

    /**
     * A user's ID.
     */
    @Nonnull
    public final String userId;
    /**
     * The bucketing experimentBucketMap of the user.
     */
    @Nonnull
    public final Map<String, Decision> experimentBucketMap;

    private static void injectFault(ExceptionSpot spot) {
        FaultInjectionManager.getInstance().injectFault(spot);
    }

    /**
     * Construct a User Profile instance from explicit components.
     *
     * @param userId              The ID of the user.
     * @param experimentBucketMap The bucketing experimentBucketMap of the user.
     */
    public UserProfile(@Nonnull String userId, @Nonnull Map<String, Decision> experimentBucketMap) {
        injectFault(ExceptionSpot.UserProfile_constructor_spot1);
        this.userId = userId;
        this.experimentBucketMap = experimentBucketMap;
    }

    @Override
    public boolean equals(Object o) {
        injectFault(ExceptionSpot.UserProfile_equals_spot1);
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        injectFault(ExceptionSpot.UserProfile_equals_spot2);
        UserProfile that = (UserProfile) o;

        if (!userId.equals(that.userId)) return false;
        injectFault(ExceptionSpot.UserProfile_equals_spot3);
        return experimentBucketMap.equals(that.experimentBucketMap);
    }

    @Override
    public int hashCode() {
        injectFault(ExceptionSpot.UserProfile_hashCode_spot1);
        int result = userId.hashCode();
        result = 31 * result + experimentBucketMap.hashCode();
        injectFault(ExceptionSpot.UserProfile_hashCode_spot2);
        return result;
    }

    /**
     * Convert a User Profile instance to a Map.
     *
     * @return A map representation of the user profile instance.
     */
    Map<String, Object> toMap() {
        injectFault(ExceptionSpot.UserProfile_toMap_spot1);
        Map<String, Object> userProfileMap = new HashMap<String, Object>(2);
        userProfileMap.put(UserProfileService.userIdKey, userId);
        Map<String, Map<String, String>> decisionsMap = new HashMap<String, Map<String, String>>(experimentBucketMap.size());
        for (Entry<String, Decision> decisionEntry : experimentBucketMap.entrySet()) {
            injectFault(ExceptionSpot.UserProfile_toMap_spot2);
            decisionsMap.put(decisionEntry.getKey(), decisionEntry.getValue().toMap());
        }
        userProfileMap.put(UserProfileService.experimentBucketMapKey, decisionsMap);
        injectFault(ExceptionSpot.UserProfile_toMap_spot3);
        return userProfileMap;
    }
}

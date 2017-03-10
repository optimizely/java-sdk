/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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

import java.util.Map;

/**
 * Gives implementors a chance to override {@link Bucketer} bucketing on subsequent activations.
 *
 * Overriding bucketing for subsequent activations is useful in order to prevent changes to
 * user experience after changing traffic allocations.  Also, this interface gives users
 * a hook to keep track of activation history.
 */
public interface UserProfile {

    /**
     * Called when implementors should save an activation
     *
     * @param userId the user id of the activation
     * @param experimentId the experiment ID of the activation
     * @param variationId the variation ID of the activation
     * @return true if saving of the record was successful
     */
    boolean save(String userId, String experimentId, String variationId);

    /**
     * Called by the bucketer to check for a record of the previous activation
     *
     * @param userId the user is id of the next activation
     * @param experimentId the experiment ID of the next activation
     * @return the variation ID of the next activation, or null if no record exists
     */
    String lookup(String userId, String experimentId);

    /**
     * Called when user profile should be removed
     *
     * Records should be removed when an experiment is not running or when an experiment has been
     * deleted.
     *
     * @param userId the user id of the record to remove
     * @param experimentId the experiment ID of the record to remove
     * @return true if the record was removed
     */
    boolean remove(String userId, String experimentId);

    /**
     * Called by bucketer to get a mapping of all stored records
     *
     * This mapping is needed so that the bucketer can {@link #remove(String, String)} outdated
     * records.
     * @return a map of user IDs to a map of experiment IDs to variation IDs
     */
    Map<String, Map<String, String>> getAllRecords();

}

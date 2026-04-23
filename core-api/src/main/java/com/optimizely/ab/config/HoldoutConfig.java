/**
 *
 *    Copyright 2016-2019, 2021, Optimizely and contributors
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


package com.optimizely.ab.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HoldoutConfig manages collections of Holdout objects.
 * All holdouts are global and apply to all flags.
 */
public class HoldoutConfig {
    private List<Holdout> allHoldouts;
    private Map<String, Holdout> holdoutIdMap;

    /**
     * Initializes a new HoldoutConfig with an empty list of holdouts.
     */
    public HoldoutConfig() {
        this(Collections.emptyList());
    }

    /**
     * Initializes a new HoldoutConfig with the specified holdouts.
     *
     * @param allHoldouts The list of holdouts to manage
     */
    public HoldoutConfig(@Nonnull List<Holdout> allHoldouts) {
        this.allHoldouts = new ArrayList<>(allHoldouts);
        this.holdoutIdMap = new HashMap<>();
        updateHoldoutMapping();
    }

    /**
     * Updates internal mapping of holdout IDs to holdout objects.
     */
    private void updateHoldoutMapping() {
        holdoutIdMap.clear();
        for (Holdout holdout : allHoldouts) {
            holdoutIdMap.put(holdout.getId(), holdout);
        }
    }

    /**
     * Returns all holdouts for the given flag ID.
     * Since all holdouts are now global, this returns all holdouts.
     *
     * @param id The flag identifier
     * @return A list of all Holdout objects
     */
    public List<Holdout> getHoldoutForFlag(@Nonnull String id) {
        return Collections.unmodifiableList(allHoldouts);
    }

    /**
     * Get a Holdout object for an Id.
     *
     * @param id The holdout identifier
     * @return The Holdout object if found, null otherwise
     */
    @Nullable
    public Holdout getHoldout(@Nonnull String id) {
        return holdoutIdMap.get(id);
    }

    /**
     * Returns all holdouts managed by this config.
     *
     * @return An unmodifiable list of all holdouts
     */
    public List<Holdout> getAllHoldouts() {
        return Collections.unmodifiableList(allHoldouts);
    }
}

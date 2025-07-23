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
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HoldoutConfig manages collections of Holdout objects and their relationships to flags.
 */
public class HoldoutConfig {
    private List<Holdout> allHoldouts;
    private List<Holdout> global;
    private Map<String, Holdout> holdoutIdMap;
    private Map<String, List<Holdout>> flagHoldoutsMap;
    private Map<String, List<Holdout>> includedHoldouts;
    private Map<String, List<Holdout>> excludedHoldouts;

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
        this.global = new ArrayList<>();
        this.holdoutIdMap = new HashMap<>();
        this.flagHoldoutsMap = new ConcurrentHashMap<>();
        this.includedHoldouts = new HashMap<>();
        this.excludedHoldouts = new HashMap<>();
        updateHoldoutMapping();
    }

    /**
     * Updates internal mappings of holdouts including the id map, global list,
     * and per-flag inclusion/exclusion maps.
     */
    private void updateHoldoutMapping() {
        holdoutIdMap.clear();
        for (Holdout holdout : allHoldouts) {
            holdoutIdMap.put(holdout.getId(), holdout);
        }

        flagHoldoutsMap.clear();
        global.clear();
        includedHoldouts.clear();
        excludedHoldouts.clear();

        for (Holdout holdout : allHoldouts) {
            boolean hasIncludedFlags = !holdout.getIncludedFlags().isEmpty();
            boolean hasExcludedFlags = !holdout.getExcludedFlags().isEmpty();

            if (!hasIncludedFlags && !hasExcludedFlags) {
                // Global holdout (applies to all flags)
                global.add(holdout);
            } else if (hasIncludedFlags) {
                // Holdout only applies to specific included flags
                for (String flagId : holdout.getIncludedFlags()) {
                    includedHoldouts.computeIfAbsent(flagId, k -> new ArrayList<>()).add(holdout);
                }
            } else {
                // Global holdout with specific exclusions
                global.add(holdout);

                for (String flagId : holdout.getExcludedFlags()) {
                    excludedHoldouts.computeIfAbsent(flagId, k -> new ArrayList<>()).add(holdout);
                }
            }
        }
    }

    /**
     * Returns the applicable holdouts for the given flag ID by combining global holdouts
     * (excluding any specified) and included holdouts, in that order.
     * Caches the result for future calls.
     *
     * @param id The flag identifier
     * @return A list of Holdout objects relevant to the given flag
     */
    public List<Holdout> getHoldoutForFlag(@Nonnull String id) {
        if (allHoldouts.isEmpty()) {
            return Collections.emptyList();
        }

        // Check cache and return persistent holdouts
        if (flagHoldoutsMap.containsKey(id)) {
            return flagHoldoutsMap.get(id);
        }

        // Prioritize global holdouts first
        List<Holdout> activeHoldouts = new ArrayList<>();
        List<Holdout> excluded = excludedHoldouts.getOrDefault(id, Collections.emptyList());

        if (!excluded.isEmpty()) {
            for (Holdout holdout : global) {
                if (!excluded.contains(holdout)) {
                    activeHoldouts.add(holdout);
                }
            }
        } else {
            activeHoldouts.addAll(global);
        }

        // Add included holdouts
        activeHoldouts.addAll(includedHoldouts.getOrDefault(id, Collections.emptyList()));

        // Cache the result
        flagHoldoutsMap.put(id, activeHoldouts);

        return activeHoldouts;
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

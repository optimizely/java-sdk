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
 * Supports both global holdouts (apply to all rules) and local holdouts (apply to specific rules).
 */
public class HoldoutConfig {
    private List<Holdout> allHoldouts;
    private Map<String, Holdout> holdoutIdMap;
    private List<Holdout> globalHoldouts;
    private Map<String, List<Holdout>> ruleHoldoutsMap;

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
        this.globalHoldouts = new ArrayList<>();
        this.ruleHoldoutsMap = new HashMap<>();
        updateHoldoutMapping();
    }

    /**
     * Updates internal mappings of holdout IDs and rule-level holdouts.
     * Separates global holdouts (includedRules == null) from local holdouts (includedRules != null).
     */
    private void updateHoldoutMapping() {
        holdoutIdMap.clear();
        globalHoldouts.clear();
        ruleHoldoutsMap.clear();

        for (Holdout holdout : allHoldouts) {
            holdoutIdMap.put(holdout.getId(), holdout);

            if (holdout.isGlobal()) {
                // Global holdout: applies to all rules
                globalHoldouts.add(holdout);
            } else {
                // Local holdout: applies to specific rules
                List<String> includedRules = holdout.getIncludedRules();
                if (includedRules != null) {
                    for (String ruleId : includedRules) {
                        ruleHoldoutsMap.computeIfAbsent(ruleId, k -> new ArrayList<>()).add(holdout);
                    }
                }
            }
        }
    }

    /**
     * Returns all global holdouts (those that apply to all rules).
     *
     * @return An unmodifiable list of global holdouts
     */
    @Nonnull
    public List<Holdout> getGlobalHoldouts() {
        return Collections.unmodifiableList(globalHoldouts);
    }

    /**
     * Returns local holdouts that target a specific rule.
     *
     * @param ruleId The rule identifier
     * @return A list of holdouts targeting this rule, or an empty list if none
     */
    @Nonnull
    public List<Holdout> getHoldoutsForRule(@Nonnull String ruleId) {
        List<Holdout> holdouts = ruleHoldoutsMap.get(ruleId);
        return holdouts != null ? Collections.unmodifiableList(holdouts) : Collections.emptyList();
    }

    /**
     * Returns all holdouts for the given flag ID.
     * Since all holdouts are now global, this returns all global holdouts.
     * This method is deprecated; use getGlobalHoldouts() instead.
     *
     * @param id The flag identifier
     * @return A list of all global Holdout objects
     * @deprecated Use {@link #getGlobalHoldouts()} instead
     */
    @Deprecated
    public List<Holdout> getHoldoutForFlag(@Nonnull String id) {
        return getGlobalHoldouts();
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

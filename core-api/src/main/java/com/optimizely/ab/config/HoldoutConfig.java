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
 * HoldoutConfig manages collections of Holdout objects and provides access
 * by global classification and by rule ID for local holdouts.
 */
public class HoldoutConfig {
    private List<Holdout> allHoldouts;
    private Map<String, Holdout> holdoutIdMap;

    /** All holdouts whose {@code includedRules} is null (global holdouts). */
    private List<Holdout> globalHoldouts;

    /**
     * Map from rule ID to the list of local holdouts targeting that rule.
     * A holdout is local when its {@code includedRules} is non-null.
     */
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
     * Rebuilds all internal mappings from the current {@code allHoldouts} list.
     *
     * <p>Global holdouts (includedRules == null) are collected into {@code globalHoldouts}.
     * Local holdouts (includedRules != null) are indexed by each rule ID they target
     * into {@code ruleHoldoutsMap}.
     */
    private void updateHoldoutMapping() {
        holdoutIdMap.clear();
        globalHoldouts.clear();
        ruleHoldoutsMap.clear();

        for (Holdout holdout : allHoldouts) {
            holdoutIdMap.put(holdout.getId(), holdout);

            if (holdout.isGlobal()) {
                globalHoldouts.add(holdout);
            } else {
                List<String> rules = holdout.getIncludedRules();
                // rules is non-null here because isGlobal() returned false
                for (String ruleId : rules) {
                    ruleHoldoutsMap.computeIfAbsent(ruleId, k -> new ArrayList<>()).add(holdout);
                }
            }
        }
    }

    /**
     * Returns all global holdouts (those with {@code includedRules == null}).
     * Global holdouts apply to every rule in every flag.
     *
     * @return An unmodifiable list of global holdouts
     */
    @Nonnull
    public List<Holdout> getGlobalHoldouts() {
        return Collections.unmodifiableList(globalHoldouts);
    }

    /**
     * Returns all local holdouts that target the given rule ID.
     * If no local holdouts are registered for this rule, an empty list is returned.
     *
     * @param ruleId The experiment or delivery rule ID to look up
     * @return An unmodifiable list of holdouts targeting the specified rule
     */
    @Nonnull
    public List<Holdout> getHoldoutsForRule(@Nonnull String ruleId) {
        List<Holdout> result = ruleHoldoutsMap.get(ruleId);
        return result != null ? Collections.unmodifiableList(result) : Collections.emptyList();
    }

    /**
     * Returns all holdouts for the given flag ID.
     * For backward compatibility this returns all global holdouts.
     *
     * @param id The flag identifier (unused — retained for API compatibility)
     * @return An unmodifiable list of global Holdout objects
     * @deprecated Prefer {@link #getGlobalHoldouts()} for new code.
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
    @Nonnull
    public List<Holdout> getAllHoldouts() {
        return Collections.unmodifiableList(allHoldouts);
    }
}

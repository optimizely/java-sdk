/**
 *
 *    Copyright 2016-2019, 2021, 2026, Optimizely and contributors
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
 * HoldoutConfig manages collections of Holdout objects, distinguishing between global holdouts
 * (which apply to all rules) and local holdouts (which target specific rule IDs).
 */
public class HoldoutConfig {
    private List<Holdout> allHoldouts;
    private Map<String, Holdout> holdoutIdMap;

    /** Global holdouts: holdouts where includedRules == null. Evaluated at flag level. */
    private List<Holdout> globalHoldouts;

    /** Rule-level map: ruleId -> list of local holdouts targeting that rule. */
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
     * Updates internal mappings:
     * - holdoutIdMap: id -> Holdout
     * - globalHoldouts: holdouts where includedRules == null
     * - ruleHoldoutsMap: ruleId -> list of holdouts that include that rule
     */
    private void updateHoldoutMapping() {
        holdoutIdMap.clear();
        globalHoldouts.clear();
        ruleHoldoutsMap.clear();

        for (Holdout holdout : allHoldouts) {
            holdoutIdMap.put(holdout.getId(), holdout);

            if (holdout.isGlobal()) {
                // includedRules == null: global holdout — applies to all rules
                globalHoldouts.add(holdout);
            } else {
                // includedRules != null: local holdout — add to each targeted rule
                List<String> includedRules = holdout.getIncludedRules();
                for (String ruleId : includedRules) {
                    if (!ruleHoldoutsMap.containsKey(ruleId)) {
                        ruleHoldoutsMap.put(ruleId, new ArrayList<>());
                    }
                    ruleHoldoutsMap.get(ruleId).add(holdout);
                }
            }
        }
    }

    /**
     * Returns all global holdouts (holdouts where includedRules == null).
     * These are evaluated at the flag level, before any rules are evaluated.
     *
     * @return An unmodifiable list of global holdouts
     */
    public List<Holdout> getGlobalHoldouts() {
        return Collections.unmodifiableList(globalHoldouts);
    }

    /**
     * Returns local holdouts targeting a specific rule ID.
     * These are evaluated per-rule, after the forced decision check and before regular rule evaluation.
     *
     * @param ruleId The rule identifier to look up
     * @return An unmodifiable list of local holdouts targeting that rule, or empty list if none
     */
    @Nonnull
    public List<Holdout> getHoldoutsForRule(@Nonnull String ruleId) {
        List<Holdout> holdouts = ruleHoldoutsMap.get(ruleId);
        return holdouts != null ? Collections.unmodifiableList(holdouts) : Collections.emptyList();
    }

    /**
     * Returns all holdouts for the given flag ID.
     * For backward compatibility: returns all global holdouts (same behavior as before local holdouts).
     *
     * @param id The flag identifier
     * @return A list of global Holdout objects
     * @deprecated Use {@link #getGlobalHoldouts()} for flag-level evaluation and
     *             {@link #getHoldoutsForRule(String)} for per-rule evaluation.
     */
    @Deprecated
    public List<Holdout> getHoldoutForFlag(@Nonnull String id) {
        return Collections.unmodifiableList(globalHoldouts);
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

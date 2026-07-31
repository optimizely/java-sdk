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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HoldoutConfig manages collections of Holdout objects partitioned by datafile section.
 *
 * <p>Two top-level datafile sections drive holdout scoping (Gen 3+):
 * <ul>
 *   <li>{@code holdouts}      — every entry is a global holdout (applied to every flag).
 *                               Any {@code includedRules} field on these entries is IGNORED
 *                               and stripped while building the mapping; section membership
 *                               alone determines scope.</li>
 *   <li>{@code localHoldouts} — every entry is a local holdout (rule-scoped via
 *                               {@code includedRules}). Entries missing {@code includedRules}
 *                               (null) are invalid and skipped with an error log. Entries
 *                               with an empty {@code includedRules} list are valid but inert:
 *                               tracked in the id map, not registered under any rule.</li>
 * </ul>
 *
 * <p>Backward compatibility: older datafiles that only emit the {@code holdouts} section
 * continue to work unchanged — every entry is treated as global, matching pre-localHoldouts
 * behavior. The {@code localHoldouts} key is simply absent and parsed as an empty list.
 */
public class HoldoutConfig {
    private static final Logger logger = LoggerFactory.getLogger(HoldoutConfig.class);

    private List<Holdout> allHoldouts;
    private Map<String, Holdout> holdoutIdMap;

    /** Global holdouts: entries from the datafile 'holdouts' section. Evaluated at flag level. */
    private List<Holdout> globalHoldouts;

    /** Rule-level map: ruleId -> list of local holdouts targeting that rule. */
    private Map<String, List<Holdout>> ruleHoldoutsMap;

    /**
     * Initializes a new HoldoutConfig with no holdouts.
     */
    public HoldoutConfig() {
        this(Collections.<Holdout>emptyList(), Collections.<Holdout>emptyList());
    }

    /**
     * Backward-compatible constructor: treats every entry as if it came from the global
     * 'holdouts' section. Any {@code includedRules} field on these entries is preserved
     * (legacy classification is by entity-level {@code includedRules}, used only by callers
     * who pre-date the section split).
     *
     * @param allHoldouts The list of holdouts to manage
     * @deprecated Prefer {@link #HoldoutConfig(List, List)} so global vs. local scope is
     *             driven by datafile section membership.
     */
    @Deprecated
    public HoldoutConfig(@Nonnull List<Holdout> allHoldouts) {
        this.allHoldouts = new ArrayList<>(allHoldouts);
        this.holdoutIdMap = new HashMap<>();
        this.globalHoldouts = new ArrayList<>();
        this.ruleHoldoutsMap = new HashMap<>();
        updateLegacyHoldoutMapping();
    }

    /**
     * Initializes a new HoldoutConfig from the two top-level datafile sections.
     *
     * <p>Entries in {@code globalHoldoutsFromSection} are treated as global regardless of
     * any {@code includedRules} field they may carry; that field is stripped so section
     * membership is the sole signal for scope.
     *
     * <p>Entries in {@code localHoldoutsFromSection} must carry an {@code includedRules}
     * list. Entries with {@code includedRules == null} are invalid, logged at ERROR, and
     * excluded from evaluation — they do NOT fall back to global application (the partition
     * between sections is hard). Entries with an empty {@code includedRules} list are valid
     * but inert: tracked in the id map, not registered under any rule.
     *
     * @param globalHoldoutsFromSection Entries from the datafile 'holdouts' section
     * @param localHoldoutsFromSection  Entries from the datafile 'localHoldouts' section
     */
    public HoldoutConfig(@Nonnull List<Holdout> globalHoldoutsFromSection,
                         @Nonnull List<Holdout> localHoldoutsFromSection) {
        this.allHoldouts = new ArrayList<>();
        this.holdoutIdMap = new HashMap<>();
        this.globalHoldouts = new ArrayList<>();
        this.ruleHoldoutsMap = new HashMap<>();
        updateHoldoutMapping(globalHoldoutsFromSection, localHoldoutsFromSection);
    }

    /**
     * Section-aware mapping: enforces that scope comes from the datafile section, not the
     * {@code includedRules} field. Stale {@code includedRules} values on global-section
     * entries are stripped; invalid local-section entries are logged and skipped.
     */
    private void updateHoldoutMapping(@Nonnull List<Holdout> globalHoldoutsFromSection,
                                      @Nonnull List<Holdout> localHoldoutsFromSection) {
        // Process global holdouts: section membership is the sole signal for scope.
        // Strip any stale 'includedRules' so the entity is unambiguously global (isGlobal -> true),
        // even if the datafile incorrectly includes one.
        for (Holdout holdout : globalHoldoutsFromSection) {
            Holdout sanitized = holdout.isGlobal() ? holdout : stripIncludedRules(holdout);

            allHoldouts.add(sanitized);
            holdoutIdMap.put(sanitized.getId(), sanitized);
            globalHoldouts.add(sanitized);
        }

        // Process local holdouts: every entry must carry an 'includedRules' field.
        // Entries with null/missing includedRules are invalid per spec — log an error and
        // exclude them from evaluation (do NOT fall back to global application).
        // An empty includedRules list is valid but inert: the entity is tracked in the id
        // map but is not registered under any rule (matches Python reference semantics).
        for (Holdout holdout : localHoldoutsFromSection) {
            List<String> includedRules = holdout.getIncludedRules();
            if (includedRules == null) {
                logger.error(
                    "Local holdout \"{}\" is missing required 'includedRules' field and will be excluded from evaluation.",
                    holdout.getKey() != null ? holdout.getKey() : holdout.getId());
                continue;
            }

            allHoldouts.add(holdout);
            holdoutIdMap.put(holdout.getId(), holdout);
            for (String ruleId : includedRules) {
                if (!ruleHoldoutsMap.containsKey(ruleId)) {
                    ruleHoldoutsMap.put(ruleId, new ArrayList<Holdout>());
                }
                ruleHoldoutsMap.get(ruleId).add(holdout);
            }
        }
    }

    /**
     * Legacy mapping used by the deprecated single-list constructor. Classifies each entry
     * by its entity-level {@code includedRules} (null -> global, non-null -> local).
     * Preserved unchanged for callers that have not migrated to section-aware construction.
     */
    private void updateLegacyHoldoutMapping() {
        for (Holdout holdout : allHoldouts) {
            holdoutIdMap.put(holdout.getId(), holdout);

            if (holdout.isGlobal()) {
                globalHoldouts.add(holdout);
            } else {
                List<String> includedRules = holdout.getIncludedRules();
                for (String ruleId : includedRules) {
                    if (!ruleHoldoutsMap.containsKey(ruleId)) {
                        ruleHoldoutsMap.put(ruleId, new ArrayList<Holdout>());
                    }
                    ruleHoldoutsMap.get(ruleId).add(holdout);
                }
            }
        }
    }

    /**
     * Returns a copy of the given holdout with {@code includedRules} forced to null, so the
     * entity is unambiguously classified as global. Used only when a stale {@code includedRules}
     * appears on an entry coming from the global 'holdouts' section.
     */
    private static Holdout stripIncludedRules(Holdout holdout) {
        return new Holdout(
            holdout.getId(),
            holdout.getKey(),
            holdout.getStatus(),
            holdout.getAudienceIds(),
            holdout.getAudienceConditions(),
            holdout.getVariations(),
            holdout.getTrafficAllocation(),
            null,
            holdout.isExcludeTargetedDeliveries()
        );
    }

    /**
     * Returns all global holdouts (entries from the datafile 'holdouts' section).
     * These are evaluated at the flag level, before any rules are evaluated.
     * Section membership in 'holdouts' is the sole signal for global scope — any
     * 'includedRules' field on these entries is ignored.
     *
     * @return An unmodifiable list of global holdouts
     */
    public List<Holdout> getGlobalHoldouts() {
        return Collections.unmodifiableList(globalHoldouts);
    }

    /**
     * Returns local holdouts targeting a specific rule ID. Local holdouts come from the
     * datafile 'localHoldouts' section and are scoped per-rule via 'includedRules'.
     * Evaluated per-rule, after the forced decision check and before regular rule evaluation.
     *
     * @param ruleId The rule identifier to look up
     * @return An unmodifiable list of local holdouts targeting that rule, or empty list if none
     */
    @Nonnull
    public List<Holdout> getHoldoutsForRule(@Nonnull String ruleId) {
        List<Holdout> holdouts = ruleHoldoutsMap.get(ruleId);
        return holdouts != null ? Collections.unmodifiableList(holdouts) : Collections.<Holdout>emptyList();
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
     * Returns all holdouts managed by this config (both global and local sections, in that order).
     *
     * @return An unmodifiable list of all holdouts
     */
    public List<Holdout> getAllHoldouts() {
        return Collections.unmodifiableList(allHoldouts);
    }
}

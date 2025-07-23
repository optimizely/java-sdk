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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Holdout implements ExperimentCore {

    private final String id;
    private final String key;
    private final String status;
    
    private final List<String> audienceIds;
    private final Condition<AudienceIdCondition> audienceConditions;
    private final List<Variation> variations;
    private final List<TrafficAllocation> trafficAllocation;
    private final List<String> includedFlags;
    private final List<String> excludedFlags;

    private final Map<String, Variation> variationKeyToVariationMap;
    private final Map<String, Variation> variationIdToVariationMap;
    // Not necessary for HO
    private final String layerId = "";

    public enum HoldoutStatus {
        RUNNING("Running"),
        DRAFT("Draft"),
        CONCLUDED("Concluded"),
        ARCHIVED("Archived");

        private final String holdoutStatus;

        HoldoutStatus(String holdoutStatus) {
            this.holdoutStatus = holdoutStatus;
        }

        public String toString() {
            return holdoutStatus;
        }
    }

    @VisibleForTesting
    public Holdout(String id, String key) {
        this(id, key, "Running", Collections.emptyList(), null, Collections.emptyList(), Collections.emptyList(), null, null);
    }

    // Keep only this constructor and add @JsonCreator to it
    @JsonCreator
    public Holdout(@JsonProperty("id") @Nonnull String id,
            @JsonProperty("key") @Nonnull String key,
            @JsonProperty("status") @Nonnull String status,
            @JsonProperty("audienceIds") @Nonnull List<String> audienceIds,
            @JsonProperty("audienceConditions") @Nullable Condition audienceConditions,
            @JsonProperty("variations") @Nonnull List<Variation> variations,
            @JsonProperty("trafficAllocation") @Nonnull List<TrafficAllocation> trafficAllocation,
            @JsonProperty("includedFlags") @Nullable List<String> includedFlags,
            @JsonProperty("excludedFlags") @Nullable List<String> excludedFlags) {
        this.id = id;
        this.key = key;
        this.status = status;
        this.audienceIds = audienceIds;
        this.audienceConditions = audienceConditions;
        this.variations = variations;
        this.trafficAllocation = trafficAllocation;
        this.includedFlags = includedFlags == null ? Collections.emptyList() : Collections.unmodifiableList(includedFlags);
        this.excludedFlags = excludedFlags == null ? Collections.emptyList() : Collections.unmodifiableList(excludedFlags);
        this.variationKeyToVariationMap = ProjectConfigUtils.generateNameMapping(this.variations);
        this.variationIdToVariationMap = ProjectConfigUtils.generateIdMapping(this.variations);
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getStatus() {
        return status;
    }

    public String getLayerId() {
        return layerId;
    }

    public List<String> getAudienceIds() {
        return audienceIds;
    }

    public Condition getAudienceConditions() {
        return audienceConditions;
    }

    public List<Variation> getVariations() {
        return variations;
    }

    public Map<String, Variation> getVariationKeyToVariationMap() {
        return variationKeyToVariationMap;
    }

    public Map<String, Variation> getVariationIdToVariationMap() {
        return variationIdToVariationMap;
    }

    public List<TrafficAllocation> getTrafficAllocation() {
        return trafficAllocation;
    }

    public String getGroupId() {
        return "";
    }

    public List<String> getIncludedFlags() {
        return includedFlags;
    }

    public List<String> getExcludedFlags() {
        return excludedFlags;
    }

    public boolean isActive() {
        return status.equals(Holdout.HoldoutStatus.RUNNING.toString());
    }

    public boolean isRunning() {
        return status.equals(Holdout.HoldoutStatus.RUNNING.toString());
    }

    @Override
    public String toString() {
        return "Holdout {"
                + "id='" + id + '\''
                + ", key='" + key + '\''
                + ", status='" + status + '\''
                + ", audienceIds=" + audienceIds
                + ", audienceConditions=" + audienceConditions
                + ", variations=" + variations
                + ", variationKeyToVariationMap=" + variationKeyToVariationMap
                + ", trafficAllocation=" + trafficAllocation
                + '}';
    }
}

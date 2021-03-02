/**
 *
 *    Copyright 2018-2021, Optimizely and contributors
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
package com.optimizely.ab.event.internal.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.optimizely.ab.annotations.VisibleForTesting;

public class Decision {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("campaign_id")
    String campaignId;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("experiment_id")
    String experimentId;
    @JsonProperty("variation_id")
    String variationId;
    @JsonProperty("is_campaign_holdback")
    boolean isCampaignHoldback;
    @JsonProperty("metadata")
    DecisionMetadata metadata;

    @VisibleForTesting
    public Decision() {
    }

    public Decision(String campaignId, String experimentId, String variationId, boolean isCampaignHoldback, DecisionMetadata metadata) {
        this.campaignId = campaignId;
        this.experimentId = experimentId;
        this.variationId = variationId;
        this.isCampaignHoldback = isCampaignHoldback;
        this.metadata = metadata;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public String getVariationId() {
        return variationId;
    }

    public boolean getIsCampaignHoldback() {
        return isCampaignHoldback;
    }

    public DecisionMetadata getMetadata() { return metadata; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Decision that = (Decision) o;

        if (isCampaignHoldback != that.isCampaignHoldback) return false;
        if (!campaignId.equals(that.campaignId)) return false;
        if (!experimentId.equals(that.experimentId)) return false;
        return variationId.equals(that.variationId);
    }

    @Override
    public int hashCode() {
        int result = campaignId.hashCode();
        result = 31 * result + experimentId.hashCode();
        result = 31 * result + variationId.hashCode();
        result = 31 * result + metadata.hashCode();
        result = 31 * result + (isCampaignHoldback ? 1 : 0);
        return result;
    }

    public static class Builder {

        private String campaignId;
        private String experimentId;
        private String variationId;
        private boolean isCampaignHoldback;
        private DecisionMetadata metadata;

        public Builder setCampaignId(String campaignId) {
            this.campaignId = campaignId;
            return this;
        }

        public Builder setExperimentId(String experimentId) {
            this.experimentId = experimentId;
            return this;
        }

        public Builder setMetadata(DecisionMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder setVariationId(String variationId) {
            this.variationId = variationId;
            return this;
        }

        public Builder setIsCampaignHoldback(boolean isCampaignHoldback) {
            this.isCampaignHoldback = isCampaignHoldback;
            return this;
        }

        public Decision build() {
            return new Decision(campaignId, experimentId, variationId, isCampaignHoldback, metadata);
        }
    }
}

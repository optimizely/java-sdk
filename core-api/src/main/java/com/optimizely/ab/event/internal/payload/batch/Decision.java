/*
 * Copyright 2016-2017, Optimizely and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.optimizely.ab.event.internal.payload.batch;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Decision {

    @Nonnull private String campaignId;
    @Nullable private String experimentId;
    @Nullable private Boolean isCampaignHoldback;
    @Nullable private String variationId;

    public Decision(@Nonnull String campaignId) {
        this.campaignId = campaignId;

    }

    public Decision(@Nonnull String campaignId, @Nullable String experimentId, @Nullable Boolean isCampaignHoldback,
                    @Nullable String variationId) {

        this.campaignId = campaignId;
        this.experimentId = experimentId;
        this.isCampaignHoldback = isCampaignHoldback;
        this.variationId = variationId;
    }

    @Nonnull
    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(@Nonnull String campaignId) {
        this.campaignId = campaignId;
    }

    @Nullable
    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(@Nullable String experimentId) {
        this.experimentId = experimentId;
    }

    @Nullable
    public Boolean getCampaignHoldback() {
        return isCampaignHoldback;
    }

    public void setCampaignHoldback(@Nullable Boolean campaignHoldback) {
        isCampaignHoldback = campaignHoldback;
    }

    @Nullable
    public String getVariationId() {
        return variationId;
    }

    public void setVariationId(@Nullable String variationId) {
        this.variationId = variationId;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Decision decision = (Decision) o;

        if (!campaignId.equals(decision.campaignId)) return false;
        if (experimentId != null ? !experimentId.equals(decision.experimentId) : decision.experimentId != null)
            return false;
        if (isCampaignHoldback != null ? !isCampaignHoldback.equals(decision.isCampaignHoldback) :
                decision.isCampaignHoldback != null)
            return false;
        return variationId != null ? variationId.equals(decision.variationId) : decision.variationId == null;
    }

    @Override
    public int hashCode() {
        int result = campaignId.hashCode();
        result = 31 * result + (experimentId != null ? experimentId.hashCode() : 0);
        result = 31 * result + (isCampaignHoldback != null ? isCampaignHoldback.hashCode() : 0);
        result = 31 * result + (variationId != null ? variationId.hashCode() : 0);
        return result;
    }
}

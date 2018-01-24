package com.optimizely.ab.event.internal.payload;

public class DecisionV3 {
    String campaignId;
    String experimentId;
    String variationId;
    boolean isCampaignHoldback;

    public DecisionV3(String campaignId, String experimentId, String variationId, boolean isCampaignHoldback) {
        this.campaignId = campaignId;
        this.experimentId = experimentId;
        this.variationId = variationId;
        this.isCampaignHoldback = isCampaignHoldback;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public String getVariationId() {
        return variationId;
    }

    public void setVariationId(String variationId) {
        this.variationId = variationId;
    }

    public boolean getIsCampaignHoldback() {
        return isCampaignHoldback;
    }

    public void setIsCampaignHoldback(boolean campaignHoldback) {
        isCampaignHoldback = campaignHoldback;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DecisionV3 that = (DecisionV3) o;

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
        result = 31 * result + (isCampaignHoldback ? 1 : 0);
        return result;
    }
}

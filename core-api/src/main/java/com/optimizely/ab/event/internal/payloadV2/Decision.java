package com.optimizely.ab.event.internal.payloadV2;

public class Decision {

    private String variationId;
    private boolean isLayerHoldback;
    private String experimentId;

    public Decision() {}

    public Decision(String variationId, boolean isLayerHoldback, String experimentId) {
        this.variationId = variationId;
        this.isLayerHoldback = isLayerHoldback;
        this.experimentId = experimentId;
    }

    public String getVariationId() {
        return variationId;
    }

    public void setVariationId(String variationId) {
        this.variationId = variationId;
    }

    public boolean getIsLayerHoldback() {
        return isLayerHoldback;
    }

    public void setIsLayerHoldback(boolean layerHoldback) {
        this.isLayerHoldback = layerHoldback;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Decision))
            return false;

        Decision otherDecision = (Decision)other;

        return variationId.equals(otherDecision.getVariationId()) &&
               isLayerHoldback == otherDecision.getIsLayerHoldback() &&
               experimentId.equals(otherDecision.getExperimentId());
    }

    @Override
    public int hashCode() {
        int result = variationId.hashCode();
        result = 31 * result + (isLayerHoldback ? 1 : 0);
        result = 31 * result + experimentId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Decision{" +
                "variationId='" + variationId + '\'' +
                ", isLayerHoldback=" + isLayerHoldback +
                ", experimentId='" + experimentId + '\'' +
                '}';
    }
}

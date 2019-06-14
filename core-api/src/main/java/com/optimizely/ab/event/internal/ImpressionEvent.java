package com.optimizely.ab.event.internal;

public class ImpressionEvent {
    public final String layerId;
    public final String experimentId;
    public final String experimentKey;
    public final String variationKey;
    public final String variationId;

    private ImpressionEvent(String layerId, String experimentId, String experimentKey, String variationKey, String variationId) {
        this.layerId = layerId;
        this.experimentId = experimentId;
        this.experimentKey = experimentKey;
        this.variationKey = variationKey;
        this.variationId = variationId;
    }

    public String getLayerId() {
        return layerId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public String getExperimentKey() {
        return experimentKey;
    }

    public String getVariationKey() {
        return variationKey;
    }

    public String getVariationId() {
        return variationId;
    }

    public static class Builder {

        private String layerId;
        private String experimentId;
        private String experimentKey;
        private String variationKey;
        private String variationId;

        public Builder withLayerId(String layerId) {
            this.layerId = layerId;
            return this;
        }

        public Builder withExperimentId(String experimentId) {
            this.experimentId = experimentId;
            return this;
        }

        public Builder withExperimentKey(String experimentKey) {
            this.experimentKey = experimentKey;
            return this;
        }

        public Builder withVariationKey(String variationKey) {
            this.variationKey = variationKey;
            return this;
        }

        public Builder withVariationId(String variationId) {
            this.variationId = variationId;
            return this;
        }

        public ImpressionEvent build() {
            return new ImpressionEvent(layerId, experimentId, experimentKey, variationKey, variationId);
        }
    }
}

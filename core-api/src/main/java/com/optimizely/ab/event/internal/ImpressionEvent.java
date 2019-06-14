/**
 *
 *    Copyright 2019, Optimizely and contributors
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
package com.optimizely.ab.event.internal;


/**
 * ImpressionEvent encapsulates information specific to conversion events.
 */
public class ImpressionEvent {
    private final String layerId;
    private final String experimentId;
    private final String experimentKey;
    private final String variationKey;
    private final String variationId;

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

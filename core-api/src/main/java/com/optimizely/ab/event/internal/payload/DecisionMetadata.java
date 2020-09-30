/**
 *
 *    Copyright 2020, Optimizely and contributors
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.optimizely.ab.annotations.VisibleForTesting;

public class DecisionMetadata {
    @JsonProperty("flag_type")
    String flagType;
    @JsonProperty("flag_key")
    String flagKey;
    @JsonProperty("variation_key")
    String variationKey;

    @VisibleForTesting
    public DecisionMetadata() {
    }

    public DecisionMetadata(String flagType, String flagKey, String variationKey) {
        this.flagType = flagType;
        this.flagKey = flagKey;
        this.variationKey = variationKey;
    }

    public String getFlagType() {
        return flagType;
    }

    public String getFlagKey() { return flagKey; }

    public String getVariationKey() { return variationKey; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DecisionMetadata that = (DecisionMetadata) o;

        if (!flagType.equals(that.flagType)) return false;
        if (!flagKey.equals(that.flagKey)) return false;
        return variationKey.equals(that.variationKey);
    }

    @Override
    public int hashCode() {
        int result = flagType.hashCode();
        result = 31 * result + flagKey.hashCode();
        result = 31 * result + variationKey.hashCode();
        return result;
    }

    public static class Builder {

        private String flagType;
        private String flagKey;
        private String variationKey;

        public Builder setFlagType(String flagType) {
            this.flagType = flagType;
            return this;
        }

        public Builder setFlagKey(String flagKey) {
            this.flagKey = flagKey;
            return this;
        }

        public Builder setVariationKey(String variationKey) {
            this.variationKey = variationKey;
            return this;
        }

        public DecisionMetadata build() {
            return new DecisionMetadata(flagType, flagKey, variationKey);
        }
    }
}

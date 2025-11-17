/**
 * Copyright 2025, Optimizely
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.cmab.service;

import java.util.Objects;

public class CmabDecision {
    private final String variationId;
    private final String cmabUUID;

    public CmabDecision(String variationId, String cmabUUID) {
        this.variationId = variationId;
        this.cmabUUID = cmabUUID;
    }

    public String getVariationId() {
        return variationId;
    }

    public String getCmabUUID() {
        return cmabUUID;
    }

    @Override
    public String toString() {
        return "CmabDecision{" +
            "variationId='" + variationId + '\'' +
            ", cmabUUID='" + cmabUUID + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CmabDecision that = (CmabDecision) o;
        return Objects.equals(variationId, that.variationId) &&
            Objects.equals(cmabUUID, that.cmabUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variationId, cmabUUID);
    }
}

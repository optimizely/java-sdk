package com.optimizely.ab.cmab.service;

import java.util.Objects;

public class CmabCacheValue {
    private final String attributesHash;
    private final String variationId;
    private final String cmabUUID;

    public CmabCacheValue(String attributesHash, String variationId, String cmabUUID) {
        this.attributesHash = attributesHash;
        this.variationId = variationId;
        this.cmabUUID = cmabUUID;
    }

    public String getAttributesHash() {
        return attributesHash;
    }

    public String getVariationId() {
        return variationId;
    }

    public String getCmabUuid() {
        return cmabUUID;
    }

    @Override
    public String toString() {
        return "CmabCacheValue{" +
            "attributesHash='" + attributesHash + '\'' +
            ", variationId='" + variationId + '\'' +
            ", cmabUuid='" + cmabUUID + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CmabCacheValue that = (CmabCacheValue) o;
        return Objects.equals(attributesHash, that.attributesHash) &&
            Objects.equals(variationId, that.variationId) &&
            Objects.equals(cmabUUID, that.cmabUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributesHash, variationId, cmabUUID);
    }
}
package com.optimizely.ab.event.internal.payloadV2;

import javax.annotation.Nonnull;

import java.util.List;

public class Impression extends V2Event {

    private String visitorId;
    private long timestamp;
    private boolean isGlobalHoldback;
    private String projectId;
    private Decision decision;
    private String layerId;
    private String accountId;
    private List<Feature> userFeatures;

    public Impression() { }

    public Impression(@Nonnull String visitorId,
                      @Nonnull long timestamp,
                      @Nonnull boolean isGlobalHoldback,
                      @Nonnull String projectId,
                      @Nonnull Decision decision,
                      @Nonnull String layerId,
                      @Nonnull String accountId,
                      @Nonnull List<Feature> userFeatures) {
        this.visitorId = visitorId;
        this.timestamp = timestamp;
        this.isGlobalHoldback = isGlobalHoldback;
        this.projectId = projectId;
        this.decision = decision;
        this.layerId = layerId;
        this.accountId = accountId;
        this.userFeatures = userFeatures;
    }

    public String getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean getIsGlobalHoldback() {
        return isGlobalHoldback;
    }

    public void setIsGlobalHoldback(boolean globalHoldback) {
        this.isGlobalHoldback = globalHoldback;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public String getLayerId() {
        return layerId;
    }

    public void setLayerId(String layerId) {
        this.layerId = layerId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public List<Feature> getUserFeatures() {
        return userFeatures;
    }

    public void setUserFeatures(List<Feature> userFeatures) {
        this.userFeatures = userFeatures;
    }

    @Override
    public String toString() {
        return "Impression{" +
                "visitorId='" + visitorId + '\'' +
                ", timestamp=" + timestamp +
                ", isGlobalHoldback=" + isGlobalHoldback +
                ", projectId='" + projectId + '\'' +
                ", decision=" + decision +
                ", layerId='" + layerId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", userFeatures=" + userFeatures +
                '}';
    }
}

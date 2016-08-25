package com.optimizely.ab.event.internal.payloadV2;

import javax.annotation.Nonnull;

import java.util.List;

public class Conversion extends V2Event {

    private String visitorId;
    private long timestamp;
    private String projectId;
    private String accountId;
    private List<Feature> userFeatures;
    private List<LayerState> layerStates;
    private String eventEntityId;
    private String eventName;
    private List<EventMetric> eventMetrics;
    private List<Feature> eventFeatures;
    private boolean isGlobalHoldback;

    public Conversion() { }

    public Conversion(@Nonnull String visitorId,
                      @Nonnull long timestamp,
                      @Nonnull String projectId,
                      @Nonnull String accountId,
                      @Nonnull List<Feature> userFeatures,
                      @Nonnull List<LayerState> layerStates,
                      @Nonnull String eventEntityId,
                      @Nonnull String eventName,
                      @Nonnull List<EventMetric> eventMetrics,
                      @Nonnull List<Feature> eventFeatures,
                      @Nonnull boolean isGlobalHoldback) {
        this.visitorId = visitorId;
        this.timestamp = timestamp;
        this.projectId = projectId;
        this.accountId = accountId;
        this.userFeatures = userFeatures;
        this.layerStates = layerStates;
        this.eventEntityId = eventEntityId;
        this.eventName = eventName;
        this.eventMetrics = eventMetrics;
        this.eventFeatures = eventFeatures;
        this.isGlobalHoldback = isGlobalHoldback;
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

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
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

    public List<LayerState> getLayerStates() {
        return layerStates;
    }

    public void setLayerStates(List<LayerState> layerStates) {
        this.layerStates = layerStates;
    }

    public String getEventEntityId() {
        return eventEntityId;
    }

    public void setEventEntityId(String eventEntityId) {
        this.eventEntityId = eventEntityId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public List<EventMetric> getEventMetrics() {
        return eventMetrics;
    }

    public void setEventMetrics(List<EventMetric> eventMetrics) {
        this.eventMetrics = eventMetrics;
    }

    public List<Feature> getEventFeatures() {
        return eventFeatures;
    }

    public void setEventFeatures(List<Feature> eventFeatures) {
        this.eventFeatures = eventFeatures;
    }

    public boolean getIsGlobalHoldback() {
        return isGlobalHoldback;
    }

    public void setIsGlobalHoldback(boolean globalHoldback) {
        this.isGlobalHoldback = globalHoldback;
    }

    @Override
    public String toString() {
        return "Conversion{" +
                "visitorId='" + visitorId + '\'' +
                ", timestamp=" + timestamp +
                ", projectId='" + projectId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", userFeatures=" + userFeatures +
                ", layerStates=" + layerStates +
                ", eventEntityId='" + eventEntityId + '\'' +
                ", eventName='" + eventName + '\'' +
                ", eventMetrics=" + eventMetrics +
                ", eventFeatures=" + eventFeatures +
                ", isGlobalHoldback=" + isGlobalHoldback +
                '}';
    }
}

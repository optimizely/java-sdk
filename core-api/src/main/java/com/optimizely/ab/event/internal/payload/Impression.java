/**
 *
 *    Copyright 2016, Optimizely
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

import java.util.List;

public class Impression extends Event {

    private String visitorId;
    private long timestamp;
    private boolean isGlobalHoldback;
    private String projectId;
    private Decision decision;
    private String layerId;
    private String accountId;
    private List<Feature> userFeatures;

    public Impression() { }

    public Impression(String visitorId, long timestamp, boolean isGlobalHoldback, String projectId, Decision decision,
                      String layerId, String accountId, List<Feature> userFeatures) {
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

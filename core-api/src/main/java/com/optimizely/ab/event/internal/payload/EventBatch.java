/**
 *
 *    Copyright 2018-2019, Optimizely and contributors
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
import com.fasterxml.jackson.annotation.JsonValue;
import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.api.EventContext;
import com.optimizely.ab.event.internal.BuildVersionInfo;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class EventBatch {
    public enum ClientEngine {
        JAVA_SDK("java-sdk"),
        ANDROID_SDK("android-sdk"),
        ANDROID_TV_SDK("android-tv-sdk");

        private final String clientEngineValue;

        ClientEngine(String clientEngineValue) {
            this.clientEngineValue = clientEngineValue;
        }

        @JsonValue
        public String getClientEngineValue() {
            return clientEngineValue;
        }
    }

    @JsonProperty("account_id")
    String accountId;
    List<Visitor> visitors;
    @JsonProperty("enrich_decisions")
    Boolean enrichDecisions;
    @JsonProperty("anonymize_ip")
    Boolean anonymizeIp;
    @JsonProperty("client_name")
    String clientName;
    @JsonProperty("client_version")
    String clientVersion;
    @JsonProperty("project_id")
    String projectId;
    String revision;

    public static EventBatch.Builder builder() {
        return new Builder();
    }

    public static EventBatch.Builder builder(EventContext context) {
        return builder().context(context);
    }

    public static EventBatch.Builder builder(EventBatch eventBatch) {
        return new Builder(eventBatch);
    }

    public static EventBatch concat(List<EventBatch> eventBatches) {
        if (eventBatches == null || eventBatches.isEmpty()) {
            return null;
        }
        EventBatch first = eventBatches.get(0);
        if (eventBatches.size() == 1) {
            return first;
        }
        Builder builder = builder(first);
        ListIterator<EventBatch> it = eventBatches.listIterator(1);
        do {
            EventBatch next = it.next();
            builder.addVisitors(next.getVisitors());
        } while (it.hasNext());
        return builder.build();
    }

    @VisibleForTesting
    public EventBatch() {
    }

    private EventBatch(String clientName, String clientVersion, String accountId, List<Visitor> visitors, Boolean anonymizeIp, String projectId, String revision) {
        this.accountId = accountId;
        this.visitors = visitors;
        this.enrichDecisions = true;
        this.anonymizeIp = anonymizeIp;
        this.clientName = clientName;
        this.clientVersion = clientVersion;
        this.projectId = projectId;
        this.revision = revision;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public List<Visitor> getVisitors() {
        return visitors;
    }

    public void setVisitors(List<Visitor> visitors) {
        this.visitors = visitors;
    }

    public Boolean getEnrichDecisions() { return enrichDecisions; }

    public void setEnrichDecisions(Boolean enrichDecisions) {
        this.enrichDecisions = enrichDecisions;
    }

    public Boolean getAnonymizeIp() {
        return anonymizeIp;
    }

    public void setAnonymizeIp(Boolean anonymizeIp) {
        this.anonymizeIp = anonymizeIp;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventBatch that = (EventBatch) o;

        if (!accountId.equals(that.accountId)) return false;
        if (!visitors.equals(that.visitors)) return false;
        if (anonymizeIp != null ? !anonymizeIp.equals(that.anonymizeIp) : that.anonymizeIp != null) return false;
        if (clientName != null ? !clientName.equals(that.clientName) : that.clientName != null) return false;
        if (clientVersion != null ? !clientVersion.equals(that.clientVersion) : that.clientVersion != null)
            return false;
        if (projectId != null ? !projectId.equals(that.projectId) : that.projectId != null) return false;
        return revision != null ? revision.equals(that.revision) : that.revision == null;
    }

    @Override
    public int hashCode() {
        int result = accountId.hashCode();
        result = 31 * result + visitors.hashCode();
        result = 31 * result + (anonymizeIp != null ? anonymizeIp.hashCode() : 0);
        result = 31 * result + (clientName != null ? clientName.hashCode() : 0);
        result = 31 * result + (clientVersion != null ? clientVersion.hashCode() : 0);
        result = 31 * result + (projectId != null ? projectId.hashCode() : 0);
        result = 31 * result + (revision != null ? revision.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("EventBatch{");
        sb.append("accountId='").append(accountId).append('\'');
        sb.append(", visitors=").append(visitors);
        sb.append(", enrichDecisions=").append(enrichDecisions);
        sb.append(", anonymizeIp=").append(anonymizeIp);
        sb.append(", clientName='").append(clientName).append('\'');
        sb.append(", clientVersion='").append(clientVersion).append('\'');
        sb.append(", projectId='").append(projectId).append('\'');
        sb.append(", revision='").append(revision).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {

        private String clientName;
        private String clientVersion;
        private String accountId;
        private List<Visitor> visitors;
        private Boolean anonymizeIp;
        private String projectId;
        private String revision;

        public Builder() {
            this.clientName = ClientEngine.JAVA_SDK.getClientEngineValue();
            this.clientVersion = BuildVersionInfo.VERSION;
        }

        Builder(EventBatch instance) {
            from(instance);
        }

        public Builder from(EventBatch instance) {
            return setAccountId(instance.getAccountId())
                .setProjectId(instance.getProjectId())
                .setClientVersion(instance.getClientVersion())
                .setClientName(instance.getClientName())
                .setRevision(instance.getRevision())
                .setAnonymizeIp(instance.getAnonymizeIp())
                .setVisitors(new ArrayList<>(instance.getVisitors()));
        }

        public Builder context(EventContext context) {
            return setAccountId(context.getAccountId())
                .setProjectId(context.getProjectId())
                .setRevision(context.getRevision())
                .setAnonymizeIp(context.getAnonymizeIp());
        }

        public Builder setClientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public Builder setClientVersion(String clientVersion) {
            this.clientVersion = clientVersion;
            return this;
        }

        public Builder setAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder setVisitors(List<Visitor> visitors) {
            this.visitors = visitors;
            return this;
        }

        public Builder addVisitor(Visitor visitor) {
            if (this.visitors == null) {
                this.visitors = new ArrayList<>();
            }
            this.visitors.add(visitor);
            return this;
        }

        public Builder addVisitors(List<Visitor> visitors) {
            if (this.visitors == null) {
                this.visitors = new ArrayList<>();
            }
            this.visitors.addAll(visitors);
            return this;
        }

        public Builder setAnonymizeIp(Boolean anonymizeIp) {
            this.anonymizeIp = anonymizeIp;
            return this;
        }

        public Builder setProjectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder setRevision(String revision) {
            this.revision = revision;
            return this;
        }

        @Nonnull
        public EventBatch build() {
            return new EventBatch(clientName, clientVersion, accountId, visitors, anonymizeIp, projectId, revision);
        }
    }

}

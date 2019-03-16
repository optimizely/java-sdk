/**
 *    Copyright 2019, Optimizely Inc. and contributors
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

import com.optimizely.ab.api.EventContext;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.config.ProjectConfig;

import java.util.Objects;

// TODO make package-private
public class EventContextImpl implements EventContext {
    private final String accountId;
    private final String projectId;
    private final String clientName;
    private final String clientVersion;
    private final String revision;
    private final Boolean anonymizeIp;
    private final Boolean botFiltering;

    private EventContextImpl(Builder builder) {
        accountId = builder.accountId;
        projectId = builder.projectId;
        clientName = builder.clientName != null ? builder.clientName : "java-sdk";
        clientVersion = builder.clientVersion != null ? builder.clientVersion : BuildVersionInfo.VERSION;
        revision = builder.revision;
        anonymizeIp = builder.anonymizeIp;
        botFiltering = builder.botFiltering;
    }

    // TODO make package-private
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public String getProjectId() {
        return projectId;
    }

    @Override
    public String getClientName() {
        return clientName;
    }

    @Override
    public String getClientVersion() {
        return clientVersion;
    }

    @Override
    public String getRevision() {
        return revision;
    }

    @Override
    public Boolean getAnonymizeIp() {
        return anonymizeIp;
    }

//    @Override
    public Boolean getBotFiltering() {
        return botFiltering;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventContextImpl)) return false;
        final EventContextImpl that = (EventContextImpl) o;
        return Objects.equals(accountId, that.accountId) &&
            Objects.equals(projectId, that.projectId) &&
            Objects.equals(clientName, that.clientName) &&
            Objects.equals(clientVersion, that.clientVersion) &&
            Objects.equals(revision, that.revision) &&
            Objects.equals(anonymizeIp, that.anonymizeIp) &&
            Objects.equals(botFiltering, that.botFiltering);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, projectId, clientName, clientVersion, revision, anonymizeIp, botFiltering);
    }

    public static final class Builder {
        private String accountId;
        private String projectId;
        private String clientName;
        private String clientVersion;
        private String revision;
        private Boolean anonymizeIp;
        private Boolean botFiltering;

        private Builder() {
        }

        public Builder from(EventContext inst) {
            return accountId(inst.getAccountId())
                .client(inst.getClientName(), inst.getClientVersion())
                .projectId(inst.getProjectId())
                .revision(inst.getRevision())
                .anonymizeIp(inst.getAnonymizeIp())
                .botFiltering(inst.getBotFiltering());
        }

        public Builder from(ProjectConfig projectConfig) {
            return accountId(projectConfig.getAccountId())
                .projectId(projectConfig.getProjectId())
                .revision(projectConfig.getRevision())
                .anonymizeIp(projectConfig.getAnonymizeIP());
        }

        public Builder client(String name, String version) {
            this.clientName = Assert.notNull(name, "name");
            this.clientVersion = Assert.notNull(version, "version");
            return this;
        }

        public Builder accountId(String val) {
            this.accountId = Assert.notNull(val, "accountId");
            return this;
        }

        public Builder projectId(String val) {
            this.projectId = Assert.notNull(val, "projectId");
            return this;
        }

        public Builder revision(String val) {
            this.revision = Assert.notNull(val, "revision");
            return this;
        }

        public Builder anonymizeIp(Boolean val) {
            this.anonymizeIp = val;
            return this;
        }

        public Builder botFiltering(Boolean val) {
            this.botFiltering = val;
            return this;
        }

        public EventContextImpl build() {
            return new EventContextImpl(this);
        }
    }
}

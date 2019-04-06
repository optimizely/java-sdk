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
import com.optimizely.ab.event.internal.payload.EventBatch;

import java.util.Objects;

/**
 * A mutable implementation of {@link EventContext}
 */
class EventContextImpl implements EventContext {
    private String accountId;
    private String projectId;
    private String revision;
    private Boolean anonymizeIp;
    private Boolean botFiltering;
    private String clientName;
    private String clientVersion;

    private static String defaultClientName() {
        // TODO get this from a more canonical class than EventBatch
        return EventBatch.ClientEngine.JAVA_SDK.getClientEngineValue();
    }

    private static String defaltClientVersion() {
        return BuildVersionInfo.VERSION;
    }

    EventContextImpl(ProjectConfig projectConfig) {
        this.accountId = projectConfig.getAccountId();
        this.projectId = projectConfig.getProjectId();
        this.revision = projectConfig.getRevision();
        this.anonymizeIp = projectConfig.getAnonymizeIP();
        this.botFiltering = projectConfig.getBotFiltering();
        this.clientName = defaultClientName();
        this.clientVersion = defaltClientVersion();
    }

    EventContextImpl(
        String accountId,
        String projectId,
        String revision,
        Boolean anonymizeIp,
        Boolean botFiltering,
        String clientName,
        String clientVersion
    ) {
        this.accountId = accountId;
        this.projectId = projectId;
        this.revision = revision;
        this.anonymizeIp = anonymizeIp;
        this.botFiltering = botFiltering;
        this.clientName = (clientName != null) ? clientName : defaultClientName();
        this.clientVersion = (clientVersion != null) ? clientVersion : defaltClientVersion();
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = Assert.notNull(accountId, "accountId");
    }

    @Override
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = Assert.notNull(projectId, "projectId");
    }

    @Override
    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = Assert.notNull(revision, "revision");
    }

    @Override
    public Boolean getAnonymizeIp() {
        return anonymizeIp;
    }

    public void setAnonymizeIp(Boolean anonymizeIp) {
        this.anonymizeIp = anonymizeIp;
    }

    @Override
    public Boolean getBotFiltering() {
        return botFiltering;
    }

    public void setBotFiltering(Boolean botFiltering) {
        this.botFiltering = botFiltering;
    }

    @Override
    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = Assert.notNull(clientName, "clientName");
    }

    @Override
    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = Assert.notNull(clientVersion, "clientVersion");
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
}

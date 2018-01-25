package com.optimizely.ab.event.internal.payload;

import com.optimizely.ab.event.internal.BuildVersionInfo;

import java.util.List;

public class EventBatch {
    String accountId;
    List<Visitor> visitors;
    Boolean anonymizeIp;
    String clientName = Event.ClientEngine.JAVA_SDK.getClientEngineValue();
    String clientVersion = BuildVersionInfo.VERSION;
    String projectId;
    String revision;

    public EventBatch() {

    }

    public EventBatch(String accountId, List<Visitor> visitors, Boolean anonymizeIp, String projectId, String revision) {
        this.accountId = accountId;
        this.visitors = visitors;
        this.anonymizeIp = anonymizeIp;
        this.projectId = projectId;
        this.revision = revision;
    }

    public EventBatch(String clientName, String clientVersion, String accountId, List<Visitor> visitors, Boolean anonymizeIp, String projectId, String revision) {
        this.accountId = accountId;
        this.visitors = visitors;
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
}

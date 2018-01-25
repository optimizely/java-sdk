package com.optimizely.ab.event.internal.payload;

import java.util.List;

public class Visitor {
    String visitorId;
    String sessionId;
    List<Attribute> attributes;
    List<Snapshot> snapshots;

    public Visitor() {

    }

    public Visitor(String visitorId, String sessionId, List<Attribute> attributes, List<Snapshot> snapshots) {
        this.visitorId = visitorId;
        this.sessionId = sessionId;
        this.attributes = attributes;
        this.snapshots = snapshots;
    }

    public String getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public List<Snapshot> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<Snapshot> snapshots) {
        this.snapshots = snapshots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Visitor visitor = (Visitor) o;

        if (!visitorId.equals(visitor.visitorId)) return false;
        if (sessionId != null ? !sessionId.equals(visitor.sessionId) : visitor.sessionId != null) return false;
        if (attributes != null ? !attributes.equals(visitor.attributes) : visitor.attributes != null) return false;
        return snapshots.equals(visitor.snapshots);
    }

    @Override
    public int hashCode() {
        int result = visitorId.hashCode();
        result = 31 * result + (sessionId != null ? sessionId.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + snapshots.hashCode();
        return result;
    }
}

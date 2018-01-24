package com.optimizely.ab.event.internal.payload;

import java.util.List;

public class Snapshot {
    List<DecisionV3> decisions;
    List<EventV3> events;
    long activationTimestamp;

    public Snapshot(List<DecisionV3> decisions, List<EventV3> events) {
        this.decisions = decisions;
        this.events = events;
    }

    public List<DecisionV3> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<DecisionV3> decisions) {
        this.decisions = decisions;
    }

    public List<EventV3> getEvents() {
        return events;
    }

    public void setEvents(List<EventV3> events) {
        this.events = events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Snapshot snapshot = (Snapshot) o;

        if (activationTimestamp != snapshot.activationTimestamp) return false;
        if (!decisions.equals(snapshot.decisions)) return false;
        return events.equals(snapshot.events);
    }

    @Override
    public int hashCode() {
        int result = decisions.hashCode();
        result = 31 * result + events.hashCode();
        result = 31 * result + (int) (activationTimestamp ^ (activationTimestamp >>> 32));
        return result;
    }
}

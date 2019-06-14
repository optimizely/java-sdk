package com.optimizely.ab.event.internal;

import java.util.Map;

public class ConversionEvent {

    private final String eventId;
    private final String eventKey;

    private final Number revenue;
    private final Number value;
    private final Map<String, ?> Tags;

    private ConversionEvent(String eventId, String eventKey, Number revenue, Number value, Map<String, ?> tags) {
        this.eventId = eventId;
        this.eventKey = eventKey;
        this.revenue = revenue;
        this.value = value;
        Tags = tags;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventKey() {
        return eventKey;
    }

    public Number getRevenue() {
        return revenue;
    }

    public Number getValue() {
        return value;
    }

    public Map<String, ?> getTags() {
        return Tags;
    }

    public static class Builder {

        private String eventId;
        private String eventKey;
        private Number revenue;
        private Number value;
        private Map<String, ?> tags;

        public Builder withEventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder withEventKey(String eventKey) {
            this.eventKey = eventKey;
            return this;
        }

        public Builder withRevenue(Number revenue) {
            this.revenue = revenue;
            return this;
        }

        public Builder withValue(Number value) {
            this.value = value;
            return this;
        }

        public Builder withTags(Map<String, ?> tags) {
            this.tags = tags;
            return this;
        }

        public ConversionEvent build() {
            return new ConversionEvent(eventId, eventKey, revenue, value, tags);
        }
    }
}

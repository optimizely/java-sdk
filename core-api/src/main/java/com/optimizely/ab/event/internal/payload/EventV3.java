package com.optimizely.ab.event.internal.payload;

import java.util.Map;

public class EventV3 {
    long timestamp;
    String uuid;
    String entityId;
    String key;
    Number quantity;
    Number revenue;
    Map<String, ?> tags;
    String type;
    Number value;

    public EventV3() {

    }

    public EventV3(long timestamp, String uuid, String entityId, String key, Number quantity,
                   Number revenue, Map<String, ?> tags, String type, Number value) {
        this.timestamp = timestamp;
        this.uuid = uuid;
        this.entityId = entityId;
        this.key = key;
        this.quantity = quantity;
        this.revenue = revenue;
        this.tags = tags;
        this.type = type;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Number getQuantity() {
        return quantity;
    }

    public void setQuantity(Number quantity) {
        this.quantity = quantity;
    }

    public Number getRevenue() {
        return revenue;
    }

    public void setRevenue(Number revenue) {
        this.revenue = revenue;
    }

    public Map<String, ?> getTags() {
        return tags;
    }

    public void setTags(Map<String, ?> tags) {
        this.tags = tags;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventV3 eventV3 = (EventV3) o;

        if (timestamp != eventV3.timestamp) return false;
        if (!uuid.equals(eventV3.uuid)) return false;
        if (entityId != null ? !entityId.equals(eventV3.entityId) : eventV3.entityId != null) return false;
        if (key != null ? !key.equals(eventV3.key) : eventV3.key != null) return false;
        if (quantity != null ? !quantity.equals(eventV3.quantity) : eventV3.quantity != null) return false;
        if (revenue != null ? !revenue.equals(eventV3.revenue) : eventV3.revenue != null) return false;
        if (tags != null ? !tags.equals(eventV3.tags) : eventV3.tags != null) return false;
        if (!type.equals(eventV3.type)) return false;
        return value != null ? value.equals(eventV3.value) : eventV3.value == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + uuid.hashCode();
        result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (quantity != null ? quantity.hashCode() : 0);
        result = 31 * result + (revenue != null ? revenue.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + type.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

}

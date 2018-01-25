package com.optimizely.ab.event.internal.payload;

public class Attribute {
    public static final String CUSTOM_ATTRIBUTE_TYPE = "custom";
    public static final String CUSTOM_EVENT_TYPE = "custom";

    String entityId;
    String key;
    String type;
    String value;

    public Attribute() {

    }

    public Attribute(String entityId, String key, String type, String value) {
        this.entityId = entityId;
        this.key = key;
        this.type = type;
        this.value = value;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (!entityId.equals(attribute.entityId)) return false;
        if (!key.equals(attribute.key)) return false;
        if (!type.equals(attribute.type)) return false;
        return value.equals(attribute.value);
    }

    @Override
    public int hashCode() {
        int result = entityId.hashCode();
        result = 31 * result + key.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}

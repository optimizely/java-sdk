package com.optimizely.ab.event.internal.payloadV2;

public class EventMetric {

    public static final String REVENUE_METRIC_TYPE = "revenue";

    private String name;
    private long value;

    public EventMetric() { }

    public EventMetric(String name, long value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }


    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EventMetric))
            return false;

        EventMetric otherEventMetric = (EventMetric)other;

        return name.equals(otherEventMetric.getName()) && value == otherEventMetric.getValue();
    }


    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (int) (value ^ (value >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "EventMetric{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}

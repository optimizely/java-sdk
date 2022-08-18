package com.optimizely.ab.odp;

import java.util.Map;

public class ODPEvent {
    private String type;
    private String action;
    private Map<String, String > identifiers;
    private Map<String, String> data;

    public ODPEvent(String type, String action, Map<String, String> identifiers, Map<String, String> data) {
        this.type = type;
        this.action = action;
        this.identifiers = identifiers;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, String> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Map<String, String> identifiers) {
        this.identifiers = identifiers;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}

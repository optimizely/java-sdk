package com.optimizely.ab.event.internal.serializer;

import com.optimizely.ab.event.internal.payloadV2.V2Event;
import org.json.JSONObject;

class JsonSerializer implements Serializer {

    public <T extends V2Event> String serialize(T payload) {
        JSONObject payloadJsonObject = new JSONObject(payload);
        return payloadJsonObject.toString();
    }
}

package com.optimizely.ab.event.internal.serializer;

import com.google.gson.Gson;

import com.optimizely.ab.event.internal.payloadV2.V2Event;

class GsonSerializer implements Serializer {

    private Gson gson = new Gson();

    public <T extends V2Event> String serialize(T payload) {
        return gson.toJson(payload);
    }
}

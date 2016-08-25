package com.optimizely.ab.event.internal.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.optimizely.ab.event.internal.payloadV2.V2Event;

class JacksonSerializer implements Serializer {

    private final ObjectMapper mapper = new ObjectMapper();

    public <T extends V2Event> String serialize(T payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Unable to serialize payload", e);
        }
    }
}

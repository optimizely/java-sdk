package com.optimizely.ab.event.internal.serializer;

import com.optimizely.ab.event.internal.payloadV2.V2Event;

public interface Serializer {
    <T extends V2Event> String serialize(T payload) throws SerializationException;
}

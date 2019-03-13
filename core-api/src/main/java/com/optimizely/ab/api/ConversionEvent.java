package com.optimizely.ab.api;


import com.optimizely.ab.event.internal.payload.Attribute;

import java.util.List;
import java.util.Map;

public interface ConversionEvent extends Event {
    String getUserId();

    com.optimizely.ab.config.EventType getEvent();

    List<Attribute> getUserAttributes();

    Map<String, ?> getTags();
}

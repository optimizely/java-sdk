package com.optimizely.ab.config.audience;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TypedAudience extends Audience {
    @JsonCreator
    public TypedAudience(@JsonProperty("id") String id,
                         @JsonProperty("name") String name,
                         @JsonProperty("conditions") Condition conditions) {
        super(id, name, conditions);
    }
}
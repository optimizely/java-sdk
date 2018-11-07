package com.optimizely.ab.config.parser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.TypedAudience;
import com.optimizely.ab.config.audience.UserAttribute;

import java.io.IOException;

public class TypedAudienceJacksonDeserializer extends JsonDeserializer<TypedAudience> {
    private ObjectMapper objectMapper;

    public TypedAudienceJacksonDeserializer() {
        this(new ObjectMapper());
    }

    TypedAudienceJacksonDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TypedAudience deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

        String id = node.get("id").textValue();
        String name = node.get("name").textValue();

        JsonNode conditionsJson = node.get("conditions");

        Condition conditions = ConditionJacksonDeserializer.<UserAttribute>parseConditions(UserAttribute.class, objectMapper, conditionsJson);

        return new TypedAudience(id, name, conditions);
    }

}


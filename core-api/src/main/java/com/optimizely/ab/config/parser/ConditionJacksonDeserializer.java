package com.optimizely.ab.config.parser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.internal.ConditionUtils;

import java.io.IOException;
import java.util.List;

public class ConditionJacksonDeserializer extends JsonDeserializer<Condition> {

    @Override
    public Condition deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = parser.getCodec().readTree(parser);

        List<Object> rawObjectList = (List<Object>)mapper.readValue(node.textValue(), List.class);
        Condition conditions = ConditionUtils.parseConditions(rawObjectList);

        return conditions;
    }
}

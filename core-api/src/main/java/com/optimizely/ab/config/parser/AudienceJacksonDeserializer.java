/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.config.parser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.config.audience.*;

import java.io.IOException;

public class AudienceJacksonDeserializer extends JsonDeserializer<Audience> {
    private ObjectMapper objectMapper;

    public AudienceJacksonDeserializer() {
        this(new ObjectMapper());
    }

    AudienceJacksonDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Audience deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

        String id = node.get("id").textValue();
        String name = node.get("name").textValue();

        JsonNode conditionsJson = node.get("conditions");
        if (conditionsJson.isTextual()) {
            conditionsJson = objectMapper.readTree(conditionsJson.textValue());
        }
        Condition conditions = ConditionJacksonDeserializer.<UserAttribute>parseConditions(UserAttribute.class, objectMapper, conditionsJson);

        return new Audience(id, name, conditions);
    }

}

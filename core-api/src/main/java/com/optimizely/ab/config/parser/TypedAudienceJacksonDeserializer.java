/**
 *    Copyright 2019, Optimizely Inc. and contributors
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

        Condition conditions = ConditionJacksonDeserializer.<UserAttribute>parseCondition(UserAttribute.class, objectMapper, conditionsJson);

        return new TypedAudience(id, name, conditions);
    }

}


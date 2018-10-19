/**
 *
 *    Copyright 2018, Optimizely and contributors
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.EmptyCondition;
import com.optimizely.ab.config.audience.NullCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConditionJacksonDeserializer extends JsonDeserializer<Condition> {
    private ObjectMapper objectMapper;

    public ConditionJacksonDeserializer() {
        this(new ObjectMapper());
    }

    ConditionJacksonDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Condition deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        JsonNode conditionsTree = node;
        if (node.isTextual()) {
            String conditionsJson = node.textValue();
            conditionsTree = objectMapper.readTree(conditionsJson);
        }
        Condition conditions = ConditionJacksonDeserializer.parseConditions(objectMapper, conditionsTree);

        return conditions;
    }

    protected static Condition parseConditions(ObjectMapper objectMapper, JsonNode conditionNode) throws JsonProcessingException {

        if (conditionNode.size() == 0) {
            return new EmptyCondition();
        }

        List<Condition> conditions = new ArrayList<>();
        int startingParsingIndex = 0;
        JsonNode opNode = conditionNode.get(0);
        String operand = opNode.asText();
        switch (operand) {
            case "or":
            case "and":
            case "not":
                startingParsingIndex = 1;
                break;
            default:
                operand = "or";
                break;
        }
        for (int i = startingParsingIndex; i < conditionNode.size(); i++) {
            JsonNode subNode = conditionNode.get(i);
            if (subNode.isArray()) {
                conditions.add(ConditionJacksonDeserializer.parseConditions(objectMapper, subNode));
            } else if (subNode.isTextual()) {
                conditions.add(objectMapper.treeToValue(subNode, AudienceIdCondition.class));
            }
            else if (subNode.isObject()) {
                conditions.add(objectMapper.treeToValue(subNode, UserAttribute.class));
            }
        }

        Condition condition;
        switch (operand) {
            case "and":
                condition = new AndCondition(conditions);
                break;
            case "or":
                condition = new OrCondition(conditions);
                break;
            case "not":
                condition = new NotCondition(conditions.isEmpty() ? new NullCondition() : conditions.get(0));
                break;
            default:
                condition = new OrCondition(conditions);
                break;
        }

        return condition;
    }

}

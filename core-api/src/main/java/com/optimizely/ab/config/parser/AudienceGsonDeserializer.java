/**
 *
 *    Copyright 2016-2018, Optimizely and contributors
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

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;

import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.UserAttribute;
import com.optimizely.ab.internal.ConditionUtils;

import java.lang.reflect.Type;

import java.util.List;

public class AudienceGsonDeserializer implements JsonDeserializer<Audience> {

    @Override
    public Audience deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = json.getAsJsonObject();

        String id = jsonObject.get("id").getAsString();
        String name = jsonObject.get("name").getAsString();

        JsonElement conditionsElement = jsonObject.get("conditions");
        if (!typeOfT.toString().contains("TypedAudience")) {
            conditionsElement = parser.parse(jsonObject.get("conditions").getAsString());
        }
        Condition conditions = null;
        if (conditionsElement.isJsonArray()) {
            List<Object> rawObjectList = gson.fromJson(conditionsElement, List.class);
            conditions =  ConditionUtils.parseConditions(UserAttribute.class, rawObjectList);
        }
        else if (conditionsElement.isJsonObject()) {
            Object object = gson.fromJson(conditionsElement,Object.class);
            conditions = ConditionUtils.parseConditions(UserAttribute.class, object);
        }

        return new Audience(id, name, conditions);
    }

}

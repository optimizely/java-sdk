/**
 *    Copyright 2022, Optimizely Inc. and contributors
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
package com.optimizely.ab.odp.serializer.impl;

import com.optimizely.ab.odp.ODPEvent;
import com.optimizely.ab.odp.serializer.ODPJsonSerializer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

public class JsonSimpleSerializer implements ODPJsonSerializer {
    @Override
    public String serializeEvents(List<ODPEvent> events) {
        JSONArray jsonArray = new JSONArray();
        for (ODPEvent event: events) {
            JSONObject eventObject = new JSONObject();
            eventObject.put("type", event.getType());
            eventObject.put("action", event.getAction());

            if (event.getIdentifiers() != null) {
                JSONObject identifiers = new JSONObject();
                for (Map.Entry<String, String> identifier : event.getIdentifiers().entrySet()) {
                    identifiers.put(identifier.getKey(), identifier.getValue());
                }
                eventObject.put("identifiers", identifiers);
            }

            if (event.getData() != null) {
                JSONObject data = new JSONObject();
                for (Map.Entry<String, String> dataEntry : event.getData().entrySet()) {
                    data.put(dataEntry.getKey(), dataEntry.getValue());
                }
                eventObject.put("data", data);
            }

            jsonArray.add(eventObject);
        }
        return jsonArray.toJSONString();
    }
}

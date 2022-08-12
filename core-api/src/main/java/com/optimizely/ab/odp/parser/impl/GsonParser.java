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
package com.optimizely.ab.odp.parser.impl;

import com.google.gson.*;
import com.google.gson.JsonParser;
import com.optimizely.ab.odp.parser.ResponseJsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GsonParser implements ResponseJsonParser {
    private static final Logger logger = LoggerFactory.getLogger(GsonParser.class);

    @Override
    public List<String> parseQualifiedSegments(String responseJson) {
        List<String> parsedSegments = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();

            if (root.has("errors")) {
                JsonArray errors =  root.getAsJsonArray("errors");
                StringBuilder logMessage = new StringBuilder();
                for (int i = 0; i < errors.size(); i++) {
                    if (i > 0) {
                        logMessage.append(", ");
                    }
                    logMessage.append(errors.get(i).getAsJsonObject().get("message").getAsString());
                }
                logger.error(logMessage.toString());
                return null;
            }

            JsonArray edges = root.getAsJsonObject("data").getAsJsonObject("customer").getAsJsonObject("audiences").getAsJsonArray("edges");
            for (int i = 0; i < edges.size(); i++) {
                JsonObject node = edges.get(i).getAsJsonObject().getAsJsonObject("node");
                if (node.has("state") && node.get("state").getAsString().equals("qualified")) {
                    parsedSegments.add(node.get("name").getAsString());
                }
            }
            return parsedSegments;
        } catch (JsonSyntaxException e) {
            logger.error("Error parsing qualified segments from response", e);
            return null;
        }
    }
}

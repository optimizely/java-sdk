/**
 *    Copyright 2022-2023, Optimizely Inc. and contributors
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

import com.optimizely.ab.odp.parser.ResponseJsonParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class JsonSimpleParser implements ResponseJsonParser {
    private static final Logger logger = LoggerFactory.getLogger(JsonSimpleParser.class);

    @Override
    public List<String> parseQualifiedSegments(String responseJson) {
        List<String> parsedSegments = new ArrayList<>();
        JSONParser parser = new JSONParser();
        JSONObject root = null;
        try {
            root = (JSONObject) parser.parse(responseJson);
            if (root.containsKey("errors")) {
                JSONArray errors = (JSONArray) root.get("errors");
                JSONObject extensions = (JSONObject) ((JSONObject) errors.get(0)).get("extensions");
                if (extensions != null && extensions.containsKey("code")) {
                    if (extensions.get("code").equals("INVALID_IDENTIFIER_EXCEPTION")) {
                        logger.warn("Audience segments fetch failed (invalid identifier)");
                    } else {
                        String errorMessage = extensions.get("classification") == null ? "decode error" : (String) extensions.get("classification");
                        logger.error("Audience segments fetch failed (" + errorMessage + ")");
                    }
                }
                return null;
            }

            JSONArray edges = (JSONArray)((JSONObject)((JSONObject)(((JSONObject) root.get("data"))).get("customer")).get("audiences")).get("edges");
            for (int i = 0; i < edges.size(); i++) {
                JSONObject node = (JSONObject) ((JSONObject) edges.get(i)).get("node");
                if (node.containsKey("state") && (node.get("state")).equals("qualified")) {
                    parsedSegments.add((String) node.get("name"));
                }
            }
            return parsedSegments;
        } catch (ParseException | NullPointerException e) {
            logger.error("Error parsing qualified segments from response", e);
            return null;
        }
    }
}

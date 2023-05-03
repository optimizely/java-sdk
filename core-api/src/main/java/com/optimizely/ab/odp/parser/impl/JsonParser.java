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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class JsonParser implements ResponseJsonParser {
    private static final Logger logger = LoggerFactory.getLogger(JsonParser.class);

    @Override
    public List<String> parseQualifiedSegments(String responseJson) {
        List<String> parsedSegments = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(responseJson);

            if (root.has("errors")) {
                JSONArray errors = root.getJSONArray("errors");
                JSONObject extensions = errors.getJSONObject(0).getJSONObject("extensions");
                if (extensions != null) {
                    if (extensions.has("code") && extensions.getString("code").equals("INVALID_IDENTIFIER_EXCEPTION")) {
                        logger.warn("Audience segments fetch failed (invalid identifier)");
                    } else {
                        String errorMessage = extensions.has("classification") ?
                            extensions.getString("classification") : "decode error";
                        logger.error("Audience segments fetch failed (" + errorMessage + ")");
                    }
                }
                return null;
            }

            JSONArray edges = root.getJSONObject("data").getJSONObject("customer").getJSONObject("audiences").getJSONArray("edges");
            for (int i = 0; i < edges.length(); i++) {
                JSONObject node = edges.getJSONObject(i).getJSONObject("node");
                if (node.has("state") && node.getString("state").equals("qualified")) {
                    parsedSegments.add(node.getString("name"));
                }
            }
            return parsedSegments;
        } catch (JSONException e) {
            logger.error("Error parsing qualified segments from response", e);
            return  null;
        }
    }
}

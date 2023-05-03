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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.odp.parser.ResponseJsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import java.util.List;

public class JacksonParser implements ResponseJsonParser {
    private static final Logger logger = LoggerFactory.getLogger(JacksonParser.class);

    @Override
    public List<String> parseQualifiedSegments(String responseJson) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> parsedSegments = new ArrayList<>();
        JsonNode root;
        try {
            root = objectMapper.readTree(responseJson);

            if (root.has("errors")) {
                JsonNode errors = root.path("errors");
                JsonNode extensions = errors.get(0).path("extensions");
                if (extensions != null && extensions.has("code")) {
                    if (extensions.path("code").asText().equals("INVALID_IDENTIFIER_EXCEPTION")) {
                        logger.warn("Audience segments fetch failed (invalid identifier)");
                    } else {
                        String errorMessage = extensions.has("classification") ? extensions.path("classification").asText() : "decode error";
                        logger.error("Audience segments fetch failed (" + errorMessage + ")");
                    }
                }
                return null;
            }

            JsonNode edges = root.path("data").path("customer").path("audiences").path("edges");
            for (JsonNode edgeNode : edges) {
                String state = edgeNode.path("node").path("state").asText();
                if (state.equals("qualified")) {
                    parsedSegments.add(edgeNode.path("node").path("name").asText());
                }
            }
            return parsedSegments;
        } catch (JsonProcessingException e) {
            logger.error("Error parsing qualified segments from response", e);
            return null;
        }
    }
}

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
                StringBuilder logMessage = new StringBuilder();
                for (int i = 0; i < errors.size(); i++) {
                    if (i > 0) {
                        logMessage.append(", ");
                    }
                    logMessage.append(errors.get(i).path("message"));
                }
                logger.error(logMessage.toString());
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

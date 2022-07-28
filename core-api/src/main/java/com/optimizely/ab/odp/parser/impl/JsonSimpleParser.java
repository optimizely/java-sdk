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
            JSONArray edges = (JSONArray)((JSONObject)((JSONObject)(((JSONObject) root.get("data"))).get("customer")).get("audiences")).get("edges");
            for (int i = 0; i < edges.size(); i++) {
                JSONObject node = (JSONObject) ((JSONObject) edges.get(i)).get("node");
                if (node.containsKey("state") && (node.get("state")).equals("qualified")) {
                    parsedSegments.add((String) node.get("name"));
                }
            }
            return parsedSegments;
        } catch (ParseException e) {
            logger.error("Error parsing qualified segments from response", e);
            return null;
        }
    }
}

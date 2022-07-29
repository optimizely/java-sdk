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
                JSONArray errors =  root.getJSONArray("errors");
                StringBuilder logMessage = new StringBuilder();
                for (int i = 0; i < errors.length(); i++) {
                    if (i > 0) {
                        logMessage.append(", ");
                    }
                    logMessage.append(errors.getJSONObject(i).getString("message"));
                }
                logger.error(logMessage.toString());
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

package com.optimizely.ab.odp.parser.impl;

import com.optimizely.ab.odp.parser.ResponseJsonParser;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonParser implements ResponseJsonParser {
    @Override
    public List<String> parseQualifiedSegments(String responseJson) {
        List<String> parsedSegments = new ArrayList<>();
        JSONObject root = new JSONObject(responseJson);
        JSONArray edges = root.getJSONObject("data").getJSONObject("customer").getJSONObject("audiences").getJSONArray("edges");
        for (int i = 0; i < edges.length(); i++) {
            JSONObject node = edges.getJSONObject(i).getJSONObject("node");
            if (node.has("state") && node.getString("state").equals("qualified")) {
                parsedSegments.add(node.getString("name"));
            }
        }
        return parsedSegments;
    }
}

package com.optimizely.ab.odp.parser.impl;

import com.google.gson.*;
import com.google.gson.JsonParser;
import com.optimizely.ab.odp.parser.ResponseJsonParser;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GsonParser implements ResponseJsonParser {
    @Override
    public List<String> parseQualifiedSegments(String responseJson) {
        List<String> parsedSegments = new ArrayList<>();
        JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
        JsonArray edges = root.getAsJsonObject("data").getAsJsonObject("customer").getAsJsonObject("audiences").getAsJsonArray("edges");
        for (int i = 0; i < edges.size() ; i++) {
            JsonObject node = edges.get(i).getAsJsonObject().getAsJsonObject("node");
            if (node.has("state") && node.get("state").getAsString().equals("qualified")) {
                parsedSegments.add(node.get("name").getAsString());
            }
        }
        return parsedSegments;
    }
}

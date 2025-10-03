package com.optimizely.ab.cmab.client;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CmabClientHelper {
    public static final String CMAB_FETCH_FAILED = "CMAB decision fetch failed with status: %s";
    public static final String INVALID_CMAB_FETCH_RESPONSE = "Invalid CMAB fetch response";
    private static final Pattern VARIATION_ID_PATTERN = Pattern.compile("\"variation_id\"\\s*:\\s*\"?([^\"\\s,}]+)\"?");

    public static String buildRequestJson(String userId, String ruleId, Map<String, Object> attributes, String cmabUuid) {
        StringBuilder json = new StringBuilder();
        json.append("{\"instances\":[{");
        json.append("\"visitorId\":\"").append(escapeJson(userId)).append("\",");
        json.append("\"experimentId\":\"").append(escapeJson(ruleId)).append("\",");
        json.append("\"cmabUUID\":\"").append(escapeJson(cmabUuid)).append("\",");
        json.append("\"attributes\":[");

        boolean first = true;
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("{\"id\":\"").append(escapeJson(entry.getKey())).append("\",");
            json.append("\"value\":").append(formatJsonValue(entry.getValue())).append(",");
            json.append("\"type\":\"custom_attribute\"}");
            first = false;
        }

        json.append("]}]}");
        return json.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private static String formatJsonValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    public static String parseVariationId(String jsonResponse) {
        // Simple regex to extract variation_id from predictions[0].variation_id
        Pattern pattern = Pattern.compile("\"predictions\"\\s*:\\s*\\[\\s*\\{[^}]*\"variation_id\"\\s*:\\s*\"?([^\"\\s,}]+)\"?");
        Matcher matcher = pattern.matcher(jsonResponse);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new CmabInvalidResponseException(INVALID_CMAB_FETCH_RESPONSE);
    }

    private static String parseVariationIdForValidation(String jsonResponse) {
        Matcher matcher = VARIATION_ID_PATTERN.matcher(jsonResponse);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static boolean validateResponse(String responseBody) {
        try {
            return responseBody.contains("predictions") &&
                responseBody.contains("variation_id") &&
                parseVariationIdForValidation(responseBody) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isSuccessStatusCode(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
}

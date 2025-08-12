/**
 *
 *    Copyright 2016-2017, 2019, Optimizely and contributors
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
package com.optimizely.ab.config.parser;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.optimizely.ab.bucketing.DecisionService;
import com.optimizely.ab.config.*;
import com.optimizely.ab.config.Experiment.ExperimentStatus;
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.internal.ConditionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GsonHelpers {

    private static final Logger logger = LoggerFactory.getLogger(DecisionService.class);

    private static List<Variation> parseVariations(JsonArray variationJson, JsonDeserializationContext context) {
        List<Variation> variations = new ArrayList<Variation>(variationJson.size());
        for (Object obj : variationJson) {
            JsonObject variationObject = (JsonObject) obj;
            String id = variationObject.get("id").getAsString();
            String key = variationObject.get("key").getAsString();
            Boolean featureEnabled = false;
            if (variationObject.has("featureEnabled") && !variationObject.get("featureEnabled").isJsonNull()) {
                featureEnabled = variationObject.get("featureEnabled").getAsBoolean();
            }

            List<FeatureVariableUsageInstance> variableUsageInstances = null;
            // this is an existence check rather than a version check since it's difficult to pass data
            // across deserializers.
            if (variationObject.has("variables")) {
                Type featureVariableUsageInstancesType = new TypeToken<List<FeatureVariableUsageInstance>>() {
                }.getType();
                variableUsageInstances =
                    context.deserialize(variationObject.getAsJsonArray("variables"),
                        featureVariableUsageInstancesType);
            }

            variations.add(new Variation(id, key, featureEnabled, variableUsageInstances));
        }

        return variations;
    }

    private static Map<String, String> parseForcedVariations(JsonObject forcedVariationJson) {
        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        Set<Map.Entry<String, JsonElement>> entrySet = forcedVariationJson.entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            userIdToVariationKeyMap.put(entry.getKey(), entry.getValue().getAsString());
        }

        return userIdToVariationKeyMap;
    }

    static List<TrafficAllocation> parseTrafficAllocation(JsonArray trafficAllocationJson) {
        List<TrafficAllocation> trafficAllocation = new ArrayList<TrafficAllocation>(trafficAllocationJson.size());

        for (Object obj : trafficAllocationJson) {
            JsonObject allocationObject = (JsonObject) obj;
            String entityId = allocationObject.get("entityId").getAsString();
            int endOfRange = allocationObject.get("endOfRange").getAsInt();

            trafficAllocation.add(new TrafficAllocation(entityId, endOfRange));
        }

        return trafficAllocation;
    }

    static Condition parseAudienceConditions(JsonObject experimentJson) {

        if (!experimentJson.has("audienceConditions")) return null;

        Gson gson = new Gson();

        JsonElement conditionsElement = experimentJson.get("audienceConditions");

        if (conditionsElement.isJsonArray()) {
            List<Object> rawObjectList = gson.fromJson(conditionsElement, List.class);
            return ConditionUtils.<AudienceIdCondition>parseConditions(AudienceIdCondition.class, rawObjectList);
        } else {
            Object jsonObject = gson.fromJson(conditionsElement, Object.class);
            return ConditionUtils.<AudienceIdCondition>parseConditions(AudienceIdCondition.class, jsonObject);
        }

    }

    static Cmab parseCmab(JsonObject cmabJson, JsonDeserializationContext context) {
        if (cmabJson == null) {
            return null;
        }

        JsonArray attributeIdsJson = cmabJson.getAsJsonArray("attributeIds");
        List<String> attributeIds = new ArrayList<>();
        if (attributeIdsJson != null) {
            for (JsonElement attributeIdElement : attributeIdsJson) {
                attributeIds.add(attributeIdElement.getAsString());
            }
        }

        int trafficAllocation = 0;
        if (cmabJson.has("trafficAllocation")) {
            trafficAllocation = cmabJson.get("trafficAllocation").getAsInt();
        }

        return new Cmab(attributeIds, trafficAllocation);
    }

    static Experiment parseExperiment(JsonObject experimentJson, String groupId, JsonDeserializationContext context) {
        String id = experimentJson.get("id").getAsString();
        String key = experimentJson.get("key").getAsString();
        JsonElement experimentStatusJson = experimentJson.get("status");
        String status = experimentStatusJson.isJsonNull() ?
            ExperimentStatus.NOT_STARTED.toString() : experimentStatusJson.getAsString();

        JsonElement layerIdJson = experimentJson.get("layerId");
        String layerId = layerIdJson == null ? null : layerIdJson.getAsString();

        JsonArray audienceIdsJson = experimentJson.getAsJsonArray("audienceIds");
        List<String> audienceIds = new ArrayList<>(audienceIdsJson.size());
        for (JsonElement audienceIdObj : audienceIdsJson) {
            audienceIds.add(audienceIdObj.getAsString());
        }

        Condition conditions = parseAudienceConditions(experimentJson);

        // parse the child objects
        List<Variation> variations = parseVariations(experimentJson.getAsJsonArray("variations"), context);
        Map<String, String> userIdToVariationKeyMap =
            parseForcedVariations(experimentJson.getAsJsonObject("forcedVariations"));
        List<TrafficAllocation> trafficAllocations =
            parseTrafficAllocation(experimentJson.getAsJsonArray("trafficAllocation"));

        Cmab cmab = null;
        if (experimentJson.has("cmab")) {
            JsonElement cmabElement = experimentJson.get("cmab");
            if (!cmabElement.isJsonNull()) {
                JsonObject cmabJson = cmabElement.getAsJsonObject();
                cmab = parseCmab(cmabJson, context);
            }
        }

        return new Experiment(id, key, status, layerId, audienceIds, conditions, variations, userIdToVariationKeyMap,
            trafficAllocations, groupId, cmab);
    }

    static Experiment parseExperiment(JsonObject experimentJson, JsonDeserializationContext context) {
        return parseExperiment(experimentJson, "", context);
    }

    static Holdout parseHoldout(JsonObject holdoutJson, JsonDeserializationContext context) {
        String id = holdoutJson.get("id").getAsString();
        String key = holdoutJson.get("key").getAsString();
        String status = holdoutJson.get("status").getAsString();

        JsonArray audienceIdsJson = holdoutJson.getAsJsonArray("audienceIds");
        List<String> audienceIds = new ArrayList<>(audienceIdsJson.size());
        for (JsonElement audienceIdObj : audienceIdsJson) {
            audienceIds.add(audienceIdObj.getAsString());
        }

        Condition conditions = parseAudienceConditions(holdoutJson);

        // parse the child objects
        List<Variation> variations = parseVariations(holdoutJson.getAsJsonArray("variations"), context);
        List<TrafficAllocation> trafficAllocations =
            parseTrafficAllocation(holdoutJson.getAsJsonArray("trafficAllocation"));

        List<String> includedFlags = new ArrayList<>();
        if (holdoutJson.has("includedFlags")) {
            JsonArray includedIdsJson = holdoutJson.getAsJsonArray("includedFlags");
            for (JsonElement hoIdObj : includedIdsJson) {
                includedFlags.add(hoIdObj.getAsString());
            }
        }

        List<String> excludedFlags = new ArrayList<>();
        if (holdoutJson.has("excludedFlags")) {
            JsonArray excludedIdsJson = holdoutJson.getAsJsonArray("excludedFlags");
            for (JsonElement hoIdObj : excludedIdsJson) {
                excludedFlags.add(hoIdObj.getAsString());
            }
        }

        return new Holdout(id, key, status, audienceIds, conditions, variations, trafficAllocations, includedFlags, excludedFlags);
    }

    static FeatureFlag parseFeatureFlag(JsonObject featureFlagJson, JsonDeserializationContext context) {
        String id = featureFlagJson.get("id").getAsString();
        String key = featureFlagJson.get("key").getAsString();
        String layerId = featureFlagJson.get("rolloutId").getAsString();

        JsonArray experimentIdsJson = featureFlagJson.getAsJsonArray("experimentIds");
        List<String> experimentIds = new ArrayList<String>();
        for (JsonElement experimentIdObj : experimentIdsJson) {
            experimentIds.add(experimentIdObj.getAsString());
        }

        List<FeatureVariable> FeatureVariables = new ArrayList<>();
        try {
            Type FeatureVariableType = new TypeToken<List<FeatureVariable>>() {
            }.getType();
            FeatureVariables = context.deserialize(featureFlagJson.getAsJsonArray("variables"),
                FeatureVariableType);
        } catch (JsonParseException exception) {
            logger.warn("Unable to parse variables for feature \"" + key
                + "\". JsonParseException: " + exception);
        }

        return new FeatureFlag(
            id,
            key,
            layerId,
            experimentIds,
            FeatureVariables
        );
    }
}

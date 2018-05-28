/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.optimizely.ab.bucketing.DecisionService;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Experiment.ExperimentStatus;
import com.optimizely.ab.config.FeatureFlag;
import com.optimizely.ab.config.LiveVariable;
import com.optimizely.ab.config.LiveVariableUsageInstance;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.faultinjection.ExceptionSpot;
import com.optimizely.ab.faultinjection.FaultInjectionManager;
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

    private static void injectFault(ExceptionSpot spot) {
        FaultInjectionManager.getInstance().injectFault(spot);
    }

    private static List<Variation> parseVariations(JsonArray variationJson, JsonDeserializationContext context) {

        try {

            injectFault(ExceptionSpot.GsonHelpers_parseVariations_spot1);
            List<Variation> variations = new ArrayList<Variation>(variationJson.size());
            for (Object obj : variationJson) {
                injectFault(ExceptionSpot.GsonHelpers_parseVariations_spot2);
                JsonObject variationObject = (JsonObject) obj;
                String id = variationObject.get("id").getAsString();
                String key = variationObject.get("key").getAsString();
                Boolean featureEnabled = false;
                if (variationObject.has("featureEnabled"))
                    featureEnabled = variationObject.get("featureEnabled").getAsBoolean();

                List<LiveVariableUsageInstance> variableUsageInstances = null;
                // this is an existence check rather than a version check since it's difficult to pass data
                // across deserializers.
                if (variationObject.has("variables")) {
                    Type liveVariableUsageInstancesType = new TypeToken<List<LiveVariableUsageInstance>>() {
                    }.getType();
                    variableUsageInstances =
                            context.deserialize(variationObject.getAsJsonArray("variables"),
                                    liveVariableUsageInstancesType);
                }

                variations.add(new Variation(id, key, featureEnabled, variableUsageInstances));
            }

            injectFault(ExceptionSpot.GsonHelpers_parseVariations_spot3);
            return variations;
        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
            return null;
        }
    }

    private static Map<String, String> parseForcedVariations(JsonObject forcedVariationJson) {
        try {

            injectFault(ExceptionSpot.GsonHelpers_parseForcedVariations_spot1);
            Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
            Set<Map.Entry<String, JsonElement>> entrySet = forcedVariationJson.entrySet();
            for (Map.Entry<String, JsonElement> entry : entrySet) {
                userIdToVariationKeyMap.put(entry.getKey(), entry.getValue().getAsString());
            }

            return userIdToVariationKeyMap;
        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
            return null;
        }
    }

    static List<TrafficAllocation> parseTrafficAllocation(JsonArray trafficAllocationJson) {

        try {

            injectFault(ExceptionSpot.GsonHelpers_parseTrafficAllocation_spot1);

            List<TrafficAllocation> trafficAllocation = new ArrayList<TrafficAllocation>(trafficAllocationJson.size());

            for (Object obj : trafficAllocationJson) {
                JsonObject allocationObject = (JsonObject) obj;
                String entityId = allocationObject.get("entityId").getAsString();
                int endOfRange = allocationObject.get("endOfRange").getAsInt();

                trafficAllocation.add(new TrafficAllocation(entityId, endOfRange));
            }

            return trafficAllocation;
        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
            return null;
        }
    }

    static Experiment parseExperiment(JsonObject experimentJson, String groupId, JsonDeserializationContext context) {

        try {

            injectFault(ExceptionSpot.GsonHelpers_parseExperiment1_spot1);

            String id = experimentJson.get("id").getAsString();
            String key = experimentJson.get("key").getAsString();
            JsonElement experimentStatusJson = experimentJson.get("status");
            String status = experimentStatusJson.isJsonNull() ?
                    ExperimentStatus.NOT_STARTED.toString() : experimentStatusJson.getAsString();

            JsonElement layerIdJson = experimentJson.get("layerId");
            String layerId = layerIdJson == null ? null : layerIdJson.getAsString();

            JsonArray audienceIdsJson = experimentJson.getAsJsonArray("audienceIds");
            List<String> audienceIds = new ArrayList<String>(audienceIdsJson.size());
            injectFault(ExceptionSpot.GsonHelpers_parseExperiment1_spot2);
            for (JsonElement audienceIdObj : audienceIdsJson) {
                audienceIds.add(audienceIdObj.getAsString());
            }

            // parse the child objects
            List<Variation> variations = parseVariations(experimentJson.getAsJsonArray("variations"), context);
            Map<String, String> userIdToVariationKeyMap =
                    parseForcedVariations(experimentJson.getAsJsonObject("forcedVariations"));
            List<TrafficAllocation> trafficAllocations =
                    parseTrafficAllocation(experimentJson.getAsJsonArray("trafficAllocation"));


            injectFault(ExceptionSpot.GsonHelpers_parseExperiment1_spot3);
            return new Experiment(id, key, status, layerId, audienceIds, variations, userIdToVariationKeyMap,
                    trafficAllocations, groupId);
        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
            return null;
        }
    }

    static Experiment parseExperiment(JsonObject experimentJson, JsonDeserializationContext context) {

        try {

            injectFault(ExceptionSpot.GsonHelpers_parseExperiment2_spot1);
            return parseExperiment(experimentJson, "", context);

        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
            return null;
        }
    }

    static FeatureFlag parseFeatureFlag(JsonObject featureFlagJson, JsonDeserializationContext context) {

        try {

            injectFault(ExceptionSpot.GsonHelpers_parseFeatureFlag_spot1);
            String id = featureFlagJson.get("id").getAsString();
            String key = featureFlagJson.get("key").getAsString();
            String layerId = featureFlagJson.get("rolloutId").getAsString();

            JsonArray experimentIdsJson = featureFlagJson.getAsJsonArray("experimentIds");
            List<String> experimentIds = new ArrayList<String>();
            injectFault(ExceptionSpot.GsonHelpers_parseFeatureFlag_spot2);
            for (JsonElement experimentIdObj : experimentIdsJson) {
                experimentIds.add(experimentIdObj.getAsString());
            }

            List<LiveVariable> liveVariables = new ArrayList<LiveVariable>();
            try {
                Type liveVariableType = new TypeToken<List<LiveVariable>>() {
                }.getType();
                liveVariables = context.deserialize(featureFlagJson.getAsJsonArray("variables"),
                        liveVariableType);
            } catch (JsonParseException exception) {
                logger.warn("Unable to parse variables for feature \"" + key
                        + "\". JsonParseException: " + exception);
            }

            injectFault(ExceptionSpot.GsonHelpers_parseFeatureFlag_spot3);
            return new FeatureFlag(
                    id,
                    key,
                    layerId,
                    experimentIds,
                    liveVariables
            );
        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
            return null;
        }
    }
}

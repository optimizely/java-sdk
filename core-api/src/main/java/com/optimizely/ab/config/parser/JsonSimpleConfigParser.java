/**
 *
 *    Copyright 2016-2019, Optimizely and contributors
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

import com.optimizely.ab.config.*;
import com.optimizely.ab.config.Experiment.ExperimentStatus;
import com.optimizely.ab.config.FeatureVariable.VariableStatus;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.UserAttribute;
import com.optimizely.ab.internal.ConditionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code json-simple}-based config parser implementation.
 */
final public class JsonSimpleConfigParser implements ConfigParser {

    @Override
    public ProjectConfig parseProjectConfig(@Nonnull String json) throws ConfigParseException {
        try {
            JSONParser parser = new JSONParser();
            JSONObject rootObject = (JSONObject) parser.parse(json);

            String accountId = (String) rootObject.get("accountId");
            String projectId = (String) rootObject.get("projectId");
            String revision = (String) rootObject.get("revision");
            String version = (String) rootObject.get("version");
            int datafileVersion = Integer.parseInt(version);

            List<Experiment> experiments = parseExperiments((JSONArray) rootObject.get("experiments"));

            List<Attribute> attributes;
            attributes = parseAttributes((JSONArray) rootObject.get("attributes"));

            List<EventType> events = parseEvents((JSONArray) rootObject.get("events"));
            List<Audience> audiences = Collections.emptyList();

            if (rootObject.containsKey("audiences")) {
                audiences = parseAudiences((JSONArray) parser.parse(rootObject.get("audiences").toString()));
            }

            List<Audience> typedAudiences = null;
            if (rootObject.containsKey("typedAudiences")) {
                typedAudiences = parseTypedAudiences((JSONArray) parser.parse(rootObject.get("typedAudiences").toString()));
            }

            List<Group> groups = parseGroups((JSONArray) rootObject.get("groups"));

            boolean anonymizeIP = false;
            if (datafileVersion >= Integer.parseInt(DatafileProjectConfig.Version.V3.toString())) {
                anonymizeIP = (Boolean) rootObject.get("anonymizeIP");
            }

            List<FeatureFlag> featureFlags = null;
            List<Rollout> rollouts = null;
            Boolean botFiltering = null;
            if (datafileVersion >= Integer.parseInt(DatafileProjectConfig.Version.V4.toString())) {
                featureFlags = parseFeatureFlags((JSONArray) rootObject.get("featureFlags"));
                rollouts = parseRollouts((JSONArray) rootObject.get("rollouts"));
                if (rootObject.containsKey("botFiltering"))
                    botFiltering = (Boolean) rootObject.get("botFiltering");
            }

            return new DatafileProjectConfig(
                accountId,
                anonymizeIP,
                botFiltering,
                projectId,
                revision,
                version,
                attributes,
                audiences,
                typedAudiences,
                events,
                experiments,
                featureFlags,
                groups,
                rollouts
            );
        } catch (RuntimeException ex) {
            throw new ConfigParseException("Unable to parse datafile: " + json, ex);
        } catch (Exception e) {
            throw new ConfigParseException("Unable to parse datafile: " + json, e);
        }
    }

    //======== Helper methods ========//

    private List<Experiment> parseExperiments(JSONArray experimentJson) {
        return parseExperiments(experimentJson, "");
    }

    private List<Experiment> parseExperiments(JSONArray experimentJson, String groupId) {
        List<Experiment> experiments = new ArrayList<Experiment>(experimentJson.size());

        for (Object obj : experimentJson) {
            JSONObject experimentObject = (JSONObject) obj;
            String id = (String) experimentObject.get("id");
            String key = (String) experimentObject.get("key");
            Object statusJson = experimentObject.get("status");
            String status = statusJson == null ? ExperimentStatus.NOT_STARTED.toString() :
                (String) experimentObject.get("status");
            Object layerIdObject = experimentObject.get("layerId");
            String layerId = layerIdObject == null ? null : (String) layerIdObject;

            JSONArray audienceIdsJson = (JSONArray) experimentObject.get("audienceIds");
            List<String> audienceIds = new ArrayList<String>(audienceIdsJson.size());

            for (Object audienceIdObj : audienceIdsJson) {
                audienceIds.add((String) audienceIdObj);
            }

            Condition conditions = null;
            if (experimentObject.containsKey("audienceConditions")) {
                Object jsonCondition = experimentObject.get("audienceConditions");
                try {
                    conditions = ConditionUtils.<AudienceIdCondition>parseConditions(AudienceIdCondition.class, jsonCondition);
                } catch (Exception e) {
                    // unable to parse conditions.
                    Logger.getAnonymousLogger().log(Level.ALL, "problem parsing audience conditions", e);
                }
            }
            // parse the child objects
            List<Variation> variations = parseVariations((JSONArray) experimentObject.get("variations"));
            Map<String, String> userIdToVariationKeyMap =
                parseForcedVariations((JSONObject) experimentObject.get("forcedVariations"));
            List<TrafficAllocation> trafficAllocations =
                parseTrafficAllocation((JSONArray) experimentObject.get("trafficAllocation"));

            experiments.add(new Experiment(id, key, status, layerId, audienceIds, conditions, variations, userIdToVariationKeyMap,
                trafficAllocations, groupId));
        }

        return experiments;
    }

    private List<String> parseExperimentIds(JSONArray experimentIdsJsonArray) {
        List<String> experimentIds = new ArrayList<String>(experimentIdsJsonArray.size());

        for (Object experimentIdObj : experimentIdsJsonArray) {
            experimentIds.add((String) experimentIdObj);
        }

        return experimentIds;
    }

    private List<FeatureFlag> parseFeatureFlags(JSONArray featureFlagJson) {
        List<FeatureFlag> featureFlags = new ArrayList<FeatureFlag>(featureFlagJson.size());

        for (Object obj : featureFlagJson) {
            JSONObject featureFlagObject = (JSONObject) obj;
            String id = (String) featureFlagObject.get("id");
            String key = (String) featureFlagObject.get("key");
            String layerId = (String) featureFlagObject.get("rolloutId");

            JSONArray experimentIdsJsonArray = (JSONArray) featureFlagObject.get("experimentIds");
            List<String> experimentIds = parseExperimentIds(experimentIdsJsonArray);

            List<FeatureVariable> featureVariable = parseFeatureVariables((JSONArray) featureFlagObject.get("variables"));

            featureFlags.add(new FeatureFlag(
                id,
                key,
                layerId,
                experimentIds,
                featureVariable
            ));
        }

        return featureFlags;
    }

    private List<Variation> parseVariations(JSONArray variationJson) {
        List<Variation> variations = new ArrayList<Variation>(variationJson.size());

        for (Object obj : variationJson) {
            JSONObject variationObject = (JSONObject) obj;
            String id = (String) variationObject.get("id");
            String key = (String) variationObject.get("key");
            Boolean featureEnabled = false;

            if (variationObject.containsKey("featureEnabled"))
                featureEnabled = (Boolean) variationObject.get("featureEnabled");

            List<FeatureVariableUsageInstance> featureVariableUsageInstances = null;
            if (variationObject.containsKey("variables")) {
                featureVariableUsageInstances = parseFeatureVariableInstances((JSONArray) variationObject.get("variables"));
            }

            variations.add(new Variation(id, key, featureEnabled, featureVariableUsageInstances));
        }

        return variations;
    }

    private Map<String, String> parseForcedVariations(JSONObject forcedVariationJson) {
        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        for (Object obj : forcedVariationJson.entrySet()) {
            Map.Entry<String, String> entry = (Map.Entry<String, String>) obj;
            userIdToVariationKeyMap.put(entry.getKey(), entry.getValue());
        }

        return userIdToVariationKeyMap;
    }

    private List<TrafficAllocation> parseTrafficAllocation(JSONArray trafficAllocationJson) {
        List<TrafficAllocation> trafficAllocation = new ArrayList<TrafficAllocation>(trafficAllocationJson.size());

        for (Object obj : trafficAllocationJson) {
            JSONObject allocationObject = (JSONObject) obj;
            String entityId = (String) allocationObject.get("entityId");
            long endOfRange = (Long) allocationObject.get("endOfRange");

            trafficAllocation.add(new TrafficAllocation(entityId, (int) endOfRange));
        }

        return trafficAllocation;
    }

    private List<Attribute> parseAttributes(JSONArray attributeJson) {
        List<Attribute> attributes = new ArrayList<Attribute>(attributeJson.size());

        for (Object obj : attributeJson) {
            JSONObject attributeObject = (JSONObject) obj;
            String id = (String) attributeObject.get("id");
            String key = (String) attributeObject.get("key");
            String segmentId = (String) attributeObject.get("segmentId");

            attributes.add(new Attribute(id, key, segmentId));
        }

        return attributes;
    }

    private List<EventType> parseEvents(JSONArray eventJson) {
        List<EventType> events = new ArrayList<EventType>(eventJson.size());

        for (Object obj : eventJson) {
            JSONObject eventObject = (JSONObject) obj;
            JSONArray experimentIdsJson = (JSONArray) eventObject.get("experimentIds");
            List<String> experimentIds = parseExperimentIds(experimentIdsJson);

            String id = (String) eventObject.get("id");
            String key = (String) eventObject.get("key");

            events.add(new EventType(id, key, experimentIds));
        }

        return events;
    }

    private List<Audience> parseAudiences(JSONArray audienceJson) throws ParseException {
        JSONParser parser = new JSONParser();
        List<Audience> audiences = new ArrayList<Audience>(audienceJson.size());

        for (Object obj : audienceJson) {
            JSONObject audienceObject = (JSONObject) obj;
            String id = (String) audienceObject.get("id");
            String key = (String) audienceObject.get("name");
            Object conditionObject = audienceObject.get("conditions");
            Object conditionJson = parser.parse((String) conditionObject);
            Condition conditions = ConditionUtils.<UserAttribute>parseConditions(UserAttribute.class, conditionJson);
            audiences.add(new Audience(id, key, conditions));
        }

        return audiences;
    }

    private List<Audience> parseTypedAudiences(JSONArray audienceJson) throws ParseException {
        List<Audience> audiences = new ArrayList<Audience>(audienceJson.size());

        for (Object obj : audienceJson) {
            JSONObject audienceObject = (JSONObject) obj;
            String id = (String) audienceObject.get("id");
            String key = (String) audienceObject.get("name");
            Object conditionObject = audienceObject.get("conditions");
            Condition conditions = ConditionUtils.<UserAttribute>parseConditions(UserAttribute.class, conditionObject);
            audiences.add(new Audience(id, key, conditions));
        }

        return audiences;
    }

    private List<Group> parseGroups(JSONArray groupJson) {
        List<Group> groups = new ArrayList<Group>(groupJson.size());

        for (Object obj : groupJson) {
            JSONObject groupObject = (JSONObject) obj;
            String id = (String) groupObject.get("id");
            String policy = (String) groupObject.get("policy");
            List<Experiment> experiments = parseExperiments((JSONArray) groupObject.get("experiments"), id);
            List<TrafficAllocation> trafficAllocations =
                parseTrafficAllocation((JSONArray) groupObject.get("trafficAllocation"));

            groups.add(new Group(id, policy, experiments, trafficAllocations));
        }

        return groups;
    }

    private List<FeatureVariable> parseFeatureVariables(JSONArray featureVariablesJson) {
        List<FeatureVariable> featureVariables = new ArrayList<FeatureVariable>(featureVariablesJson.size());

        for (Object obj : featureVariablesJson) {
            JSONObject featureVariableObject = (JSONObject) obj;
            String id = (String) featureVariableObject.get("id");
            String key = (String) featureVariableObject.get("key");
            String defaultValue = (String) featureVariableObject.get("defaultValue");
            String type = (String) featureVariableObject.get("type");
            VariableStatus status = VariableStatus.fromString((String) featureVariableObject.get("status"));

            featureVariables.add(new FeatureVariable(id, key, defaultValue, status, type));
        }

        return featureVariables;
    }

    private List<FeatureVariableUsageInstance> parseFeatureVariableInstances(JSONArray featureVariableInstancesJson) {
        List<FeatureVariableUsageInstance> featureVariableUsageInstances =
            new ArrayList<FeatureVariableUsageInstance>(featureVariableInstancesJson.size());

        for (Object obj : featureVariableInstancesJson) {
            JSONObject featureVariableInstanceObject = (JSONObject) obj;
            String id = (String) featureVariableInstanceObject.get("id");
            String value = (String) featureVariableInstanceObject.get("value");

            featureVariableUsageInstances.add(new FeatureVariableUsageInstance(id, value));
        }

        return featureVariableUsageInstances;
    }

    private List<Rollout> parseRollouts(JSONArray rolloutsJson) {
        List<Rollout> rollouts = new ArrayList<Rollout>(rolloutsJson.size());

        for (Object obj : rolloutsJson) {
            JSONObject rolloutObject = (JSONObject) obj;
            String id = (String) rolloutObject.get("id");
            List<Experiment> experiments = parseExperiments((JSONArray) rolloutObject.get("experiments"));

            rollouts.add(new Rollout(id, experiments));
        }

        return rollouts;
    }

    @Override
    public String toJson(Object src) {
        return JSONValue.toJSONString(src);
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) throws ConfigParseException {
        if (Map.class.isAssignableFrom(clazz)) {
            org.json.JSONObject obj = new org.json.JSONObject(json);
            return (T)JsonHelpers.jsonObjectToMap(obj);
        }

        // org.json.simple does not support parsing to user objects
        throw new ConfigParseException("Parsing fails with a unsupported type");
    }

}


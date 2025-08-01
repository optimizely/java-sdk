/**
 *
 *    Copyright 2016-2022, Optimizely and contributors
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
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.AudienceIdCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.UserAttribute;
import com.optimizely.ab.internal.ConditionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * {@code org.json}-based config parser implementation.
 */
final public class JsonConfigParser implements ConfigParser {

    @Override
    public ProjectConfig parseProjectConfig(@Nonnull String json) throws ConfigParseException {
        try {
            JSONObject rootObject = new JSONObject(json);

            String accountId = rootObject.getString("accountId");
            String projectId = rootObject.getString("projectId");
            String revision = rootObject.getString("revision");
            String version = rootObject.getString("version");
            int datafileVersion = Integer.parseInt(version);

            List<Experiment> experiments = parseExperiments(rootObject.getJSONArray("experiments"));

            List<Holdout> holdouts;
            if (rootObject.has("holdouts")) {
                holdouts = parseHoldouts(rootObject.getJSONArray("holdouts"));
            } else {
                holdouts = Collections.emptyList();
            }

            List<Attribute> attributes;
            attributes = parseAttributes(rootObject.getJSONArray("attributes"));

            List<EventType> events = parseEvents(rootObject.getJSONArray("events"));
            List<Audience> audiences = Collections.emptyList();

            if (rootObject.has("audiences")) {
                audiences = parseAudiences(rootObject.getJSONArray("audiences"));
            }

            List<Audience> typedAudiences = null;
            if (rootObject.has("typedAudiences")) {
                typedAudiences = parseTypedAudiences(rootObject.getJSONArray("typedAudiences"));
            }

            List<Group> groups = parseGroups(rootObject.getJSONArray("groups"));

            boolean anonymizeIP = false;
            if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V3.toString())) {
                anonymizeIP = rootObject.getBoolean("anonymizeIP");
            }

            List<FeatureFlag> featureFlags = null;
            List<Rollout> rollouts = null;
            List<Integration> integrations = null;
            String sdkKey = null;
            String environmentKey = null;
            Boolean botFiltering = null;
            boolean sendFlagDecisions = false;
            if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString())) {
                featureFlags = parseFeatureFlags(rootObject.getJSONArray("featureFlags"));
                rollouts = parseRollouts(rootObject.getJSONArray("rollouts"));
                if (rootObject.has("integrations")) {
                    integrations = parseIntegrations(rootObject.getJSONArray("integrations"));
                }
                if (rootObject.has("sdkKey"))
                    sdkKey = rootObject.getString("sdkKey");
                if (rootObject.has("environmentKey"))
                    environmentKey = rootObject.getString("environmentKey");
                if (rootObject.has("botFiltering"))
                    botFiltering = rootObject.getBoolean("botFiltering");
                if (rootObject.has("sendFlagDecisions"))
                    sendFlagDecisions = rootObject.getBoolean("sendFlagDecisions");
            }

            String region = "US"; // Default to US
            if (rootObject.has("region")) {
                String regionString = rootObject.getString("region");
            }

            return new DatafileProjectConfig(
                accountId,
                anonymizeIP,
                sendFlagDecisions,
                botFiltering,
                region,
                projectId,
                revision,
                sdkKey,
                environmentKey,
                version,
                attributes,
                audiences,
                typedAudiences,
                events,
                experiments,
                holdouts,
                featureFlags,
                groups,
                rollouts,
                integrations
            );
        } catch (RuntimeException e) {
            throw new ConfigParseException("Unable to parse datafile: " + json, e);
        } catch (Exception e) {
            throw new ConfigParseException("Unable to parse datafile: " + json, e);
        }
    }

    //======== Helper methods ========//

    private List<Experiment> parseExperiments(JSONArray experimentJson) {
        return parseExperiments(experimentJson, "");
    }

    private List<Experiment> parseExperiments(JSONArray experimentJson, String groupId) {
        List<Experiment> experiments = new ArrayList<Experiment>(experimentJson.length());

        for (int i = 0; i < experimentJson.length(); i++) {
            Object obj = experimentJson.get(i);
            JSONObject experimentObject = (JSONObject) obj;
            String id = experimentObject.getString("id");
            String key = experimentObject.getString("key");
            String status = experimentObject.isNull("status") ?
                ExperimentStatus.NOT_STARTED.toString() : experimentObject.getString("status");
            String layerId = experimentObject.has("layerId") ? experimentObject.getString("layerId") : null;

            JSONArray audienceIdsJson = experimentObject.getJSONArray("audienceIds");
            List<String> audienceIds = new ArrayList<String>(audienceIdsJson.length());

            for (int j = 0; j < audienceIdsJson.length(); j++) {
                Object audienceIdObj = audienceIdsJson.get(j);
                audienceIds.add((String) audienceIdObj);
            }

            Condition conditions = null;
            if (experimentObject.has("audienceConditions")) {
                Object jsonCondition = experimentObject.get("audienceConditions");
                conditions = ConditionUtils.<AudienceIdCondition>parseConditions(AudienceIdCondition.class, jsonCondition);
            }

            // parse the child objects
            List<Variation> variations = parseVariations(experimentObject.getJSONArray("variations"));
            Map<String, String> userIdToVariationKeyMap =
                parseForcedVariations(experimentObject.getJSONObject("forcedVariations"));
            List<TrafficAllocation> trafficAllocations =
                parseTrafficAllocation(experimentObject.getJSONArray("trafficAllocation"));

            experiments.add(new Experiment(id, key, status, layerId, audienceIds, conditions, variations, userIdToVariationKeyMap,
                trafficAllocations, groupId));
        }

        return experiments;
    }
    
    private List<Holdout> parseHoldouts(JSONArray holdoutJson) {
        List<Holdout> holdouts = new ArrayList<Holdout>(holdoutJson.length());

        for (int i = 0; i < holdoutJson.length(); i++) {
            Object obj = holdoutJson.get(i);
            JSONObject holdoutObject = (JSONObject) obj;
            String id = holdoutObject.getString("id");
            String key = holdoutObject.getString("key");
            String status = holdoutObject.getString("status");

            JSONArray audienceIdsJson = holdoutObject.getJSONArray("audienceIds");
            List<String> audienceIds = new ArrayList<String>(audienceIdsJson.length());

            for (int j = 0; j < audienceIdsJson.length(); j++) {
                Object audienceIdObj = audienceIdsJson.get(j);
                audienceIds.add((String) audienceIdObj);
            }

            Condition conditions = null;
            if (holdoutObject.has("audienceConditions")) {
                Object jsonCondition = holdoutObject.get("audienceConditions");
                conditions = ConditionUtils.<AudienceIdCondition>parseConditions(AudienceIdCondition.class, jsonCondition);
            }

            // parse the child objects
            List<Variation> variations = parseVariations(holdoutObject.getJSONArray("variations"));

            List<TrafficAllocation> trafficAllocations =
                parseTrafficAllocation(holdoutObject.getJSONArray("trafficAllocation"));

            List<String> includedFlags;
            if (holdoutObject.has("includedFlags")) {
                JSONArray includedIdsJson = holdoutObject.getJSONArray("includedFlags");
                includedFlags = new ArrayList<>(includedIdsJson.length());

                for (int j = 0; j < includedIdsJson.length(); j++) {
                    Object idObj = includedIdsJson.get(j);
                    includedFlags.add((String) idObj);
                }
            } else {
                includedFlags = Collections.emptyList();
            }

            List<String> excludedFlags;
            if (holdoutObject.has("excludedFlags")) {
                JSONArray excludedIdsJson = holdoutObject.getJSONArray("excludedFlags");
                excludedFlags = new ArrayList<>(excludedIdsJson.length());

                for (int j = 0; j < excludedIdsJson.length(); j++) {
                    Object idObj = excludedIdsJson.get(j);
                    excludedFlags.add((String) idObj);
                }
            } else {
                excludedFlags = Collections.emptyList();
            }

            holdouts.add(new Holdout(id, key, status, audienceIds, conditions, variations,
                trafficAllocations, includedFlags, excludedFlags));
        }

        return holdouts;
    }

    private List<String> parseExperimentIds(JSONArray experimentIdsJson) {
        ArrayList<String> experimentIds = new ArrayList<String>(experimentIdsJson.length());

        for (int i = 0; i < experimentIdsJson.length(); i++) {
            Object experimentIdObj = experimentIdsJson.get(i);
            experimentIds.add((String) experimentIdObj);
        }

        return experimentIds;
    }

    private List<FeatureFlag> parseFeatureFlags(JSONArray featureFlagJson) {
        List<FeatureFlag> featureFlags = new ArrayList<FeatureFlag>(featureFlagJson.length());

        for (int i = 0; i < featureFlagJson.length();i++) {
            Object obj = featureFlagJson.get(i);
            JSONObject featureFlagObject = (JSONObject) obj;
            String id = featureFlagObject.getString("id");
            String key = featureFlagObject.getString("key");
            String layerId = featureFlagObject.getString("rolloutId");

            List<String> experimentIds = parseExperimentIds(featureFlagObject.getJSONArray("experimentIds"));

            List<FeatureVariable> variables = parseFeatureVariables(featureFlagObject.getJSONArray("variables"));

            featureFlags.add(new FeatureFlag(
                id,
                key,
                layerId,
                experimentIds,
                variables
            ));
        }

        return featureFlags;
    }

    private List<Variation> parseVariations(JSONArray variationJson) {
        List<Variation> variations = new ArrayList<Variation>(variationJson.length());

        for (int i = 0; i < variationJson.length(); i++) {
            Object obj = variationJson.get(i);
            JSONObject variationObject = (JSONObject) obj;
            String id = variationObject.getString("id");
            String key = variationObject.getString("key");
            Boolean featureEnabled = false;

            if (variationObject.has("featureEnabled") && !variationObject.isNull("featureEnabled")) {
                featureEnabled = variationObject.getBoolean("featureEnabled");
            }

            List<FeatureVariableUsageInstance> featureVariableUsageInstances = null;
            if (variationObject.has("variables")) {
                featureVariableUsageInstances =
                    parseFeatureVariableInstances(variationObject.getJSONArray("variables"));
            }

            variations.add(new Variation(id, key, featureEnabled, featureVariableUsageInstances));
        }

        return variations;
    }

    private Map<String, String> parseForcedVariations(JSONObject forcedVariationJson) {
        Map<String, String> userIdToVariationKeyMap = new HashMap<String, String>();
        Set<String> userIdSet = forcedVariationJson.keySet();

        for (String userId : userIdSet) {
            userIdToVariationKeyMap.put(userId, forcedVariationJson.get(userId).toString());
        }

        return userIdToVariationKeyMap;
    }

    private List<TrafficAllocation> parseTrafficAllocation(JSONArray trafficAllocationJson) {
        List<TrafficAllocation> trafficAllocation = new ArrayList<TrafficAllocation>(trafficAllocationJson.length());

        for (int i = 0; i < trafficAllocationJson.length();i++) {
            Object obj = trafficAllocationJson.get(i);
            JSONObject allocationObject = (JSONObject) obj;
            String entityId = allocationObject.getString("entityId");
            int endOfRange = allocationObject.getInt("endOfRange");

            trafficAllocation.add(new TrafficAllocation(entityId, endOfRange));
        }

        return trafficAllocation;
    }

    private List<Attribute> parseAttributes(JSONArray attributeJson) {
        List<Attribute> attributes = new ArrayList<Attribute>(attributeJson.length());

        for (int i = 0; i < attributeJson.length();i++) {
            Object obj = attributeJson.get(i);
            JSONObject attributeObject = (JSONObject) obj;
            String id = attributeObject.getString("id");
            String key = attributeObject.getString("key");

            attributes.add(new Attribute(id, key, attributeObject.optString("segmentId", null)));
        }

        return attributes;
    }

    private List<EventType> parseEvents(JSONArray eventJson) {
        List<EventType> events = new ArrayList<EventType>(eventJson.length());

        for (int i = 0; i < eventJson.length(); i++) {
            Object obj = eventJson.get(i);
            JSONObject eventObject = (JSONObject) obj;
            List<String> experimentIds = parseExperimentIds(eventObject.getJSONArray("experimentIds"));

            String id = eventObject.getString("id");
            String key = eventObject.getString("key");

            events.add(new EventType(id, key, experimentIds));
        }

        return events;
    }

    private List<Audience> parseAudiences(JSONArray audienceJson) {
        List<Audience> audiences = new ArrayList<Audience>(audienceJson.length());

        for (int i = 0; i < audienceJson.length(); i++) {
            Object obj = audienceJson.get(i);
            JSONObject audienceObject = (JSONObject) obj;
            String id = audienceObject.getString("id");
            String key = audienceObject.getString("name");
            Object conditionsObject = audienceObject.get("conditions");
            if (conditionsObject instanceof String) { // should always be true
                JSONTokener tokener = new JSONTokener((String) conditionsObject);
                char token = tokener.nextClean();
                if (token == '[') {
                    // must be an array
                    conditionsObject = new JSONArray((String) conditionsObject);
                } else if (token == '{') {
                    conditionsObject = new JSONObject((String) conditionsObject);
                }
            }

            Condition conditions = ConditionUtils.<UserAttribute>parseConditions(UserAttribute.class, conditionsObject);
            audiences.add(new Audience(id, key, conditions));
        }

        return audiences;
    }

    private List<Audience> parseTypedAudiences(JSONArray audienceJson) {
        List<Audience> audiences = new ArrayList<Audience>(audienceJson.length());

        for (int i = 0; i < audienceJson.length(); i++) {
            Object obj = audienceJson.get(i);
            JSONObject audienceObject = (JSONObject) obj;
            String id = audienceObject.getString("id");
            String key = audienceObject.getString("name");
            Object conditionsObject = audienceObject.get("conditions");

            Condition conditions = ConditionUtils.<UserAttribute>parseConditions(UserAttribute.class, conditionsObject);
            audiences.add(new Audience(id, key, conditions));
        }

        return audiences;
    }

    private List<Group> parseGroups(JSONArray groupJson) {
        List<Group> groups = new ArrayList<Group>(groupJson.length());

        for (int i = 0; i < groupJson.length(); i++) {
            Object obj = groupJson.get(i);
            JSONObject groupObject = (JSONObject) obj;
            String id = groupObject.getString("id");
            String policy = groupObject.getString("policy");
            List<Experiment> experiments = parseExperiments(groupObject.getJSONArray("experiments"), id);
            List<TrafficAllocation> trafficAllocations =
                parseTrafficAllocation(groupObject.getJSONArray("trafficAllocation"));

            groups.add(new Group(id, policy, experiments, trafficAllocations));
        }

        return groups;
    }

    private List<FeatureVariable> parseFeatureVariables(JSONArray featureVariablesJson) {
        List<FeatureVariable> featureVariables = new ArrayList<FeatureVariable>(featureVariablesJson.length());

        for (int i = 0; i < featureVariablesJson.length();i++) {
            Object obj = featureVariablesJson.get(i);
            JSONObject FeatureVariableObject = (JSONObject) obj;
            String id = FeatureVariableObject.getString("id");
            String key = FeatureVariableObject.getString("key");
            String defaultValue = FeatureVariableObject.getString("defaultValue");
            String type = FeatureVariableObject.getString("type");
            String subType = null;
            if (FeatureVariableObject.has("subType")) {
                subType = FeatureVariableObject.getString("subType");
            }
            FeatureVariable.VariableStatus status = null;
            if (FeatureVariableObject.has("status")) {
                status = FeatureVariable.VariableStatus.fromString(FeatureVariableObject.getString("status"));
            }

            featureVariables.add(new FeatureVariable(id, key, defaultValue, status, type, subType));
        }

        return featureVariables;
    }

    private List<FeatureVariableUsageInstance> parseFeatureVariableInstances(JSONArray featureVariableInstancesJson) {
        List<FeatureVariableUsageInstance> featureVariableUsageInstances = new ArrayList<FeatureVariableUsageInstance>(featureVariableInstancesJson.length());

        for (int i = 0; i < featureVariableInstancesJson.length(); i++) {
            Object obj = featureVariableInstancesJson.get(i);
            JSONObject featureVariableInstanceObject = (JSONObject) obj;
            String id = featureVariableInstanceObject.getString("id");
            String value = featureVariableInstanceObject.getString("value");

            featureVariableUsageInstances.add(new FeatureVariableUsageInstance(id, value));
        }

        return featureVariableUsageInstances;
    }

    private List<Rollout> parseRollouts(JSONArray rolloutsJson) {
        List<Rollout> rollouts = new ArrayList<Rollout>(rolloutsJson.length());

        for (int i = 0; i < rolloutsJson.length(); i++) {
            Object obj = rolloutsJson.get(i);
            JSONObject rolloutObject = (JSONObject) obj;
            String id = rolloutObject.getString("id");
            List<Experiment> experiments = parseExperiments(rolloutObject.getJSONArray("experiments"));

            rollouts.add(new Rollout(id, experiments));
        }

        return rollouts;
    }

    private List<Integration> parseIntegrations(JSONArray integrationsJson) {
        List<Integration> integrations = new ArrayList<Integration>(integrationsJson.length());

        for (int i = 0; i < integrationsJson.length(); i++) {
            Object obj = integrationsJson.get(i);
            JSONObject integrationObject = (JSONObject) obj;
            String key = integrationObject.getString("key");
            String host = integrationObject.has("host") ? integrationObject.getString("host") : null;
            String publicKey = integrationObject.has("publicKey") ? integrationObject.getString("publicKey") : null;
            integrations.add(new Integration(key, host, publicKey));
        }

        return integrations;
    }

    @Override
    public String toJson(Object src) {
        JSONObject json = (JSONObject)JsonHelpers.convertToJsonObject(src);
        return json.toString();
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) throws JsonParseException {
        if (Map.class.isAssignableFrom(clazz)) {
            JSONObject obj = new JSONObject(json);
            return (T)JsonHelpers.jsonObjectToMap(obj);
        }

        // org.json parser does not support parsing to user objects
        throw new JsonParseException("Parsing fails with a unsupported type");
    }

}

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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.optimizely.ab.config.*;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.TypedAudience;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * GSON {@link DatafileProjectConfig} deserializer to allow the constructor to be used.
 */
public class DatafileGsonDeserializer implements JsonDeserializer<ProjectConfig> {

    @Override
    public ProjectConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        String accountId = jsonObject.get("accountId").getAsString();
        String projectId = jsonObject.get("projectId").getAsString();
        String revision = jsonObject.get("revision").getAsString();
        String version = jsonObject.get("version").getAsString();
        int datafileVersion = Integer.parseInt(version);

        // generic list type tokens
        Type groupsType = new TypeToken<List<Group>>() {
        }.getType();
        Type experimentsType = new TypeToken<List<Experiment>>() {
        }.getType();
        Type attributesType = new TypeToken<List<Attribute>>() {
        }.getType();
        Type eventsType = new TypeToken<List<EventType>>() {
        }.getType();
        Type audienceType = new TypeToken<List<Audience>>() {
        }.getType();
        Type typedAudienceType = new TypeToken<List<TypedAudience>>() {
        }.getType();

        List<Group> groups = context.deserialize(jsonObject.get("groups").getAsJsonArray(), groupsType);
        List<Experiment> experiments =
            context.deserialize(jsonObject.get("experiments").getAsJsonArray(), experimentsType);

        List<Attribute> attributes;
        attributes = context.deserialize(jsonObject.get("attributes"), attributesType);

        List<EventType> events =
            context.deserialize(jsonObject.get("events").getAsJsonArray(), eventsType);
        List<Audience> audiences = Collections.emptyList();
        if (jsonObject.has("audiences")) {
            audiences = context.deserialize(jsonObject.get("audiences").getAsJsonArray(), audienceType);
        }

        List<Audience> typedAudiences = null;
        if (jsonObject.has("typedAudiences")) {
            typedAudiences = context.deserialize(jsonObject.get("typedAudiences").getAsJsonArray(), typedAudienceType);
        }
        boolean anonymizeIP = false;
        if (datafileVersion >= Integer.parseInt(DatafileProjectConfig.Version.V3.toString())) {
            anonymizeIP = jsonObject.get("anonymizeIP").getAsBoolean();
        }


        List<FeatureFlag> featureFlags = null;
        List<Rollout> rollouts = null;
        List<Integration> integrations = null;
        Boolean botFiltering = null;
        String sdkKey = null;
        String environmentKey = null;
        boolean sendFlagDecisions = false;
        if (datafileVersion >= Integer.parseInt(DatafileProjectConfig.Version.V4.toString())) {
            Type featureFlagsType = new TypeToken<List<FeatureFlag>>() {
            }.getType();
            featureFlags = context.deserialize(jsonObject.getAsJsonArray("featureFlags"), featureFlagsType);
            Type rolloutsType = new TypeToken<List<Rollout>>() {
            }.getType();
            rollouts = context.deserialize(jsonObject.get("rollouts").getAsJsonArray(), rolloutsType);
            if (jsonObject.has("integrations")) {
                Type integrationsType = new TypeToken<List<Integration>>() {}.getType();
                integrations = context.deserialize(jsonObject.get("integrations").getAsJsonArray(), integrationsType);
            }
            if (jsonObject.has("sdkKey"))
                sdkKey = jsonObject.get("sdkKey").getAsString();
            if (jsonObject.has("environmentKey"))
                environmentKey = jsonObject.get("environmentKey").getAsString();
            if (jsonObject.has("botFiltering"))
                botFiltering = jsonObject.get("botFiltering").getAsBoolean();
            if (jsonObject.has("sendFlagDecisions"))
                sendFlagDecisions = jsonObject.get("sendFlagDecisions").getAsBoolean();
        }

        ProjectConfig.Region region = ProjectConfig.Region.US;

        if (jsonObject.has("region")) {
            region = ProjectConfig.Region.valueOf(jsonObject.get("region").getAsString());
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
            featureFlags,
            groups,
            rollouts,
            integrations
        );
    }
}

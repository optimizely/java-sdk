/**
 *
 *    Copyright 2016-2018, Optimizely and contributors
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.optimizely.ab.config.*;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

class ProjectConfigJacksonDeserializer extends JsonDeserializer<ProjectConfig> {
    @Override
    public ProjectConfig deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

        String accountId = node.get("accountId").textValue();
        String projectId = node.get("projectId").textValue();
        String revision = node.get("revision").textValue();
        String version = node.get("version").textValue();
        int datafileVersion = Integer.parseInt(version);

        List<Group> groups = JacksonHelpers.arrayNodeToList(node.get("groups"), Group.class, codec);
        List<Experiment> experiments = JacksonHelpers.arrayNodeToList(node.get("experiments"), Experiment.class, codec);
        List<Attribute> attributes = JacksonHelpers.arrayNodeToList(node.get("attributes"), Attribute.class, codec);
        List<EventType> events = JacksonHelpers.arrayNodeToList(node.get("events"), EventType.class, codec);

        List<Audience> audiences = Collections.emptyList();
        if (node.has("audiences")) {
            audiences = JacksonHelpers.arrayNodeToList(node.get("audiences"), Audience.class, codec);
        }

        List<Audience> typedAudiences = null;
        if (node.has("typedAudiences")) {
            typedAudiences = JacksonHelpers.arrayNodeToList(node.get("typedAudiences"), Audience.class, codec);
        }

        boolean anonymizeIP = false;
        List<LiveVariable> liveVariables = null;
        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V3.toString())) {
            liveVariables = JacksonHelpers.arrayNodeToList(node.get("variables"), LiveVariable.class, codec);
            anonymizeIP = node.get("anonymizeIP").asBoolean();
        }

        List<FeatureFlag> featureFlags = null;
        List<Rollout> rollouts = null;
        Boolean botFiltering = null;
        if (datafileVersion >= Integer.parseInt(ProjectConfig.Version.V4.toString())) {
            featureFlags = JacksonHelpers.arrayNodeToList(node.get("featureFlags"), FeatureFlag.class, codec);
            rollouts = JacksonHelpers.arrayNodeToList(node.get("rollouts"), Rollout.class, codec);
            if (node.hasNonNull("botFiltering")) {
                botFiltering = node.get("botFiltering").asBoolean();
            }
        }

        return new ProjectConfig(
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
                liveVariables,
                rollouts
        );
    }

}
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.optimizely.ab.config.*;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.TypedAudience;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

class DatafileJacksonDeserializer extends JsonDeserializer<DatafileProjectConfig> {
    @Override
    public DatafileProjectConfig deserialize(JsonParser parser, DeserializationContext context) throws IOException {
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

        List<Holdout> holdouts;
        if (node.has("holdouts")) {
             holdouts = JacksonHelpers.arrayNodeToList(node.get("holdouts"), Holdout.class, codec);
        } else {
            holdouts = Collections.emptyList();
        }

        List<Audience> audiences = Collections.emptyList();
        if (node.has("audiences")) {
            audiences = JacksonHelpers.arrayNodeToList(node.get("audiences"), Audience.class, codec);
        }

        List<TypedAudience> typedAudiences = null;
        if (node.has("typedAudiences")) {
            typedAudiences = JacksonHelpers.arrayNodeToList(node.get("typedAudiences"), TypedAudience.class, codec);
        }

        boolean anonymizeIP = false;
        if (datafileVersion >= Integer.parseInt(DatafileProjectConfig.Version.V3.toString())) {
            anonymizeIP = node.get("anonymizeIP").asBoolean();
        }

        List<FeatureFlag> featureFlags = null;
        List<Rollout> rollouts = null;
        List<Integration> integrations = null;
        String sdkKey = null;
        String environmentKey = null;
        Boolean botFiltering = null;
        boolean sendFlagDecisions = false;
        if (datafileVersion >= Integer.parseInt(DatafileProjectConfig.Version.V4.toString())) {
            featureFlags = JacksonHelpers.arrayNodeToList(node.get("featureFlags"), FeatureFlag.class, codec);
            rollouts = JacksonHelpers.arrayNodeToList(node.get("rollouts"), Rollout.class, codec);
            if (node.hasNonNull("integrations")) {
                integrations = JacksonHelpers.arrayNodeToList(node.get("integrations"), Integration.class, codec);
            }
            if (node.hasNonNull("sdkKey")) {
                sdkKey = node.get("sdkKey").textValue();
            }
            if (node.hasNonNull("environmentKey")) {
                environmentKey = node.get("environmentKey").textValue();
            }
            if (node.hasNonNull("botFiltering")) {
                botFiltering = node.get("botFiltering").asBoolean();
            }
            if (node.hasNonNull("sendFlagDecisions")) {
                sendFlagDecisions = node.get("sendFlagDecisions").asBoolean();
            }
        }

        String region = "US";

        if (node.hasNonNull("region")) {
            region = node.get("region").textValue();
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
            (List<Audience>) (List<? extends Audience>) typedAudiences,
            events,
            experiments,
            holdouts,
            featureFlags,
            groups,
            rollouts,
            integrations
        );
    }

}
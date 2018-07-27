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
package com.optimizely.ab.event.internal;

import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.Attribute;
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.event.internal.payload.Event;
import com.optimizely.ab.event.internal.payload.Snapshot;
import com.optimizely.ab.event.internal.payload.Visitor;
import com.optimizely.ab.event.internal.serializer.DefaultJsonSerializer;
import com.optimizely.ab.event.internal.serializer.Serializer;
import com.optimizely.ab.internal.EventTagUtils;
import com.optimizely.ab.internal.ControlAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EventBuilder {
    private static final Logger logger = LoggerFactory.getLogger(EventBuilder.class);
    static final String EVENT_ENDPOINT = "https://logx.optimizely.com/v1/events";  // Should be part of the datafile
    static final String  ACTIVATE_EVENT_KEY = "campaign_activated";

    private Serializer serializer;
    @VisibleForTesting
    public final String clientVersion;
    @VisibleForTesting
    public final EventBatch.ClientEngine clientEngine;

    public EventBuilder() {
        this(EventBatch.ClientEngine.JAVA_SDK, BuildVersionInfo.VERSION);
    }

    public EventBuilder(EventBatch.ClientEngine clientEngine, String clientVersion) {
        this.clientEngine = clientEngine;
        this.clientVersion = clientVersion;
        this.serializer = DefaultJsonSerializer.getInstance();
    }

    public LogEvent createImpressionEvent(@Nonnull ProjectConfig projectConfig,
                                          @Nonnull Experiment activatedExperiment,
                                          @Nonnull Variation variation,
                                          @Nonnull String userId,
                                          @Nonnull Map<String, String> attributes) {

        Decision decision = new Decision.Builder()
                .setCampaignId(activatedExperiment.getLayerId())
                .setExperimentId(activatedExperiment.getId())
                .setVariationId(variation.getId())
                .setIsCampaignHoldback(false)
                .build();

        Event impressionEvent = new Event.Builder()
                .setTimestamp(System.currentTimeMillis())
                .setUuid(UUID.randomUUID().toString())
                .setEntityId(activatedExperiment.getLayerId())
                .setKey(ACTIVATE_EVENT_KEY)
                .setType(ACTIVATE_EVENT_KEY)
                .build();

        Snapshot snapshot = new Snapshot.Builder()
                .setDecisions(Collections.singletonList(decision))
                .setEvents(Collections.singletonList(impressionEvent))
                .build();

        Visitor visitor = new Visitor.Builder()
                .setVisitorId(userId)
                .setAttributes(buildAttributeList(projectConfig, attributes))
                .setSnapshots(Collections.singletonList((snapshot)))
                .build();

        EventBatch eventBatch = new EventBatch.Builder()
                .setClientName(clientEngine.getClientEngineValue())
                .setClientVersion(clientVersion)
                .setAccountId(projectConfig.getAccountId())
                .setVisitors(Collections.singletonList(visitor))
                .setAnonymizeIp(projectConfig.getAnonymizeIP())
                .setProjectId(projectConfig.getProjectId())
                .setRevision(projectConfig.getRevision())
                .build();

        String payload = this.serializer.serialize(eventBatch);
        return new LogEvent(LogEvent.RequestMethod.POST, EVENT_ENDPOINT, Collections.<String, String>emptyMap(), payload);
    }

    public LogEvent createConversionEvent(@Nonnull ProjectConfig projectConfig,
                                          @Nonnull Map<Experiment, Variation> experimentVariationMap,
                                          @Nonnull String userId,
                                          @Nonnull String eventId, // Why is this not used?
                                          @Nonnull String eventName,
                                          @Nonnull Map<String, String> attributes,
                                          @Nonnull Map<String, ?> eventTags) {

        if (experimentVariationMap.isEmpty()) {
            return null;
        }

        ArrayList<Decision> decisions = new ArrayList<Decision>(experimentVariationMap.size());
        for (Map.Entry<Experiment, Variation> entry : experimentVariationMap.entrySet()) {
            Decision decision = new Decision.Builder()
                    .setCampaignId(entry.getKey().getLayerId())
                    .setExperimentId(entry.getKey().getId())
                    .setVariationId(entry.getValue().getId())
                    .setIsCampaignHoldback(false)
                    .build();

            decisions.add(decision);
        }

        EventType eventType = projectConfig.getEventNameMapping().get(eventName);

        Event conversionEvent = new Event.Builder()
                .setTimestamp(System.currentTimeMillis())
                .setUuid(UUID.randomUUID().toString())
                .setEntityId(eventType.getId())
                .setKey(eventType.getKey())
                .setRevenue(EventTagUtils.getRevenueValue(eventTags))
                .setTags(eventTags)
                .setType(eventType.getKey())
                .setValue(EventTagUtils.getNumericValue(eventTags))
                .build();

        Snapshot snapshot = new Snapshot.Builder()
                .setDecisions(decisions)
                .setEvents(Collections.singletonList((conversionEvent)))
                .build();

        Visitor visitor = new Visitor.Builder()
                .setVisitorId(userId)
                .setAttributes(buildAttributeList(projectConfig, attributes))
                .setSnapshots(Collections.singletonList(snapshot))
                .build();

        EventBatch eventBatch = new EventBatch.Builder()
                .setClientName(clientEngine.getClientEngineValue())
                .setClientVersion(clientVersion)
                .setAccountId(projectConfig.getAccountId())
                .setVisitors(Collections.singletonList(visitor))
                .setAnonymizeIp(projectConfig.getAnonymizeIP())
                .setProjectId(projectConfig.getProjectId())
                .setRevision(projectConfig.getRevision())
                .build();

        String payload = this.serializer.serialize(eventBatch);
        return new LogEvent(LogEvent.RequestMethod.POST, EVENT_ENDPOINT, Collections.<String, String>emptyMap(), payload);
    }

    private List<Attribute> buildAttributeList(ProjectConfig projectConfig, Map<String, String> attributes) {
        List<Attribute> attributesList = new ArrayList<Attribute>();

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String attributeId = projectConfig.getAttributeId(projectConfig, entry.getKey());
            if(attributeId == null) {
                continue;
            }

            Attribute attribute = new Attribute.Builder()
                    .setEntityId(attributeId)
                    .setKey(entry.getKey())
                    .setType(Attribute.CUSTOM_ATTRIBUTE_TYPE)
                    .setValue(entry.getValue())
                    .build();

            attributesList.add(attribute);
        }

        //checks if botFiltering value is not set in the project config file.
        if(projectConfig.getBotFiltering() != null) {
            Attribute attribute = new Attribute.Builder()
                    .setEntityId(ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString())
                    .setKey(ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString())
                    .setType(Attribute.CUSTOM_ATTRIBUTE_TYPE)
                    .setValue(projectConfig.getBotFiltering())
                    .build();

            attributesList.add(attribute);
        }

        return attributesList;
    }
}

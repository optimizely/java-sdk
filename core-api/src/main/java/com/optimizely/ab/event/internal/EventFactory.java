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

public class EventFactory {
    private static final Logger logger = LoggerFactory.getLogger(EventFactory.class);
    static final String EVENT_ENDPOINT = "https://logx.optimizely.com/v1/events";  // Should be part of the datafile
    static final String ACTIVATE_EVENT_KEY = "campaign_activated";

    @VisibleForTesting
    public final String clientVersion;
    @VisibleForTesting
    public final EventBatch.ClientEngine clientEngine;

    public EventFactory() {
        this(EventBatch.ClientEngine.JAVA_SDK, BuildVersionInfo.VERSION);
    }

    public EventFactory(EventBatch.ClientEngine clientEngine, String clientVersion) {
        this.clientEngine = clientEngine;
        this.clientVersion = clientVersion;
    }

    public LogEvent createImpressionEvent(@Nonnull ProjectConfig projectConfig,
                                          @Nonnull Experiment activatedExperiment,
                                          @Nonnull Variation variation,
                                          @Nonnull String userId,
                                          @Nonnull Map<String, ?> attributes) {

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

        return new LogEvent(LogEvent.RequestMethod.POST, EVENT_ENDPOINT, Collections.<String, String>emptyMap(), eventBatch);
    }

    public LogEvent createConversionEvent(@Nonnull ProjectConfig projectConfig,
                                          @Nonnull String userId,
                                          @Nonnull String eventId, // Why is this not used?
                                          @Nonnull String eventName,
                                          @Nonnull Map<String, ?> attributes,
                                          @Nonnull Map<String, ?> eventTags) {

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
                .setEvents(Collections.singletonList(conversionEvent))
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

        return new LogEvent(LogEvent.RequestMethod.POST, EVENT_ENDPOINT, Collections.<String, String>emptyMap(), eventBatch);
    }

    private List<Attribute> buildAttributeList(ProjectConfig projectConfig, Map<String, ?> attributes) {
        List<Attribute> attributesList = new ArrayList<Attribute>();

        if (attributes != null) {
            for (Map.Entry<String, ?> entry : attributes.entrySet()) {

                // Ignore attributes with empty key
                if (entry.getKey().isEmpty()) {
                    continue;
                }

                // Filter down to the types of values we're allowed to track.
                // Don't allow Longs, BigIntegers, or BigDecimals - they /can/ theoretically be serialized as JSON numbers
                // but may take on values that can't be faithfully parsed by the backend.
                // https://developers.optimizely.com/x/events/api/#Attribute
                if (entry.getValue() == null ||
                    !((entry.getValue() instanceof String) ||
                        (entry.getValue() instanceof Integer) ||
                        (entry.getValue() instanceof Double) ||
                        (entry.getValue() instanceof Boolean))) {
                    continue;
                }

                String attributeId = projectConfig.getAttributeId(projectConfig, entry.getKey());
                if (attributeId == null) {
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
        }

        //checks if botFiltering value is not set in the project config file.
        if (projectConfig.getBotFiltering() != null) {
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

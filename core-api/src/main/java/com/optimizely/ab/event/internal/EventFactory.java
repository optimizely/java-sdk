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
import com.optimizely.ab.api.ConversionEvent;
import com.optimizely.ab.api.EventContext;
import com.optimizely.ab.api.ImpressionEvent;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.Attribute;
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.Event;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.event.internal.payload.Snapshot;
import com.optimizely.ab.event.internal.payload.Visitor;
import com.optimizely.ab.internal.ControlAttribute;
import com.optimizely.ab.internal.EventTagUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.optimizely.ab.internal.AttributesUtil.isValidNumber;

public class EventFactory {
    private static final Logger logger = LoggerFactory.getLogger(EventFactory.class);
    static final String EVENT_ENDPOINT = "https://logx.optimizely.com/v1/events";  // Should be part of the datafile
    static final String ACTIVATE_EVENT_KEY = "campaign_activated";

    @VisibleForTesting
    public final String clientVersion;
    @VisibleForTesting
    public final EventBatch.ClientEngine clientEngine;

    private final String endpoint;
    private final Map<String, String> parameters;

    public EventFactory() {
        this(null, null);
    }

    public EventFactory(EventBatch.ClientEngine clientEngine, String clientVersion) {
        this(clientEngine, clientVersion, EVENT_ENDPOINT, Collections.emptyMap());
    }

    private EventFactory(
        EventBatch.ClientEngine clientEngine,
        String clientVersion,
        String endpoint,
        Map<String, String> parameters
    ) {
        this.clientEngine = clientEngine != null ? clientEngine : EventBatch.ClientEngine.JAVA_SDK;
        this.clientVersion = clientVersion != null ? clientVersion : BuildVersionInfo.VERSION;
        this.endpoint = endpoint;
        this.parameters = parameters;

        logger.debug("Optimizely client: {}/{}", this.clientEngine.getClientEngineValue(), this.clientVersion);
        logger.debug("Optimizely endpoint: {}", endpoint);
        if (!BuildVersionInfo.VERSION.equals(clientVersion)) {
            logger.warn("Optimizely client version is being overridden from {} to {}. This is NOT recommended.",
                BuildVersionInfo.VERSION,
                clientVersion);
        }
    }

    @Nonnull
    public LogEvent createLogEvent(@Nonnull EventBatch eventBatch) {
        return new LogEvent(LogEvent.RequestMethod.POST, endpoint, parameters, eventBatch);
    }

    @Nonnull
    public LogEvent createLogEvent(com.optimizely.ab.api.Event event) {
        Assert.notNull(event, "event");
        return createLogEvent(createEventBatch(event));
    }

    @Nonnull
    public EventBatch createEventBatch(com.optimizely.ab.api.Event event) {
        Assert.notNull(event, "event");
        Assert.isTrue(event.getType() != null, "Event must specify type");
        switch (event.getType()) {
            case CONVERSION:
                return createEventBatch((Conversion) event);
            case IMPRESSION:
                return createEventBatch((Impression) event);
            default: throw new IllegalArgumentException("Unhandled event type: " + event.getType());
        }
    }

    @Nonnull
    public EventBatch createEventBatch(ImpressionEvent impression) {
        Assert.notNull(impression, "impression");

        Variation variation = impression.getVariation();
        Experiment experiment = impression.getExperiment(); // TODO variation.getExperiment()
        EventContext context = impression.getContext();

        Decision decision = new Decision.Builder()
            .setCampaignId(experiment.getLayerId())
            .setExperimentId(experiment.getId())
            .setVariationId(variation.getId())
            .setIsCampaignHoldback(false)
            .build();

        Event impressionEvent = new Event.Builder()
            .setTimestamp(impression.getTimestamp().getTime())
            .setUuid(UUID.randomUUID().toString())
            .setEntityId(experiment.getLayerId())
            .setKey(ACTIVATE_EVENT_KEY)
            .setType(ACTIVATE_EVENT_KEY)
            .build();

        Snapshot snapshot = new Snapshot.Builder()
            .setDecisions(Collections.singletonList(decision))
            .setEvents(Collections.singletonList(impressionEvent))
            .build();

        Visitor visitor = new Visitor.Builder()
            .setVisitorId(impression.getUserId())
            .setAttributes(impression.getUserAttributes())
            .setSnapshots(Collections.singletonList((snapshot)))
            .build();

        return EventBatch.builder()
            .setClientName(clientEngine.getClientEngineValue())
            .setClientVersion(clientVersion)
            .setAccountId(context.getAccountId())
            .addVisitor(visitor)
            .setAnonymizeIp(context.getAnonymizeIp())
            .setProjectId(context.getProjectId())
            .setRevision(context.getRevision())
            .build();
    }

    @Deprecated
    public LogEvent createImpressionEvent(
        @Nonnull ProjectConfig projectConfig,
        @Nonnull Experiment activatedExperiment,
        @Nonnull Variation variation,
        @Nonnull String userId,
        @Nonnull Map<String, ?> attributes
    ) {
        return createLogEvent(createEventBatch(
            ImpressionEvent.builder()
                .context(createEventContext(projectConfig))
                .experiment(activatedExperiment)
                .variation(variation)
                .userId(userId)
                .userAttributes(buildAttributeList(projectConfig, attributes))
                .build(createEventContext(projectConfig))));
    }

    public ConversionEvent createConversion(
        @Nonnull ProjectConfig projectConfig,
        @Nonnull String userId,
        @Nonnull String eventName,
        @Nonnull Map<String, ?> attributes,
        @Nonnull Map<String, ?> eventTags
    ) {
        return conversionBuilder(projectConfig, userId, eventName, attributes, eventTags)
            .build(createEventContext(projectConfig));
    }

    public Conversion.Builder conversionBuilder(
        @Nonnull ProjectConfig projectConfig,
        @Nonnull String userId,
        @Nonnull String eventName,
        @Nonnull Map<String, ?> attributes,
        @Nonnull Map<String, ?> eventTags
    ) {
        return Conversion.builder()
            .context(createEventContext(projectConfig))
            .userId(userId)
            .event(projectConfig.getEventNameMapping().get(eventName))
            .userAttributes(buildAttributeList(projectConfig, attributes))
            .tags(eventTags);
    }

    public EventBatch createEventBatch(ConversionEvent conversion) {
        EventContext context = conversion.getContext();
        EventType event = conversion.getEvent();

        Event conversionEvent = new Event.Builder()
            .setTimestamp(System.currentTimeMillis())
            .setUuid(UUID.randomUUID().toString())
            .setEntityId(event.getId())
            .setKey(event.getKey())
            .setRevenue(EventTagUtils.getRevenueValue(conversion.getTags()))
            .setTags(conversion.getTags())
            .setType(event.getKey())
            .setValue(EventTagUtils.getNumericValue(conversion.getTags()))
            .build();

        Snapshot snapshot = new Snapshot.Builder()
            .setEvents(Collections.singletonList(conversionEvent))
            .build();

        Visitor visitor = new Visitor.Builder()
            .setVisitorId(conversion.getUserId())
            .setAttributes(conversion.getUserAttributes())
            .addSnapshot(snapshot)
            .build();

        return new EventBatch.Builder()
            .setClientName(clientEngine.getClientEngineValue())
            .setClientVersion(clientVersion)
            .setAccountId(context.getAccountId())
            .setVisitors(Collections.singletonList(visitor))
            .setAnonymizeIp(context.getAnonymizeIp())
            .setProjectId(context.getProjectId())
            .setRevision(context.getRevision())
            .build();
    }

    @Deprecated
    public LogEvent createConversionEvent(
        @Nonnull ProjectConfig projectConfig,
        @Nonnull String userId,
        @Nonnull String eventId, // Why is this not used?
        @Nonnull String eventName,
        @Nonnull Map<String, ?> attributes,
        @Nonnull Map<String, ?> eventTags
    ) {
        return createLogEvent(createEventBatch(
            createConversion(
                projectConfig,
                userId,
                eventName,
                attributes,
                eventTags
            )));
    }

    @Nonnull
    private EventContext createEventContext(ProjectConfig projectConfig) {
        return EventContext.create(projectConfig, clientEngine.getClientEngineValue(), clientVersion);
    }

    // TODO make private
    public static List<Attribute> buildAttributeList(ProjectConfig projectConfig, Map<String, ?> attributes) {
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
                        (entry.getValue() instanceof Boolean) ||
                        (isValidNumber(entry.getValue())))) {
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

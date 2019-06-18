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

import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.Attribute;
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.event.internal.payload.Event;
import com.optimizely.ab.event.internal.payload.Snapshot;
import com.optimizely.ab.event.internal.payload.Visitor;
import com.optimizely.ab.internal.ControlAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.internal.AttributesUtil.isValidNumber;

public class EventFactory {
    private static final Logger logger = LoggerFactory.getLogger(EventFactory.class);
    public static final String EVENT_ENDPOINT = "https://logx.optimizely.com/v1/events";  // Should be part of the datafile
    private static final String ACTIVATE_EVENT_KEY = "campaign_activated";

    public static LogEvent createLogEvent(UserEvent userEvent) {
        return createLogEvent(Collections.singletonList(userEvent));
    }

    public static LogEvent createLogEvent(List<UserEvent> userEvents) {
        EventBatch.Builder builder = new EventBatch.Builder();
        List<Visitor> visitors = new ArrayList<>(userEvents.size());

        for (UserEvent userEvent: userEvents) {

            if (userEvent == null) {
                continue;
            }

            if (userEvent instanceof ImpressionEvent) {
                visitors.add(createVisitor((ImpressionEvent) userEvent));
            }

            if (userEvent instanceof ConversionEvent) {
                visitors.add(createVisitor((ConversionEvent) userEvent));
            }

            // This needs an interface.
            UserContext userContext = userEvent.getUserContext();
            ProjectConfig projectConfig = userContext.getProjectConfig();

            builder
                .setClientName(ClientEngineInfo.getClientEngine().getClientEngineValue())
                .setClientVersion(BuildVersionInfo.VERSION)
                .setAccountId(projectConfig.getAccountId())
                .setAnonymizeIp(projectConfig.getAnonymizeIP())
                .setProjectId(projectConfig.getProjectId())
                .setRevision(projectConfig.getRevision());
        }

        if (visitors.isEmpty()) {
            return null;
        }

        builder.setVisitors(visitors);
        return new LogEvent(LogEvent.RequestMethod.POST, EVENT_ENDPOINT, Collections.emptyMap(), builder.build());
    }

    private static Visitor createVisitor(ImpressionEvent impressionEvent) {
        if (impressionEvent == null) {
            return null;
        }

        UserContext userContext = impressionEvent.getUserContext();

        Decision decision = new Decision.Builder()
            .setCampaignId(impressionEvent.getLayerId())
            .setExperimentId(impressionEvent.getExperimentId())
            .setVariationId(impressionEvent.getVariationId())
            .setIsCampaignHoldback(false)
            .build();

        Event event = new Event.Builder()
            .setTimestamp(userContext.getTimestamp())
            .setUuid(userContext.getUUID())
            .setEntityId(impressionEvent.getLayerId())
            .setKey(ACTIVATE_EVENT_KEY)
            .setType(ACTIVATE_EVENT_KEY)
            .build();

        Snapshot snapshot = new Snapshot.Builder()
            .setDecisions(Collections.singletonList(decision))
            .setEvents(Collections.singletonList(event))
            .build();

        return new Visitor.Builder()
            .setVisitorId(userContext.getUserId())
            .setAttributes(buildAttributeList(userContext.getProjectConfig(), userContext.getAttributes()))
            .setSnapshots(Collections.singletonList((snapshot)))
            .build();
    }

    public static Visitor createVisitor(ConversionEvent conversionEvent) {
        if (conversionEvent == null) {
            return null;
        }

        UserContext userContext = conversionEvent.getUserContext();

        Event event = new Event.Builder()
            .setTimestamp(userContext.getTimestamp())
            .setUuid(userContext.getUUID())
            .setEntityId(conversionEvent.getEventId())
            .setKey(conversionEvent.getEventKey())
            .setRevenue(conversionEvent.getRevenue())
            .setTags(conversionEvent.getTags())
            .setType(conversionEvent.getEventKey())
            .setValue(conversionEvent.getValue())
            .build();

        Snapshot snapshot = new Snapshot.Builder()
            .setEvents(Collections.singletonList(event))
            .build();

        return new Visitor.Builder()
            .setVisitorId(userContext.getUserId())
            .setAttributes(buildAttributeList(userContext.getProjectConfig(), userContext.getAttributes()))
            .setSnapshots(Collections.singletonList(snapshot))
            .build();
    }

    private static List<Attribute> buildAttributeList(ProjectConfig projectConfig, Map<String, ?> attributes) {
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

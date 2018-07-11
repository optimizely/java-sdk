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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EventBuilder {
    private static final Logger logger = LoggerFactory.getLogger(EventBuilder.class);
    static final String EVENT_ENDPOINT = "https://logx.optimizely.com/v1/events";
    static final String ACTIVATE_EVENT_KEY = "campaign_activated";
    static final String EVENT_TEMPLATE = "{\"revision\":\"${BATCHEVENT.REVISION}\",\"visitors\":[{\"attributes\":${VISITOR.ATTRIBUTES},\"snapshots\":[{\"decisions\":[{\"campaign_id\":\"${DECISION.CAMPAIGNID}\",\"experiment_id\":\"${DECISION.EXPERIMENTID}\",\"is_campaign_holdback\":${DECISION.HOLDBACK},\"variation_id\":\"${DECISION.VARIATIONID}\"}],\"events\":[{\"key\":\"${EVENT.KEY}\",\"timestamp\":${EVENT.TIMESTAMP},\"type\":\"${EVENT.TYPE}\",\"uuid\":\"${EVENT.UUID}\",\"entity_id\":\"${EVENT.ENTITYID}\"}]}],\"visitor_id\":\"${VISITOR.VISITORID}\"}],\"account_id\":\"${EVENTBATCH.ACCOUNTID}\",\"anonymize_ip\":${EVENTBATCH.ANONIP},\"client_name\":\"${EVENTBATCH.CLIENTNAME}\",\"client_version\":\"${EVENTBATCH.CLIENTVERSION}\",\"project_id\":\"${EVENTBATCH.PROJECTID}\"}";

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

        String payload = EVENT_TEMPLATE;
        payload = payload.replace("${DECISION.CAMPAIGNID}", activatedExperiment.getLayerId());
        payload = payload.replace("${DECISION.EXPERIMENTID}", activatedExperiment.getId());
        payload = payload.replace("${DECISION.VARIATIONID}", variation.getId());
        payload = payload.replace("${DECISION.HOLDBACK}", "false");

        payload = payload.replace("${EVENT.TIMESTAMP}", Long.toString(System.currentTimeMillis()));
        payload = payload.replace("${EVENT.UUID}", UUID.randomUUID().toString());
        payload = payload.replace("${EVENT.ENTITYID}", activatedExperiment.getLayerId());
        payload = payload.replace("${EVENT.KEY}", ACTIVATE_EVENT_KEY);
        payload = payload.replace("${EVENT.TYPE}", ACTIVATE_EVENT_KEY);

        payload = payload.replace("${VISITOR.VISITORID}", userId);
        payload = payload.replace("${VISITOR.ATTRIBUTES}", buildAttributeStrings(buildAttributeList(projectConfig, attributes)));

        payload = payload.replace("${EVENTBATCH.CLIENTNAME}", clientEngine.getClientEngineValue());
        payload = payload.replace("${EVENTBATCH.CLIENTVERSION}", clientVersion);
        payload = payload.replace("${EVENTBATCH.ANONIP}", projectConfig.getAnonymizeIP() ? "true" : "false");
        payload = payload.replace("${EVENTBATCH.PROJECTID}", projectConfig.getProjectId());
        payload = payload.replace("${BATCHEVENT.REVISION}",  projectConfig.getRevision());
        payload = payload.replace("${EVENTBATCH.ACCOUNTID}",  projectConfig.getAccountId());

        return new LogEvent(LogEvent.RequestMethod.POST, EVENT_ENDPOINT, Collections.<String, String>emptyMap(), payload);
    }

    public LogEvent createConversionEvent(@Nonnull ProjectConfig projectConfig,
                                                   @Nonnull Map<Experiment, Variation> experimentVariationMap,
                                                   @Nonnull String userId,
                                                   @Nonnull String eventId,
                                                   @Nonnull String eventName,
                                                   @Nonnull Map<String, String> attributes,
                                                   @Nonnull Map<String, ?> eventTags) {

        if (experimentVariationMap.isEmpty()) {
            return null;
        }

        ArrayList<Decision> decisions = new ArrayList<Decision>();
        for (Map.Entry<Experiment, Variation> entry : experimentVariationMap.entrySet()) {
            Decision decision = new Decision(entry.getKey().getLayerId(), entry.getKey().getId(), entry.getValue().getId(), false);
            decisions.add(decision);
        }

        EventType eventType = projectConfig.getEventNameMapping().get(eventName);

        Event conversionEvent = new Event(System.currentTimeMillis(),UUID.randomUUID().toString(), eventType.getId(),
                eventType.getKey(), null, EventTagUtils.getRevenueValue(eventTags), eventTags, eventType.getKey(), EventTagUtils.getNumericValue(eventTags));
        Snapshot snapshot = new Snapshot(decisions, Arrays.asList(conversionEvent));

        Visitor visitor = new Visitor(userId, null, buildAttributeList(projectConfig, attributes), Arrays.asList(snapshot));
        List<Visitor> visitors = Arrays.asList(visitor);
        EventBatch eventBatch = new EventBatch(clientEngine.getClientEngineValue(), clientVersion, projectConfig.getAccountId(), visitors, projectConfig.getAnonymizeIP(), projectConfig.getProjectId(), projectConfig.getRevision());
        String payload = this.serializer.serialize(eventBatch);
        return new LogEvent(LogEvent.RequestMethod.POST, EVENT_ENDPOINT, Collections.<String, String>emptyMap(), payload);
    }

    private List<Attribute> buildAttributeList(ProjectConfig projectConfig, Map<String, String> attributes) {
        List<Attribute> attributesList = new ArrayList<Attribute>();

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String attributeId = projectConfig.getAttributeId(projectConfig, entry.getKey());
            if(attributeId != null) {
                Attribute attribute = new Attribute(attributeId,
                        entry.getKey(),
                        Attribute.CUSTOM_ATTRIBUTE_TYPE,
                        entry.getValue());
                attributesList.add(attribute);
            }
        }

        //checks if botFiltering value is not set in the project config file.
        if(projectConfig.getBotFiltering() != null) {
            Attribute attribute = new Attribute(
                    ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                    ControlAttribute.BOT_FILTERING_ATTRIBUTE.toString(),
                    Attribute.CUSTOM_ATTRIBUTE_TYPE,
                    projectConfig.getBotFiltering()
            );
            attributesList.add(attribute);
        }

        return attributesList;
    }

    String buildAttributeStrings(List<Attribute> attributes) {
        StringBuilder stringBuilder = new StringBuilder("[");

        if (attributes == null || attributes.size() == 0) {
            stringBuilder.append("]");
            return stringBuilder.toString();
        }

        for (Attribute attribute : attributes) {
            stringBuilder.append("{");

            stringBuilder.append("\"entity_id\":");
            stringBuilder.append("\"");
            stringBuilder.append(attribute.getEntityId());
            stringBuilder.append("\"");
            stringBuilder.append(",");

            stringBuilder.append("\"key\":");
            stringBuilder.append("\"");
            stringBuilder.append(attribute.getKey().replaceAll("\"", "\\" + "\""));
            stringBuilder.append("\"");
            stringBuilder.append(",");

            stringBuilder.append("\"type\":");
            stringBuilder.append("\"");
            stringBuilder.append(attribute.getType());
            stringBuilder.append("\"");
            stringBuilder.append(",");

            stringBuilder.append("\"value\":");
            boolean isString = attribute.getValue() instanceof String;

            if (isString) {
                stringBuilder.append("\"");

                stringBuilder.append(((String)attribute.getValue()).replaceAll("\"", "\\" + "\""));

                stringBuilder.append("\"");
            }
            else {
                stringBuilder.append(attribute.getValue());
            }

            stringBuilder.append("}");

            if (!attribute.equals(attributes.get(attributes.size()-1))) {
                stringBuilder.append(",");
            }
        }

        stringBuilder.append("]");

        return stringBuilder.toString();
    }
}

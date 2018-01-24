package com.optimizely.ab.event.internal;

import com.optimizely.ab.bucketing.DecisionService;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.Attribute;
import com.optimizely.ab.event.internal.payload.DecisionV3;
import com.optimizely.ab.event.internal.payload.Event;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.event.internal.payload.EventV3;
import com.optimizely.ab.event.internal.payload.Snapshot;
import com.optimizely.ab.event.internal.payload.Visitor;
import com.optimizely.ab.event.internal.serializer.DefaultJsonSerializer;
import com.optimizely.ab.event.internal.serializer.Serializer;
import com.optimizely.ab.internal.EventTagUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EventBuilderV3 extends EventBuilder {
    private static final Logger logger = LoggerFactory.getLogger(EventBuilderV3.class);
    static final String EVENT_ENDPOINT = "https://logx.optimizely.com/v1/events";
    static final String  ACTIVATE_EVENT_KEY = "campaign_activated";
    private Serializer serializer;
    private String clientVersion;
    private Event.ClientEngine clientEngine;

    public EventBuilderV3() {
        this(Event.ClientEngine.JAVA_SDK, BuildVersionInfo.VERSION);
    }

    public EventBuilderV3(Event.ClientEngine clientEngine, String clientVersion) {
        this.clientEngine = clientEngine;
        this.clientVersion = clientVersion;
        this.serializer = DefaultJsonSerializer.getInstance();
    }


    public LogEvent createImpressionEvent(@Nonnull ProjectConfig projectConfig,
                                                   @Nonnull Experiment activatedExperiment,
                                                   @Nonnull Variation variation,
                                                   @Nonnull String userId,
                                                   @Nonnull Map<String, String> attributes) {

        DecisionV3 decisionV3 = new DecisionV3(activatedExperiment.getLayerId(), activatedExperiment.getId(),
                variation.getId(), false);
        EventV3 eventV3 = new EventV3(System.currentTimeMillis(),UUID.randomUUID().toString(), activatedExperiment.getLayerId(),
                ACTIVATE_EVENT_KEY, null, null, null, ACTIVATE_EVENT_KEY, null);
        Snapshot snapshot = new Snapshot(Arrays.asList(decisionV3), Arrays.asList(eventV3));

        Visitor visitor = new Visitor(userId, null, buildAttributeList(projectConfig, attributes), Arrays.asList(snapshot));
        List<Visitor> visitors = Arrays.asList(visitor);
        EventBatch eventBatch = new EventBatch(projectConfig.getAccountId(), visitors, projectConfig.getAnonymizeIP(), projectConfig.getProjectId(), projectConfig.getRevision());
        String payload = this.serializer.serialize(eventBatch);
        return new LogEvent(LogEvent.RequestMethod.POST, EVENT_ENDPOINT, Collections.<String, String>emptyMap(), payload);
    }

    public LogEvent createConversionEvent(@Nonnull ProjectConfig projectConfig,
                                                   @Nonnull Map<Experiment, Variation> experimentVariationMap,
                                                   @Nonnull String userId,
                                                   @Nonnull String eventId,
                                                   @Nonnull String eventName,
                                                   @Nonnull Map<String, String> attributes,
                                                   @Nonnull Map<String, ?> eventTags) {

        ArrayList<DecisionV3> decisionV3s = new ArrayList<DecisionV3>();
        ArrayList<EventV3> eventV3s = new ArrayList<EventV3>();
        for (Experiment experiment : experimentVariationMap.keySet()) {
            Variation variation = experimentVariationMap.get(experiment);
            DecisionV3 decisionV3 = new DecisionV3(experiment.getLayerId(), experiment.getId(), variation.getId(), false);
            decisionV3s.add(decisionV3);
        }

        EventType eventType = projectConfig.getEventNameMapping().get(eventName);

        EventV3 eventV3 = new EventV3(System.currentTimeMillis(),UUID.randomUUID().toString(), eventType.getId(),
                eventType.getKey(), null, EventTagUtils.getRevenueValue(eventTags), eventTags, eventType.getKey(), EventTagUtils.getNumericValue(eventTags));
        Snapshot snapshot = new Snapshot(decisionV3s, Arrays.asList(eventV3));

        Visitor visitor = new Visitor(userId, null, buildAttributeList(projectConfig, attributes), Arrays.asList(snapshot));
        List<Visitor> visitors = Arrays.asList(visitor);
        EventBatch eventBatch = new EventBatch(projectConfig.getAccountId(), visitors, projectConfig.getAnonymizeIP(), projectConfig.getProjectId(), projectConfig.getRevision());
        String payload = this.serializer.serialize(eventBatch);
        return new LogEvent(LogEvent.RequestMethod.POST, EVENT_ENDPOINT, Collections.<String, String>emptyMap(), payload);
    }

    private List<Attribute> buildAttributeList(ProjectConfig projectConfig, Map<String, String> attributes) {
        List<Attribute> attributes1 = new ArrayList<Attribute>();

        Map<String, com.optimizely.ab.config.Attribute> attributeMap = projectConfig.getAttributeKeyMapping();
        for (String key : attributes.keySet()) {
            com.optimizely.ab.config.Attribute projectAttribute = attributeMap.get(key);
            Attribute attribute = new Attribute((projectAttribute != null ? projectAttribute.getId() : null),
                    key, Attribute.CUSTOM_ATTRIBUTE_TYPE, attributes.get(key));

            if (key == DecisionService.BUCKETING_ATTRIBUTE) {
                attribute = new Attribute(com.optimizely.ab.bucketing.DecisionService.BUCKETING_ATTRIBUTE,
                        EventBuilderV2.ATTRIBUTE_KEY_FOR_BUCKETING_ATTRIBUTE, Attribute.CUSTOM_ATTRIBUTE_TYPE, attributes.get(key));
            }

            attributes1.add(attribute);
        }

        return attributes1;
    }
}

/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.*;
import com.optimizely.ab.event.internal.payload.Event.ClientEngine;
import com.optimizely.ab.event.internal.payload.batch.*;
import com.optimizely.ab.event.internal.payload.batch.Decision;
import com.optimizely.ab.event.internal.payload.batch.Event;
import com.optimizely.ab.event.internal.serializer.DefaultJsonSerializer;
import com.optimizely.ab.event.internal.serializer.Serializer;
import com.optimizely.ab.internal.EventTagUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

import static com.optimizely.ab.event.internal.EventBuilderV2.createUserFeatures;

public class EventBuilderV3 extends EventBuilder {

    private static final Logger logger = LoggerFactory.getLogger(EventBuilderV3.class);

    static final String BATCH_ENDPOINT = "https://logx.optimizely.com/v1/events";
    static final String KEY_CAMPAIGN_ACTIVATED = "campaign_activated";

    @VisibleForTesting
    public final ClientEngine clientEngine;

    @VisibleForTesting
    public final String clientVersion;

    private Serializer serializer;

    public EventBuilderV3() {
        this(ClientEngine.JAVA_SDK, BuildVersionInfo.VERSION);
    }

    public EventBuilderV3(ClientEngine clientEngine, String clientVersion) {
        this.clientEngine = clientEngine;
        this.clientVersion = clientVersion;
        this.serializer = DefaultJsonSerializer.getInstance();
    }

    public LogEvent createImpressionEvent(@Nonnull ProjectConfig projectConfig,
                                          @Nonnull Experiment activatedExperiment,
                                          @Nonnull Variation variation,
                                          @Nonnull String userId,
                                          @Nonnull Map<String, String> attributes) {
        Decision decision = new Decision(
                        activatedExperiment.getLayerId());
        decision.setExperimentId(activatedExperiment.getId());
        decision.setVariationId(variation.getId());
        List<Decision> decisions = Collections.singletonList(decision);

        Event event = new Event(
                        KEY_CAMPAIGN_ACTIVATED,
                        System.currentTimeMillis(),
                        UUID.randomUUID().toString());
        event.setEntityId(activatedExperiment.getLayerId());
        List<Event> events = Collections.singletonList(event);

        Snapshot snapshot = new Snapshot(decisions, events);
        List<Snapshot> snapshots = Collections.singletonList(snapshot);

        Visitor visitor = new Visitor(userId);
        List<Attribute> batchAttributes
                = createUserAttributes(projectConfig, attributes);
        visitor.setAttributes(batchAttributes);
        visitor.setSnapshots(snapshots);
        List<Visitor> visitors = Collections.singletonList(visitor);

        Batch impressionPayload = new Batch(projectConfig.getAccountId());
        impressionPayload.setAnonymizeIp(projectConfig.getAnonymizeIP());
        impressionPayload.setClientName(clientEngine.getClientEngineValue());
        impressionPayload.setClientVersion(clientVersion);
        impressionPayload.setVisitors(visitors);
        impressionPayload.setProjectId(projectConfig.getProjectId());

        String payload = this.serializer.serialize(impressionPayload);
        return new LogEvent(LogEvent.RequestMethod.POST, BATCH_ENDPOINT, Collections.<String, String>emptyMap(), payload);
    }


    // TODO remove conversion for performance gain
    private List<Attribute> createUserAttributes(@Nonnull ProjectConfig projectConfig, @Nonnull Map<String, String> attributes) {
        List<Feature> features = createUserFeatures(attributes, projectConfig);
        List<Attribute> batchAttributes = new ArrayList<Attribute>();
        for (Feature feature : features) {
            batchAttributes.add(new Attribute(feature));
        }
        return batchAttributes;
    }

    // TODO remove conversion for performance gain
    static List<Decision> createDecisions(ProjectConfig projectConfig, Bucketer bucketer, String visitorId,
                                               String eventKey, Map<String, String> attributes) {
        List<LayerState> layerStates = EventBuilderV2.createLayerStates(projectConfig, bucketer, visitorId, eventKey,
                attributes);
        List<Decision> decisions = new ArrayList<Decision>();
        for (LayerState layerState : layerStates) {
            com.optimizely.ab.event.internal.payload.Decision decision = layerState.getDecision();
            Decision batchDecision = new Decision(layerState.getLayerId());
            batchDecision.setVariationId(decision.getVariationId());
            batchDecision.setExperimentId(decision.getExperimentId());
            decisions.add(batchDecision);
        }

        return decisions;
    }

    public LogEvent createConversionEvent(@Nonnull ProjectConfig projectConfig,
                                          @Nonnull Bucketer bucketer,
                                          @Nonnull String userId,
                                          @Nonnull String eventId,
                                          @Nonnull String eventName,
                                          @Nonnull Map<String, String> attributes,
                                          @Nonnull Map<String, ?> eventTags) {
        Long revenue = EventTagUtils.getRevenueValue(eventTags);

        eventTags.remove(EventMetric.REVENUE_METRIC_TYPE);
        Event event = new Event(eventName, System.currentTimeMillis(), UUID.randomUUID().toString());
        event.setTags(eventTags);
        event.setEntityId(eventId);
        event.setRevenue(revenue);

        List<Event> events = Collections.singletonList(event);

        List<Decision> decisions = createDecisions(projectConfig, bucketer, userId, eventName, attributes);
        Snapshot snapshot = new Snapshot(decisions, events);
        List<Snapshot> snapshots = Collections.singletonList(snapshot);

        Visitor visitor = new Visitor(userId);
        List<com.optimizely.ab.event.internal.payload.batch.Attribute> batchAttributes
                = createUserAttributes(projectConfig, attributes);
        visitor.setAttributes(batchAttributes);
        visitor.setSnapshots(snapshots);
        List<Visitor> visitors = Collections.singletonList(visitor);

        Batch conversionPayload = new Batch(projectConfig.getAccountId());
        conversionPayload.setProjectId(projectConfig.getProjectId());
        conversionPayload.setClientName(clientEngine.getClientEngineValue());
        conversionPayload.setClientVersion(clientVersion);
        conversionPayload.setAnonymizeIp(projectConfig.getAnonymizeIP());
        conversionPayload.setVisitors(visitors);

        String payload = this.serializer.serialize(conversionPayload);
        return new LogEvent(LogEvent.RequestMethod.POST, BATCH_ENDPOINT, Collections.<String, String>emptyMap(), payload);
    }
}

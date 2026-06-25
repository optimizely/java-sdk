/**
 *
 *    Copyright 2025, Optimizely and contributors
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
import com.optimizely.ab.event.internal.payload.Decision;
import com.optimizely.ab.event.internal.payload.DecisionMetadata;
import com.optimizely.ab.event.internal.payload.Event;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.event.internal.payload.Snapshot;
import com.optimizely.ab.event.internal.payload.Visitor;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for FSSDK-12813 decision-event identifier normalization,
 * exercising the full path from {@link ImpressionEvent} through
 * {@link EventFactory#createLogEvent} to the wire payload.
 *
 * <p>Verifies:
 * <ul>
 *   <li>FR-001/FR-002: {@code campaign_id} substitution from {@code experiment_id}
 *       when campaign_id is empty / null / non-numeric / whitespace.</li>
 *   <li>FR-003/FR-004: {@code variation_id} replacement with {@code null} when
 *       empty / non-numeric / whitespace.</li>
 *   <li>FR-005: Uniform behavior across all rule types (experiment / feature-test /
 *       rollout / holdout).</li>
 *   <li>FR-006/FR-007: Event is never dropped and no exceptions are raised.</li>
 *   <li>FR-009: Impression {@code entity_id} equals the normalized {@code campaign_id}
 *       byte-for-byte.</li>
 *   <li>FR-010: Conversion {@code entity_id} is left unchanged.</li>
 * </ul>
 */
public class EventFactoryNormalizationTest {

    private static final String USER_ID = "test-user";

    private ProjectConfig projectConfig;

    @Before
    public void setUp() {
        projectConfig = mock(ProjectConfig.class);
        when(projectConfig.getAccountId()).thenReturn("1");
        when(projectConfig.getProjectId()).thenReturn("100");
        when(projectConfig.getRevision()).thenReturn("3");
        when(projectConfig.getAnonymizeIP()).thenReturn(true);
        when(projectConfig.getBotFiltering()).thenReturn(null);
        when(projectConfig.getRegion()).thenReturn("US");
    }

    private UserContext userContext() {
        return new UserContext.Builder()
            .withUserId(USER_ID)
            .withAttributes(Collections.emptyMap())
            .withProjectConfig(projectConfig)
            .build();
    }

    private ImpressionEvent buildImpression(String ruleType,
                                            String layerId,
                                            String experimentId,
                                            String variationId) {
        DecisionMetadata metadata = new DecisionMetadata.Builder()
            .setFlagKey("test_flag")
            .setRuleKey("test_rule")
            .setRuleType(ruleType)
            .setVariationKey("variationKey")
            .setEnabled(true)
            .build();

        return new ImpressionEvent.Builder()
            .withUserContext(userContext())
            .withLayerId(layerId)
            .withExperimentId(experimentId)
            .withExperimentKey("experimentKey")
            .withVariationId(variationId)
            .withVariationKey("variationKey")
            .withMetadata(metadata)
            .build();
    }

    private static Decision firstDecision(LogEvent logEvent) {
        EventBatch batch = logEvent.getEventBatch();
        Visitor visitor = batch.getVisitors().get(0);
        Snapshot snapshot = visitor.getSnapshots().get(0);
        return snapshot.getDecisions().get(0);
    }

    private static Event firstEvent(LogEvent logEvent) {
        EventBatch batch = logEvent.getEventBatch();
        Visitor visitor = batch.getVisitors().get(0);
        Snapshot snapshot = visitor.getSnapshots().get(0);
        return snapshot.getEvents().get(0);
    }

    // ---------------------------------------------------------------------
    // Happy path
    // ---------------------------------------------------------------------

    /**
     * FR-001/FR-009 happy path: a valid numeric campaign_id is passed through,
     * and entity_id matches it byte-for-byte.
     */
    @Test
    public void happyPath_numericIds_passThroughUnchanged() {
        ImpressionEvent imp = buildImpression(
            "experiment", "1111", "2222", "3333");

        LogEvent log = EventFactory.createLogEvent(imp);

        assertNotNull(log);
        Decision d = firstDecision(log);
        Event e = firstEvent(log);

        assertEquals("1111", d.getCampaignId());
        assertEquals("2222", d.getExperimentId());
        assertEquals("3333", d.getVariationId());
        assertEquals("1111", e.getEntityId());
        // FR-009 byte-for-byte: same reference content
        assertEquals(d.getCampaignId(), e.getEntityId());
    }

    // ---------------------------------------------------------------------
    // FR-001/FR-002: campaign_id fallback
    // ---------------------------------------------------------------------

    @Test
    public void campaignId_null_fallsBackToExperimentId() {
        ImpressionEvent imp = buildImpression(
            "experiment", null, "2222", "3333");
        LogEvent log = EventFactory.createLogEvent(imp);
        assertEquals("2222", firstDecision(log).getCampaignId());
        assertEquals("2222", firstEvent(log).getEntityId());
    }

    @Test
    public void campaignId_empty_fallsBackToExperimentId() {
        ImpressionEvent imp = buildImpression(
            "experiment", "", "2222", "3333");
        LogEvent log = EventFactory.createLogEvent(imp);
        assertEquals("2222", firstDecision(log).getCampaignId());
        assertEquals("2222", firstEvent(log).getEntityId());
    }

    @Test
    public void campaignId_whitespace_fallsBackToExperimentId() {
        ImpressionEvent imp = buildImpression(
            "experiment", " ", "2222", "3333");
        LogEvent log = EventFactory.createLogEvent(imp);
        assertEquals("2222", firstDecision(log).getCampaignId());
        assertEquals("2222", firstEvent(log).getEntityId());
    }

    @Test
    public void campaignId_nonNumericString_fallsBackToExperimentId() {
        ImpressionEvent imp = buildImpression(
            "experiment", "layerKey", "2222", "3333");
        LogEvent log = EventFactory.createLogEvent(imp);
        assertEquals("2222", firstDecision(log).getCampaignId());
        assertEquals("2222", firstEvent(log).getEntityId());
    }

    // ---------------------------------------------------------------------
    // FR-003/FR-004: variation_id null replacement
    // ---------------------------------------------------------------------

    @Test
    public void variationId_null_remainsNull() {
        ImpressionEvent imp = buildImpression(
            "experiment", "1111", "2222", null);
        LogEvent log = EventFactory.createLogEvent(imp);
        assertNull(firstDecision(log).getVariationId());
    }

    @Test
    public void variationId_empty_becomesNull() {
        ImpressionEvent imp = buildImpression(
            "experiment", "1111", "2222", "");
        LogEvent log = EventFactory.createLogEvent(imp);
        assertNull(firstDecision(log).getVariationId());
    }

    @Test
    public void variationId_whitespace_becomesNull() {
        ImpressionEvent imp = buildImpression(
            "experiment", "1111", "2222", "   ");
        LogEvent log = EventFactory.createLogEvent(imp);
        assertNull(firstDecision(log).getVariationId());
    }

    @Test
    public void variationId_nonNumericString_becomesNull() {
        ImpressionEvent imp = buildImpression(
            "experiment", "1111", "2222", "variationKey");
        LogEvent log = EventFactory.createLogEvent(imp);
        assertNull(firstDecision(log).getVariationId());
    }

    // ---------------------------------------------------------------------
    // FR-005: Uniform across decision types
    // ---------------------------------------------------------------------

    @Test
    public void normalization_uniformAcrossAllRuleTypes() {
        String[] ruleTypes = {"experiment", "feature-test", "rollout", "holdout"};
        for (String ruleType : ruleTypes) {
            ImpressionEvent imp = buildImpression(
                ruleType, "bad-layer", "2222", "bad-variation");
            LogEvent log = EventFactory.createLogEvent(imp);

            Decision d = firstDecision(log);
            Event e = firstEvent(log);

            // For every rule type, the same normalization rules apply.
            assertEquals(
                "campaign_id should fall back to experiment_id for ruleType=" + ruleType,
                "2222", d.getCampaignId());
            assertEquals(
                "entity_id must mirror campaign_id for ruleType=" + ruleType,
                "2222", e.getEntityId());
            assertNull(
                "variation_id should become null for ruleType=" + ruleType,
                d.getVariationId());
        }
    }

    // ---------------------------------------------------------------------
    // FR-009: byte-for-byte equality of entity_id and campaign_id
    // ---------------------------------------------------------------------

    @Test
    public void entityId_alwaysMirrorsNormalizedCampaignId() {
        // 1) Both valid → equal.
        ImpressionEvent imp1 = buildImpression("experiment", "1111", "2222", "3333");
        LogEvent log1 = EventFactory.createLogEvent(imp1);
        assertEquals(firstDecision(log1).getCampaignId(), firstEvent(log1).getEntityId());

        // 2) campaign_id invalid → both equal to experiment_id.
        ImpressionEvent imp2 = buildImpression("experiment", "bad", "2222", "3333");
        LogEvent log2 = EventFactory.createLogEvent(imp2);
        assertEquals(firstDecision(log2).getCampaignId(), firstEvent(log2).getEntityId());
        assertEquals("2222", firstEvent(log2).getEntityId());

        // 3) campaign_id null → both equal to experiment_id.
        ImpressionEvent imp3 = buildImpression("experiment", null, "2222", "3333");
        LogEvent log3 = EventFactory.createLogEvent(imp3);
        assertEquals(firstDecision(log3).getCampaignId(), firstEvent(log3).getEntityId());
    }

    // ---------------------------------------------------------------------
    // FR-006/FR-007: Event must never be dropped, no exceptions
    // ---------------------------------------------------------------------

    @Test
    public void event_isNeverDropped_evenWhenAllIdsAreInvalid() {
        ImpressionEvent imp = buildImpression(
            "experiment", "bad", "2222", "bad");
        LogEvent log = EventFactory.createLogEvent(imp);
        assertNotNull("Event must not be dropped due to id normalization", log);
        assertNotNull(firstDecision(log));
        assertNotNull(firstEvent(log));
    }

    // ---------------------------------------------------------------------
    // FR-010: Conversion entity_id is unchanged
    // ---------------------------------------------------------------------

    @Test
    public void conversionEvent_entityId_isUnchanged() {
        // Conversion events derive entity_id from a different source (event_id) and
        // must NOT be normalized. We use a non-numeric event_id ("event_abc") and
        // verify it passes through verbatim.
        ConversionEvent conversion = new ConversionEvent.Builder()
            .withUserContext(userContext())
            .withEventId("event_abc")
            .withEventKey("checkout")
            .withRevenue(null)
            .withValue(null)
            .withTags(Collections.emptyMap())
            .build();

        LogEvent log = EventFactory.createLogEvent(conversion);
        assertNotNull(log);

        Event e = firstEvent(log);
        assertEquals("event_abc", e.getEntityId());
        assertEquals("checkout", e.getKey());
    }

    // ---------------------------------------------------------------------
    // FR-008: Wire output is identical for identical inputs (determinism).
    // ---------------------------------------------------------------------

    @Test
    public void wireOutput_isDeterministic_forSameInput() {
        ImpressionEvent imp1 = buildImpression("experiment", "1111", "2222", "3333");
        ImpressionEvent imp2 = buildImpression("experiment", "1111", "2222", "3333");

        LogEvent log1 = EventFactory.createLogEvent(imp1);
        LogEvent log2 = EventFactory.createLogEvent(imp2);

        Decision d1 = firstDecision(log1);
        Decision d2 = firstDecision(log2);
        assertEquals(d1.getCampaignId(), d2.getCampaignId());
        assertEquals(d1.getExperimentId(), d2.getExperimentId());
        assertEquals(d1.getVariationId(), d2.getVariationId());
        assertEquals(firstEvent(log1).getEntityId(), firstEvent(log2).getEntityId());
    }

    // ---------------------------------------------------------------------
    // Reference safety: when campaign_id is valid, the same string is used
    // for both decision.campaign_id and event.entity_id (no defensive copy).
    // ---------------------------------------------------------------------

    @Test
    public void validCampaignId_usedForBothDecisionAndEntity() {
        String layerId = "9876543210";
        ImpressionEvent imp = buildImpression("experiment", layerId, "2222", "3333");
        LogEvent log = EventFactory.createLogEvent(imp);

        assertSame(
            "Same numeric campaign_id must be reused for both fields",
            layerId, firstDecision(log).getCampaignId());
        assertSame(
            "entity_id must reuse the same normalized campaign_id reference",
            layerId, firstEvent(log).getEntityId());
    }
}

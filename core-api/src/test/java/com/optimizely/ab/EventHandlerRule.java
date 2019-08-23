/**
 *    Copyright 2019, Optimizely Inc. and contributors
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
package com.optimizely.ab;

import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.*;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.optimizely.ab.config.ProjectConfig.RESERVED_ATTRIBUTE_PREFIX;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * EventHandlerRule is a JUnit rule that implements an Optimizely {@link EventHandler}.
 *
 * This implementation captures events being dispatched in a List.
 *
 * The List of "actual" events are compared, in order, against a list of "expected" events.
 *
 * Expected events are validated at the end of the test to allow asynchronous event dispatching.
 *
 * A failure is raised if at the end of the test there remain non-validated actual events. This is by design
 * to ensure that all outbound traffic is known and validated.
 */
public class EventHandlerRule implements EventHandler, TestRule {

    private static final Logger logger = LoggerFactory.getLogger(EventHandlerRule.class);
    private static final String IMPRESSION_EVENT_NAME = "campaign_activated";

    private List<CanonicalEvent> expectedEvents;
    private LinkedList<CanonicalEvent> actualEvents;
    private int actualCalls;
    private Integer expectedCalls;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                    verify();
                } finally {
                    after();
                }
            }
        };
    }

    private void before() {
        expectedEvents = new LinkedList<>();
        actualEvents = new LinkedList<>();

        expectedCalls = null;
        actualCalls = 0;
    }

    private void after() {
    }

    private void verify() {
        if (expectedCalls != null) {
            assertEquals(expectedCalls.intValue(), actualCalls);
        }

        assertEquals(expectedEvents.size(), actualEvents.size());

        ListIterator<CanonicalEvent> expectedIterator = expectedEvents.listIterator();
        ListIterator<CanonicalEvent> actualIterator = actualEvents.listIterator();

        while (expectedIterator.hasNext()) {
            CanonicalEvent expected = expectedIterator.next();
            CanonicalEvent actual = actualIterator.next();

            assertEquals(expected, actual);
        }
    }

    public void expectCalls(int expected) {
        expectedCalls = expected;
    }

    public void expectImpression(String experientId, String variationId, String userId) {
        expectImpression(experientId, variationId, userId, Collections.emptyMap());
    }

    public void expectImpression(String experientId, String variationId, String userId, Map<String, ?> attributes) {
        expect(experientId, variationId, IMPRESSION_EVENT_NAME, userId, attributes, null);
    }

    public void expectConversion(String eventName, String userId) {
        expectConversion(eventName, userId, Collections.emptyMap());
    }

    public void expectConversion(String eventName, String userId, Map<String, ?> attributes) {
        expectConversion(eventName, userId, attributes, Collections.emptyMap());
    }

    public void expectConversion(String eventName, String userId, Map<String, ?> attributes, Map<String, ?> tags) {
        expect(null, null, eventName, userId, attributes, tags);
    }

    public void expect(String experientId, String variationId, String eventName, String userId,
                  Map<String, ?> attributes, Map<String, ?> tags) {
        CanonicalEvent expectedEvent = new CanonicalEvent(experientId, variationId, eventName, userId, attributes, tags);
        expectedEvents.add(expectedEvent);
    }

    @Override
    public void dispatchEvent(LogEvent logEvent) {
        logger.info("Receiving event: {}", logEvent);
        actualCalls++;

        List<Visitor> visitors = logEvent.getEventBatch().getVisitors();

        if (visitors == null) {
            return;
        }

        for (Visitor visitor: visitors) {
            for (Snapshot snapshot: visitor.getSnapshots()) {
                List<Decision> decisions = snapshot.getDecisions();
                if (decisions == null) {
                    decisions = new ArrayList<>();
                }

                if (decisions.isEmpty()) {
                    decisions.add(new Decision());
                }

                for (Decision decision: decisions) {
                    for (Event event: snapshot.getEvents()) {
                        CanonicalEvent actual = new CanonicalEvent(
                            decision.getExperimentId(),
                            decision.getVariationId(),
                            event.getKey(),
                            visitor.getVisitorId(),
                            visitor.getAttributes().stream()
                                .filter(attribute -> !attribute.getKey().startsWith(RESERVED_ATTRIBUTE_PREFIX))
                                .collect(Collectors.toMap(Attribute::getKey, Attribute::getValue)),
                            event.getTags()
                        );

                        logger.info("Adding dispatched, event: {}", actual);
                        actualEvents.add(actual);
                    }
                }
            }
        }
    }

    private static class CanonicalEvent {
        private String experimentId;
        private String variationId;
        private String eventName;
        private String visitorId;
        private Map<String, ?> attributes;
        private Map<String, ?> tags;

        public CanonicalEvent(String experimentId, String variationId, String eventName,
                              String visitorId, Map<String, ?> attributes, Map<String, ?> tags) {
            this.experimentId = experimentId;
            this.variationId = variationId;
            this.eventName = eventName;
            this.visitorId = visitorId;
            this.attributes = attributes;
            this.tags = tags;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CanonicalEvent that = (CanonicalEvent) o;
            return Objects.equals(experimentId, that.experimentId) &&
                Objects.equals(variationId, that.variationId) &&
                Objects.equals(eventName, that.eventName) &&
                Objects.equals(visitorId, that.visitorId) &&
                Objects.equals(attributes, that.attributes) &&
                Objects.equals(tags, that.tags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(experimentId, variationId, eventName, visitorId, attributes, tags);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", CanonicalEvent.class.getSimpleName() + "[", "]")
                .add("experimentId='" + experimentId + "'")
                .add("variationId='" + variationId + "'")
                .add("eventName='" + eventName + "'")
                .add("visitorId='" + visitorId + "'")
                .add("attributes=" + attributes)
                .add("tags=" + tags)
                .toString();
        }
    }
}

/**
 *
 *    Copyright 2019, Optimizely and contributors
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

import java.util.Map;

/**
 * ConversionEvent encapsulates information specific to conversion events.
 */
public class ConversionEvent implements UserEvent {

    private final UserContext userContext;
    private final String eventId;
    private final String eventKey;
    private final Number revenue;
    private final Number value;
    private final Map<String, ?> tags;


    private ConversionEvent(UserContext userContext, String eventId, String eventKey, Number revenue, Number value, Map<String, ?> tags) {
        this.userContext = userContext;
        this.eventId = eventId;
        this.eventKey = eventKey;
        this.revenue = revenue;
        this.value = value;
        this.tags = tags;
    }

    public UserContext getUserContext() {
        return userContext;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventKey() {
        return eventKey;
    }

    public Number getRevenue() {
        return revenue;
    }

    public Number getValue() {
        return value;
    }

    public Map<String, ?> getTags() {
        return tags;
    }

    public static class Builder {

        private UserContext userContext;
        private String eventId;
        private String eventKey;
        private Number revenue;
        private Number value;
        private Map<String, ?> tags;

        public Builder withUserContext(UserContext userContext) {
            this.userContext = userContext;
            return this;
        }

        public Builder withEventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder withEventKey(String eventKey) {
            this.eventKey = eventKey;
            return this;
        }

        public Builder withRevenue(Number revenue) {
            this.revenue = revenue;
            return this;
        }

        public Builder withValue(Number value) {
            this.value = value;
            return this;
        }

        public Builder withTags(Map<String, ?> tags) {
            this.tags = tags;
            return this;
        }

        public ConversionEvent build() {
            return new ConversionEvent(userContext, eventId, eventKey, revenue, value, tags);
        }
    }
}

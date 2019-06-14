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

import com.optimizely.ab.config.ProjectConfig;

import java.util.Map;

/**
 * UserEvent stores the user and project context along with a {@link ConversionEvent} and {@link ImpressionEvent}
 */
public class UserEvent {
    private final ProjectConfig projectConfig;

    private final String UUID;
    private final long timestamp;

    private final String userId;
    private final Map<String, ?> attributes;

    private final ConversionEvent conversionEvent;
    private final ImpressionEvent impressionEvent;

    private UserEvent(ProjectConfig projectConfig, String UUID, long timestamp, String userId, Map<String, ?> attributes, ConversionEvent conversionEvent, ImpressionEvent impressionEvent) {
        this.projectConfig = projectConfig;
        this.UUID = UUID;
        this.timestamp = timestamp;
        this.userId = userId;
        this.attributes = attributes;
        this.conversionEvent = conversionEvent;
        this.impressionEvent = impressionEvent;
    }

    public ProjectConfig getProjectConfig() {
        return projectConfig;
    }

    public String getUUID() {
        return UUID;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public ConversionEvent getConversionEvent() {
        return conversionEvent;
    }

    public ImpressionEvent getImpressionEvent() {
        return impressionEvent;
    }

    public static class Builder {

        private ProjectConfig projectConfig;
        private String uuid;
        private long timestamp;
        private String userId;
        private Map<String, ?> attributes;
        private ConversionEvent conversionEvent;
        private ImpressionEvent impressionEvent;

        public Builder withProjectConfig(ProjectConfig projectConfig) {
            this.projectConfig = projectConfig;
            return this;
        }

        public Builder withUUID(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder withTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder withAttributes(Map<String, ?> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder withConversionEvent(ConversionEvent conversionEvent) {
            this.conversionEvent = conversionEvent;
            return this;
        }

        public Builder withImpressionEvent(ImpressionEvent impressionEvent) {
            this.impressionEvent = impressionEvent;
            return this;
        }

        public UserEvent build() {
            return new UserEvent(projectConfig, uuid, timestamp, userId, attributes, conversionEvent, impressionEvent);
        }
    }
}

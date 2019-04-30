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
package com.optimizely.ab.notification;

import com.optimizely.ab.event.LogEvent;

import java.util.Collections;
import java.util.Map;

/**
 * TrackNotification encapsulates the arguments used to submit tracking calls.
 */
public class TrackNotification implements Notification {

    // Event API key being tracked.
    private final String eventKey;
    // UserId that triggered the event.
    private final String userId;
    // User attributes associated with the user. Included in the event to enable results segmentation.
    private final Map<String, ?> attributes;
    // Additional metadata associated with the event.
    private final Map<String, ?> eventTags;
    // LogEvent to be sent to Optimizely log endpoint.
    private final LogEvent event;

    public TrackNotification(String eventKey, String userId, Map<String, ?> attributes, Map<String, ?> eventTags, LogEvent event) {
        this.eventKey = eventKey;
        this.userId = userId;
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
        this.eventTags = eventTags == null ? Collections.emptyMap() : eventTags;
        this.event = event;
    }

    public String getEventKey() {
        return eventKey;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public Map<String, ?> getEventTags() {
        return eventTags;
    }

    public LogEvent getEvent() {
        return event;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TrackNotification{");
        sb.append("eventKey='").append(eventKey).append('\'');
        sb.append(", userId='").append(userId).append('\'');
        sb.append(", attributes=").append(attributes);
        sb.append(", eventTags=").append(eventTags);
        sb.append(", event=").append(event);
        sb.append('}');
        return sb.toString();
    }
}

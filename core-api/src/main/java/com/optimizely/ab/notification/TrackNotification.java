/**
 *
 *    Copyright 2019,2021, Optimizely and contributors
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

import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.event.LogEvent;

import java.util.Map;

/**
 * TrackNotification encapsulates the arguments used to submit tracking calls.
 */
public final class TrackNotification {

    private final String eventKey;
    private final String userId;
    private final Map<String, ?> attributes;
    private final Map<String, ?> eventTags;
    private final LogEvent event;

    @VisibleForTesting
    TrackNotification() {
        this(null, null, null, null, null);
    }

    /**
     * @param eventKey   - The event key that was triggered.
     * @param userId     - user id passed into track.
     * @param attributes - filtered attributes list after passed into track
     * @param eventTags  - event tags if any were passed in.
     * @param event      - The event being recorded.
     */
    public TrackNotification(String eventKey, String userId, Map<String, ?> attributes, Map<String, ?> eventTags, LogEvent event) {
        this.eventKey = eventKey;
        this.userId = userId;
        this.attributes = attributes;
        this.eventTags = eventTags;
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

    /**
     * This interface is deprecated since this is no longer a one-to-one mapping.
     * Please use a {@link NotificationHandler} explicitly for LogEvent messages.
     * {@link com.optimizely.ab.Optimizely#addLogEventNotificationHandler(NotificationHandler)}
     *
     * @return The event
     */
    @Deprecated
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

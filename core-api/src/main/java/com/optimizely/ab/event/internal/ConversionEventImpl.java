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
package com.optimizely.ab.event.internal;

import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.api.ConversionEvent;
import com.optimizely.ab.api.EventContext;
import com.optimizely.ab.event.internal.payload.Attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConversionEventImpl extends AbstractEvent implements ConversionEvent {
    private com.optimizely.ab.config.EventType event;
    private String userId;
    private List<Attribute> userAttributes;
    private Map<String, ?> tags;

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ConversionEvent copy) {
        Builder builder = new Builder();
        builder.event = copy.getEvent();
        builder.userId = copy.getUserId();
        builder.userAttributes = copy.getUserAttributes();
        builder.tags = copy.getTags();
        return builder;
    }

    ConversionEventImpl(
        EventContext context,
        Date timestamp,
        String uuid,
        com.optimizely.ab.config.EventType event,
        String userId,
        List<Attribute> userAttributes,
        Map<String, ?> tags
    ) {
        super(context, uuidOrRandom(uuid), timestampOrNow(timestamp));
        // TODO Assert.notNull
        this.event = Assert.notNull(event, "event");
        this.userId = Assert.notNull(userId, "userId");
        this.userAttributes = (userAttributes != null) ? userAttributes : Collections.emptyList();
        this.tags = (tags != null) ? tags : Collections.emptyMap();
    }

    @Override
    public EventType getType() {
        return EventType.CONVERSION;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public com.optimizely.ab.config.EventType getEvent() {
        return event;
    }

    @Override
    public List<Attribute> getUserAttributes() {
        return userAttributes;
    }

    @Override
    public Map<String, ?> getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversionEventImpl)) return false;
        if (!super.equals(o)) return false;
        final ConversionEventImpl that = (ConversionEventImpl) o;
        return Objects.equals(event, that.event) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(userAttributes, that.userAttributes) &&
            Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), event, userId, userAttributes, tags);
    }

    public static final class Builder {
        private EventContext context;
        private String uuid;
        private Date timestamp;
        private com.optimizely.ab.config.EventType event;
        private String userId;
        private List<Attribute> userAttributes;
        private Map<String, ?> tags;

        private Builder() {
        }

        private Builder(ConversionEvent inst) {
            this.context = inst.getContext();
            this.uuid = inst.getUuid();
            this.timestamp = inst.getTimestamp();
            this.event = inst.getEvent();
            this.userId = inst.getUserId();
            this.userAttributes = inst.getUserAttributes() != null ? new ArrayList<>(inst.getUserAttributes()) : null;
            this.tags = inst.getTags() != null ? new HashMap<>(inst.getTags()) : null;
        }

        public Builder context(EventContext context) {
            this.context = context;
            return this;
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder timestamp(Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder event(com.optimizely.ab.config.EventType event) {
            this.event = event;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder userAttributes(List<Attribute> userAttributes) {
            this.userAttributes = userAttributes;
            return this;
        }

        public Builder tags(Map<String, ?> tags) {
            this.tags = tags;
            return this;
        }

        public ConversionEventImpl build() {
            return new ConversionEventImpl(
                context,
                timestamp,
                uuid,
                event,
                userId,
                userAttributes,
                tags
            );
        }
    }
}

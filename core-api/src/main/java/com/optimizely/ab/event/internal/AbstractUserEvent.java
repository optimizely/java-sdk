/**
 * Copyright 2019, Optimizely Inc. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.optimizely.ab.event.internal;

import com.optimizely.ab.api.Event;
import com.optimizely.ab.api.EventContext;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.event.internal.payload.Attribute;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractUserEvent implements Event {
    protected final EventContext context;
    protected final String uuid;
    protected final Date timestamp;
    private String userId;
    private List<Attribute> userAttributes;

    protected static Date timestampOrNow(@Nullable Date timestamp) {
        return (timestamp != null) ? timestamp : new Date();
    }

    protected static String uuidOrRandom(@Nullable String uuid) {
        return (uuid != null) ? uuid : nextUuid();
    }

    protected static String nextUuid() {
        return UUID.randomUUID().toString();
    }

    AbstractUserEvent(
        EventContext context,
        String uuid,
        Date timestamp,
        String userId,
        List<Attribute> userAttributes
    ) {
        this.context = Assert.notNull(context, "context");
        this.uuid = uuidOrRandom(uuid);
        this.timestamp = timestampOrNow(timestamp);
        this.userId = userId;
        this.userAttributes = userAttributes != null ? userAttributes : Collections.emptyList();
    }

    @Override
    public EventContext getContext() {
        return context;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public List<Attribute> getUserAttributes() {
        return userAttributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractUserEvent)) return false;
        final AbstractUserEvent that = (AbstractUserEvent) o;
        return Objects.equals(context, that.context) &&
            Objects.equals(uuid, that.uuid) &&
            Objects.equals(timestamp, that.timestamp) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(userAttributes, that.userAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, uuid, timestamp, userId, userAttributes);
    }

    public abstract static class AbstractBuilder<S extends AbstractBuilder<S>> {
        protected final Type type;
        protected EventContext context;
        protected String uuid;
        protected Date timestamp;
        protected String userId;
        protected List<Attribute> userAttributes;

        protected AbstractBuilder(Type type) {
            this.type = Assert.notNull(type, "type");
        }

        protected AbstractBuilder(Event inst) {
            this.type = inst.getType();
            this.context = inst.getContext();
            this.uuid = inst.getUuid();
            this.timestamp = inst.getTimestamp();
            this.userId = inst.getUserId();
            this.userAttributes = Optional.ofNullable(inst.getUserAttributes())
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
        }

        abstract public Event build(EventContext context);

        public S context(EventContext context) {
            this.context = Assert.notNull(context, "context");
            return self();
        }

        public S uuid(String uuid) {
            this.uuid = Assert.notNull(uuid, "uuid");
            return self();
        }

        public S timestamp(Date timestamp) {
            this.timestamp = Assert.notNull(timestamp, "timestamp");
            return self();
        }

        public S userId(String userId) {
            this.userId = Assert.notNull(userId, "userId");
            return self();
        }

        public S userAttributes(List<Attribute> userAttributes) {
            this.userAttributes = Assert.notNull(userAttributes, "userAttributes");
            return self();
        }

        public final Type getType() {
            return type;
        }

        public EventContext getContext() {
            return context;
        }

        public String getUuid() {
            return uuid;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public String getUserId() {
            return userId;
        }

        public List<Attribute> getUserAttributes() {
            return userAttributes;
        }

        @SuppressWarnings("unchecked")
        protected S self() {
            return (S) this;
        }
    }
}

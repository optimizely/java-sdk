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
import com.optimizely.ab.api.Event;
import com.optimizely.ab.api.EventContext;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractEvent implements Event {
    protected final EventContext context;
    protected final String uuid;
    protected final Date timestamp;

    protected static Date timestampOrNow(@Nullable Date timestamp) {
        return (timestamp != null) ? timestamp : new Date();
    }

    protected static String uuidOrRandom(@Nullable String uuid) {
        return (uuid != null) ? uuid : nextUuid();
    }

    protected static String nextUuid() {
        return UUID.randomUUID().toString();
    }

    AbstractEvent(EventContext context, String uuid, Date timestamp) {
        this.context = Assert.notNull(context, "context");
        this.uuid = uuidOrRandom(uuid);
        this.timestamp = timestampOrNow(timestamp);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractEvent)) return false;
        final AbstractEvent that = (AbstractEvent) o;
        return Objects.equals(context, that.context) &&
            Objects.equals(uuid, that.uuid) &&
            Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, uuid, timestamp);
    }
}

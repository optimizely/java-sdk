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

import com.optimizely.ab.api.ConversionEvent;
import com.optimizely.ab.api.EventContext;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.event.internal.payload.Attribute;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Conversion extends AbstractUserEvent implements ConversionEvent {
    public static Type TYPE = Type.CONVERSION;

    private EventType event;
    private Map<String, ?> tags;

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ConversionEvent copy) {
        return new Builder(copy);
    }

    private Conversion(
        EventContext context,
        Date timestamp,
        String uuid,
        EventType event,
        String userId,
        List<Attribute> userAttributes,
        Map<String, ?> tags
    ) {
        super(context, uuidOrRandom(uuid), timestampOrNow(timestamp), userId, userAttributes);
        this.event = Assert.notNull(event, "event");
        this.tags = tags;
    }

    @Override
    public Type getType() {
        return Type.CONVERSION;
    }

    @Override
    public EventType getEvent() {
        return event;
    }

    @Override
    public Map<String, ?> getTags() {
        return tags != null ? tags : Collections.emptyMap();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conversion)) return false;
        if (!super.equals(o)) return false;
        final Conversion that = (Conversion) o;
        return Objects.equals(getEvent(), that.getEvent()) &&
            Objects.equals(getTags(), that.getTags());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getEvent(), getTags());
    }

    public static final class Builder extends AbstractBuilder<Builder> {
        private EventType event;
        private Map<String, ?> tags;

        private Builder() {
            super(TYPE);
        }

        private Builder(ConversionEvent inst) {
            super(inst);
            this.event = inst.getEvent();
            this.tags = Optional.ofNullable(inst.getTags()).map(HashMap::new).orElseGet(HashMap::new);
        }

        public Builder event(EventType event) {
            this.event = event;
            return this;
        }

        public Builder tags(Map<String, ?> tags) {
            this.tags = tags;
            return this;
        }

        public Conversion build(EventContext context) {
            return new Conversion(
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

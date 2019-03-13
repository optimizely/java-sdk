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
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.api.EventContext;
import com.optimizely.ab.api.ImpressionEvent;
import com.optimizely.ab.event.internal.payload.Attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ImpressionEventImpl extends AbstractEvent implements ImpressionEvent {
    private final Experiment experiment;
    private final Variation variation;
    private String userId;
    private final List<Attribute> userAttributes;

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ImpressionEvent copy) {
        Builder builder = new Builder();
        builder.context = copy.getContext();
        builder.uuid = copy.getUuid();
        builder.timestamp = copy.getTimestamp();
        builder.experiment = copy.getExperiment();
        builder.variation = copy.getVariation();
        builder.userId = copy.getUserId();
        builder.userAttributes = copy.getUserAttributes() != null ? new ArrayList<>(copy.getUserAttributes()) : null;
        return builder;
    }

    ImpressionEventImpl(
        EventContext context,
        Date timestamp,
        String uuid,
        Experiment experiment,
        Variation variation,
        String userId,
        List<Attribute> userAttributes
    ) {
        super(context, uuidOrRandom(uuid), timestampOrNow(timestamp));
        this.experiment = Assert.notNull(experiment, "experiment");
        this.variation = Assert.notNull(variation, "variation");
        this.userId = Assert.notNull(userId, "userId");
        this.userAttributes = (userAttributes != null) ? userAttributes : Collections.emptyList();
    }

    @Override
    public EventType getType() {
        return EventType.IMPRESSION;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public Experiment getExperiment() {
        return experiment;
    }

    @Override
    public Variation getVariation() {
        return variation;
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
        if (!(o instanceof ImpressionEventImpl)) return false;
        if (!super.equals(o)) return false;
        final ImpressionEventImpl that = (ImpressionEventImpl) o;
        return Objects.equals(experiment, that.experiment) &&
            Objects.equals(variation, that.variation) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(userAttributes, that.userAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), experiment, variation, userId, userAttributes);
    }


    public static final class Builder {
        private EventContext context;
        private String uuid;
        private Date timestamp;
        private Experiment experiment;
        private Variation variation;
        private String userId;
        private List<Attribute> userAttributes;

        private Builder() {
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

        public Builder experiment(Experiment experiment) {
            this.experiment = experiment;
            return this;
        }

        public Builder variation(Variation variation) {
            this.variation = variation;
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

        public ImpressionEventImpl build() {
            return new ImpressionEventImpl(
                context,
                timestamp,
                uuid,
                experiment,
                variation,
                userId,
                userAttributes
            );
        }
    }
}

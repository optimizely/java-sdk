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

import com.optimizely.ab.api.EventContext;
import com.optimizely.ab.api.ImpressionEvent;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.internal.payload.Attribute;

import java.util.Date;
import java.util.List;

public class Impression extends AbstractUserEvent implements ImpressionEvent {
    public static Type TYPE = Type.IMPRESSION;

    private final Experiment experiment;
    private final Variation variation;

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ImpressionEvent copy) {
        return new Builder(copy);
    }

    Impression(
        EventContext context,
        Date timestamp,
        String uuid,
        Experiment experiment,
        Variation variation,
        String userId,
        List<Attribute> userAttributes
    ) {
        super(context, uuidOrRandom(uuid), timestampOrNow(timestamp), userId, userAttributes);
        this.experiment = Assert.notNull(experiment, "experiment");
        this.variation = Assert.notNull(variation, "variation");
    }

    @Override
    public Type getType() {
        return Type.IMPRESSION;
    }

    @Override
    public Experiment getExperiment() {
        return experiment;
    }

    @Override
    public Variation getVariation() {
        return variation;
    }


    public static final class Builder extends AbstractBuilder<Builder> {
        private Experiment experiment;
        private Variation variation;

        private Builder() {
            super(TYPE);
        }

        private Builder(ImpressionEvent inst) {
            super(inst);
            this.experiment = inst.getExperiment();
            this.variation = inst.getVariation();
        }

        public Builder experiment(Experiment experiment) {
            this.experiment = experiment;
            return this;
        }

        public Builder variation(Variation variation) {
            this.variation = variation;
            return this;
        }

        @Override
        public Impression build(EventContext context) {
            return new Impression(
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

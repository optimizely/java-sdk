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
package com.optimizely.ab.processor.internal;

import com.optimizely.ab.api.ImpressionEvent;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.event.internal.Impression;

import javax.annotation.Nonnull;

public class OutgoingImpression extends OutgoingEvent<Impression.Builder, ImpressionEvent> {
    private final Impression.Builder builder;

    public static OutgoingImpression create(
        @Nonnull ProjectConfig projectConfig,
        Impression.Builder impression
    ) {
        return new OutgoingImpression(projectConfig, impression);
    }

    private OutgoingImpression(
        ProjectConfig projectConfig,
        Impression.Builder builder
    ) {
        super(projectConfig);
        this.builder = Assert.notNull(builder, "builder");
    }

    @Override
    public Impression.Builder getEventBuilder() {
        return builder;
    }

    @Override
    public ImpressionEvent buildEvent() {
        return builder.build(getEventContext());
    }
}


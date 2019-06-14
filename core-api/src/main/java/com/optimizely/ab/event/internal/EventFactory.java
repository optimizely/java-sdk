/**
 *
 *    Copyright 2016-2019, Optimizely and contributors
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

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.internal.EventTagUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

public class EventFactory {
    private static final Logger logger = LoggerFactory.getLogger(EventFactory.class);

    public static UserEvent createImpressionEvent(@Nonnull ProjectConfig projectConfig,
                                                  @Nonnull Experiment activatedExperiment,
                                                  @Nonnull Variation variation,
                                                  @Nonnull String userId,
                                                  @Nonnull Map<String, ?> attributes) {

        ImpressionEvent impressionEvent = new ImpressionEvent.Builder()
            .withLayerId(activatedExperiment.getLayerId())
            .withExperimentId(activatedExperiment.getId())
            .withExperimentKey(activatedExperiment.getKey())
            .withVariationId(variation.getId())
            .withVariationKey(variation.getKey())
            .build();

        return new UserEvent.Builder()
            .withUserId(userId)
            .withAttributes(attributes)
            .withProjectConfig(projectConfig)
            .withImpressionEvent(impressionEvent)
            .withUUID(UUID.randomUUID().toString())
            .withTimestamp(System.currentTimeMillis())
            .build();
    }

    public static UserEvent createConversionEvent(@Nonnull ProjectConfig projectConfig,
                                                  @Nonnull String userId,
                                                  @Nonnull String eventId, // Why is this not used?
                                                  @Nonnull String eventName,
                                                  @Nonnull Map<String, ?> attributes,
                                                  @Nonnull Map<String, ?> eventTags) {

        ConversionEvent conversionEvent = new ConversionEvent.Builder()
            .withEventId(eventId)
            .withEventKey(eventName)
            .withRevenue(EventTagUtils.getRevenueValue(eventTags))
            .withValue(EventTagUtils.getNumericValue(eventTags))
            .withTags(eventTags)
            .build();

        return new UserEvent.Builder()
            .withUserId(userId)
            .withAttributes(attributes)
            .withProjectConfig(projectConfig)
            .withConversionEvent(conversionEvent)
            .withUUID(UUID.randomUUID().toString())
            .withTimestamp(System.currentTimeMillis())
            .build();
    }
}

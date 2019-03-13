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
package com.optimizely.ab.api;

import com.optimizely.ab.common.internal.Assert;

import javax.annotation.Nonnull;
import java.util.Date;

public interface Event {
    EventType getType();

    Date getTimestamp();

    String getUuid();

    EventContext getContext();

    enum EventType {
        // may want to follow convention of NotificationCenter.NotificationType
        CONVERSION,
        IMPRESSION;

        public interface EventConsumer {
            void acceptConversion(ConversionEvent conversion);

            void acceptImpression(ImpressionEvent impression);
        }

        public interface EventMapper<R> {
            R applyConversion(ConversionEvent conversion);

            R applyImpression(ImpressionEvent impression);
        }

        public void handle(Event event, @Nonnull EventConsumer consumer) {
            Assert.notNull(event, "event");
            switch (this) {
                case CONVERSION:
                    consumer.acceptConversion((ConversionEvent) event);
                    break;
                case IMPRESSION:
                    consumer.acceptImpression((ImpressionEvent) event);
                    break;
            }
        }

        public <R> R map(Event event, @Nonnull EventMapper<R> mapper) {
            Assert.notNull(event, "event");
            switch (this) {
                case CONVERSION:
                    return mapper.applyConversion((ConversionEvent) event);
                case IMPRESSION:
                    return mapper.applyImpression((ImpressionEvent) event);
            }
            throw new Error("Missing case for " + this);
        }
    }
}

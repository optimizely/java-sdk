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
package com.optimizely.ab.processor;

import com.optimizely.ab.common.internal.Assert;

import javax.annotation.Nullable;
import java.time.Duration;

public interface BatchOptions {
    /**
     * @return a positive number the buffer size that triggers flush, otherwise null if size should not be enforced
     */
    @Nullable
    Integer getMaxBatchSize();

    /**
     * @return a positive duration for buffer age that triggers flush, otherwise null if age should not be enforced.
     */
    @Nullable
    Duration getMaxBatchAge();

    /**
     * Limits the number of in-flight batches (threads) In-flight batches can accumulate when batches are produced
     * faster than they can be flushed to sink.
     */
    @Nullable
    Integer getMaxBatchInFlight();

    /**
     * @return true if buffer should flush when shutdown is requested
     */
    boolean shouldFlushOnShutdown();

    /**
     * @return a builder object for {@link BatchOptions}
     */
    static <T> Builder<T> builder() {
        return new Builder<>();
    }

    class Builder<T> implements BatchOptions {
        static int MAX_BATCH_SIZE_DEFAULT = 10;
        static Duration MAX_BATCH_OPEN_MS_DEFAULT = Duration.ofMillis(250);

        private Integer maxBatchSize = MAX_BATCH_SIZE_DEFAULT;
        private Duration maxBatchAge = MAX_BATCH_OPEN_MS_DEFAULT;
        private Integer maxBatchInFlight = null;
        private boolean flushOnShutdown = true;

        private Builder() {
        }

        public Builder<T> from(BatchOptions config) {
            Assert.notNull(config, "options");
            this.maxBatchSize = config.getMaxBatchSize();
            this.maxBatchAge = config.getMaxBatchAge();
            this.maxBatchInFlight = config.getMaxBatchInFlight();
            this.flushOnShutdown = config.shouldFlushOnShutdown();
            return this;
        }

        public Builder<T> maxBatchSize(@Nullable Integer maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
            return this;
        }

        public Builder<T> maxBatchAge(@Nullable Duration maxBatchAge) {
            this.maxBatchAge = maxBatchAge;
            return this;
        }

        public Builder<T> maxBatchInFlight(@Nullable Integer maxBatchInFlight) {
            this.maxBatchInFlight = maxBatchInFlight;
            return this;
        }

        public Builder<T> flushOnShutdown(boolean flushOnShutdown) {
            this.flushOnShutdown = flushOnShutdown;
            return this;
        }

        @Override
        public Integer getMaxBatchSize() {
            return maxBatchSize;
        }

        @Override
        public Duration getMaxBatchAge() {
            return maxBatchAge;
        }

        @Override
        public Integer getMaxBatchInFlight() {
            return maxBatchInFlight;
        }

        @Override
        public boolean shouldFlushOnShutdown() {
            return flushOnShutdown;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("Builder{");
            sb.append(", maxBatchSize=").append(maxBatchSize);
            sb.append(", maxBatchAge=").append(maxBatchAge);
            sb.append(", maxBatchInFlight=").append(maxBatchInFlight);
            sb.append(", flushOnShutdown=").append(flushOnShutdown);
            sb.append('}');
            return sb.toString();
        }

        public BatchOptions build() {
            return this;
        }
    }
}

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

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Objects;

/**
 * Specifies batching-related parameters.
 */
public interface BatchOptions {
    int UNBOUNDED_SIZE = Integer.MAX_VALUE;
    long UNBOUNDED_AGE = Long.MAX_VALUE;

    /**
     * @return maximum number of elements in a single batch
     */
    int getMaxSize();

    /**
     * @return amount of time, in milliseconds, to hold a batch open before flushing
     */
    long getMaxAge();

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

    static boolean hasMaxSize(BatchOptions options) {
        return options.getMaxSize() != UNBOUNDED_SIZE;
    }

    static boolean hasMaxAge(BatchOptions options) {
        return options.getMaxAge() != UNBOUNDED_AGE;
    }

    /**
     * @return a builder object for {@link BatchOptions}
     */
    static <T> Builder<T> builder() {
        return new Builder<>();
    }

    class Builder<T> implements BatchOptions {
        private int maxBatchSize = UNBOUNDED_SIZE;
        private long maxBatchAge = UNBOUNDED_AGE;
        private Integer maxBatchInFlight = null;
        private boolean flushOnShutdown = true;

        private Builder() {
        }

        public Builder<T> maxBatchSize(@Nullable Integer maxBatchSize) {
            this.maxBatchSize = (maxBatchSize != null) ? maxBatchSize : UNBOUNDED_SIZE;
            return this;
        }

        public Builder<T> maxBatchAge(@Nullable Duration maxBatchAge) {
            this.maxBatchAge = (maxBatchAge != null) ? maxBatchAge.toMillis() : UNBOUNDED_AGE;
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
        public int getMaxSize() {
            return maxBatchSize;
        }

        @Override
        public long getMaxAge() {
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Builder)) return false;
            final Builder<?> builder = (Builder<?>) o;
            return maxBatchSize == builder.maxBatchSize &&
                maxBatchAge == builder.maxBatchAge &&
                flushOnShutdown == builder.flushOnShutdown &&
                Objects.equals(maxBatchInFlight, builder.maxBatchInFlight);
        }

        @Override
        public int hashCode() {
            return Objects.hash(maxBatchSize, maxBatchAge, maxBatchInFlight, flushOnShutdown);
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

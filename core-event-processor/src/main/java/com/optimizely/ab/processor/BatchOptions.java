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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.concurrent.Executor;

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
     * TODO move out so this is only stateless/scalar options
     * @return executor that batching tasks will be submitted to.
     */
    @Nonnull
    Executor getExecutor();
}

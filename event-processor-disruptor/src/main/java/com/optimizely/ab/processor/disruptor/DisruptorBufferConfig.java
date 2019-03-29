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
package com.optimizely.ab.processor.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.optimizely.ab.common.internal.Assert;

import java.util.concurrent.ThreadFactory;

public class DisruptorBufferConfig implements DisruptorBuffer.Config {
    private final int batchMaxSize;
    private final int capacity;
    private final ThreadFactory threadFactory;
    private final WaitStrategy waitStrategy;
    private final ExceptionHandler<Object> exceptionHandler;

    private DisruptorBufferConfig(Builder builder) {
        batchMaxSize = builder.batchMaxSize;
        capacity = builder.capacity;
        threadFactory = Assert.notNull(builder.threadFactory, "builder.threadFactory");
        waitStrategy = Assert.notNull(builder.waitStrategy, "builder.waitStrategy");
        exceptionHandler = Assert.notNull(builder.exceptionHandler, "builder.exceptionHandler");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(DisruptorBufferConfig copy) {
        Builder builder = new Builder();
        builder.batchMaxSize = copy.getBatchMaxSize();
        builder.capacity = copy.getCapacity();
        builder.threadFactory = copy.getThreadFactory();
        builder.waitStrategy = copy.getWaitStrategy();
        builder.exceptionHandler = copy.getExceptionHandler();
        return builder;
    }

    @Override
    public int getBatchMaxSize() {
        return batchMaxSize;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    @Override
    public WaitStrategy getWaitStrategy() {
        return waitStrategy;
    }

    @Override
    public ExceptionHandler<Object> getExceptionHandler() {
        return exceptionHandler;
    }

    public static final class Builder {
        private Integer batchMaxSize;
        private Integer capacity;
        private ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;
        private WaitStrategy waitStrategy = new BlockingWaitStrategy();
        private ExceptionHandler<Object> exceptionHandler = DisruptorBuffer.LoggingExceptionHandler.getInstance();

        private Builder() {
        }

        public Builder batchMaxSize(int batchMaxSize) {
            this.batchMaxSize = batchMaxSize;
            return this;
        }

        public Builder capacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder threadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public Builder waitStrategy(WaitStrategy waitStrategy) {
            this.waitStrategy = waitStrategy;
            return this;
        }

        public Builder exceptionHandler(ExceptionHandler<Object> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        public DisruptorBufferConfig build() {
            Assert.state(batchMaxSize != null, "batchMaxSize not set");
            Assert.state(capacity != null, "capacity not set");
            return new DisruptorBufferConfig(this);
        }
    }
}

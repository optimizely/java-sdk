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

import com.optimizely.ab.common.internal.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TODO: this should probably be managed as SDK-wide defaults
 */
public class EventProcessorDefaults {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessorDefaults.class);

    private static class Holder {
        private static EventProcessorDefaults instance = new EventProcessorDefaults();
    }

    public static EventProcessorDefaults get() {
        return Holder.instance;
    }

    public static void set(EventProcessorDefaults defaults) {
        Assert.isTrue(Holder.instance == null, "Already set");
        Holder.instance = defaults;
    }

    public ThreadFactory defaultThreadFactory() {
        return new ThreadFactory() {
            private final ThreadFactory delegate = Executors.defaultThreadFactory();
            private final AtomicLong threadCount = new AtomicLong(0);

            @Override
            public Thread newThread(@Nonnull final Runnable r) {
                Thread t = delegate.newThread(r);
                t.setName(String.format("optimizely-event-processor-thread-%d", threadCount.getAndIncrement()));
                t.setPriority(Thread.MIN_PRIORITY);
                logger.trace("Creating thread {}", t);
                return t;
            }
        };
    }
}

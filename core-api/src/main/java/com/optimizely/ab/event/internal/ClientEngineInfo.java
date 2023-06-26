/**
 *
 *    Copyright 2019, Optimizely and contributors
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

import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.notification.DecisionNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ClientEngineInfo is a utility to globally get and set the ClientEngine used in
 * event tracking. The ClientEngine defaults to JAVA_SDK but can be overridden at
 * runtime.
 */
public class ClientEngineInfo {
    private static final Logger logger = LoggerFactory.getLogger(ClientEngineInfo.class);

    public static final String DEFAULT_NAME = "java-sdk";
    private static String clientEngineName = DEFAULT_NAME;

    public static void setClientEngineName(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            logger.warn("ClientEngineName cannot be empty, defaulting to {}", ClientEngineInfo.clientEngineName);
            return;
        }
        ClientEngineInfo.clientEngineName = name;
    }

    @Nonnull
    public static String getClientEngineName() {
        return clientEngineName;
    }

    private ClientEngineInfo() {
    }

    @Deprecated
    public static final EventBatch.ClientEngine DEFAULT = EventBatch.ClientEngine.JAVA_SDK;
    @Deprecated
    private static EventBatch.ClientEngine clientEngine = DEFAULT;

    /**
     * @deprecated in favor of {@link #setClientEngineName(String)} which can set with arbitrary client names.
     */
    @Deprecated
    public static void setClientEngine(EventBatch.ClientEngine clientEngine) {
        if (clientEngine == null) {
            logger.warn("ClientEngine cannot be null, defaulting to {}", ClientEngineInfo.clientEngine.getClientEngineValue());
            return;
        }

        logger.info("Setting Optimizely client engine to {}", clientEngine.getClientEngineValue());
        ClientEngineInfo.clientEngine = clientEngine;
        ClientEngineInfo.clientEngineName = clientEngine.getClientEngineValue();
    }

    /**
     * @deprecated in favor of {@link #getClientEngineName()}.
     */
    @Deprecated
    public static EventBatch.ClientEngine getClientEngine() {
        return clientEngine;
    }

}

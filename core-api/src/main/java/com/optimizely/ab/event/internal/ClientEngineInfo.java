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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ClientEngineInfo is a utility to globally get and set the ClientEngine used in
 * event tracking. The ClientEngine defaults to JAVA_SDK but can be overridden at
 * runtime.
 */
public class ClientEngineInfo {
    private static final Logger logger = LoggerFactory.getLogger(ClientEngineInfo.class);

    public static final EventBatch.ClientEngine DEFAULT = EventBatch.ClientEngine.JAVA_SDK;
    private static EventBatch.ClientEngine clientEngine = DEFAULT;

    public static void setClientEngine(EventBatch.ClientEngine clientEngine) {
        if (clientEngine == null) {
            logger.warn("ClientEngine cannot be null, defaulting to {}", ClientEngineInfo.clientEngine.getClientEngineValue());
            return;
        }

        logger.info("Setting Optimizely client engine to {}", clientEngine.getClientEngineValue());
        ClientEngineInfo.clientEngine = clientEngine;
    }

    public static EventBatch.ClientEngine getClientEngine() {
        return clientEngine;
    }

    private ClientEngineInfo() {
    }
}

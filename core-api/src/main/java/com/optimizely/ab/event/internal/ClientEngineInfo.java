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

    private static EventBatch.ClientEngine clientEngine = EventBatch.ClientEngine.JAVA_SDK;

    public static void setClientEngine(EventBatch.ClientEngine clientEngine) {
        ClientEngineInfo.clientEngine = clientEngine;
    }

    public static EventBatch.ClientEngine getClientEngine() {
        return clientEngine;
    }

    private ClientEngineInfo() {
    }
}

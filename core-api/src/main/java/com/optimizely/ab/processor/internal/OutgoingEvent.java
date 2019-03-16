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

import com.optimizely.ab.api.Event;
import com.optimizely.ab.api.EventContext;
import com.optimizely.ab.common.internal.Assert;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.event.internal.AbstractUserEvent;
import com.optimizely.ab.event.internal.BuildVersionInfo;
import com.optimizely.ab.event.internal.payload.EventBatch.ClientEngine;

public abstract class OutgoingEvent<T extends AbstractUserEvent.AbstractBuilder, U extends Event> {
    // TODO store these separately somewhere else
    private static ClientEngine clientEngine = ClientEngine.JAVA_SDK;
    private static String clientVersion = BuildVersionInfo.VERSION;

    private final ProjectConfig projectConfig;
    private final EventContext eventContext;

    // TODO move this somewhere else
    static void setClientInfo(ClientEngine engine, String version) {
        clientEngine = Assert.notNull(engine, "engine");
        clientVersion = Assert.notNull(version, "version");
    }

    protected OutgoingEvent(ProjectConfig projectConfig) {
        this.projectConfig = Assert.notNull(projectConfig, "projectConfig");
        this.eventContext = EventContext.create(projectConfig, clientEngine.getClientEngineValue(), clientVersion);
    }

    public abstract T getEventBuilder();

    public abstract U buildEvent();

    public Event.Type getEventType() {
        return getEventBuilder().getType();
    }

    public ProjectConfig getProjectConfig() {
        return projectConfig;
    }

    public boolean isConversion() {
        return Event.Type.CONVERSION == getEventType();
    }

    public boolean isImpression() {
        return Event.Type.IMPRESSION == getEventType();
    }

    protected EventContext getEventContext() {
        return eventContext;
    }
}

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
package com.optimizely.ab.event;

import com.optimizely.ab.event.internal.EventFactory;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.notification.NotificationHandler;
import com.optimizely.ab.notification.NotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class ForwardingEventProcessor implements EventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ForwardingEventProcessor.class);

    private final NotificationManager<LogEvent> notificationManager = new NotificationManager<>();

    @Override
    public void process(EventBatch eventBatch) {
        logger.debug("Not processing event");
        LogEvent logEvent = new LogEvent(LogEvent.RequestMethod.POST, EventFactory.EVENT_ENDPOINT,
            Collections.emptyMap(), eventBatch);

        notificationManager.send(logEvent);
    }

    public int addEventHandler(EventHandler handler) {
        return addHandler(handler::dispatchEvent);
    }

    public int addHandler(NotificationHandler<LogEvent> handler) {
        return notificationManager.addHandler(handler);
    }
}

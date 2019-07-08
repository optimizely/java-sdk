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
import com.optimizely.ab.event.internal.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ForwardingEventProcessor is a basic transformation stage for converting
 * the event batch into a LogEvent to be dispatched.
 */
public class ForwardingEventProcessor implements EventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ForwardingEventProcessor.class);

    private final EventHandler eventHandler;

    public ForwardingEventProcessor(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public void process(UserEvent userEvent) {
        LogEvent logEvent = EventFactory.createLogEvent(userEvent);

        try {
            eventHandler.dispatchEvent(logEvent);
        } catch(Exception e) {
            logger.error("Error dispatching event: {}", logEvent, e);
        }
    }
}

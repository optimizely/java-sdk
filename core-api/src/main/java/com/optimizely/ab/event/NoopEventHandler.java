/**
 *
 *    Copyright 2016, Optimizely and contributors
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * {@link EventHandler} that logs events but <b>does not</b> perform any dispatching.
 */
public class NoopEventHandler implements EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(NoopEventHandler.class);

    @Override
    public void dispatchEvent(LogEvent logEvent) {
        logger.debug("Called dispatchEvent with URL: {} and params: {}", logEvent.getEndpointUrl(),
                     logEvent.getRequestParams());
    }
}
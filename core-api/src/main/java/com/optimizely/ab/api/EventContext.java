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
package com.optimizely.ab.api;

import com.optimizely.ab.event.internal.EventContextImpl;

public interface EventContext {
    String getAccountId();

    String getProjectId();

    String getClientName();

    String getClientVersion();

    String getRevision();

    Boolean getAnonymizeIp();

    Boolean getBotFiltering();

//        Object get(String key);
//        void set(String key, Object value);

    static EventContextImpl.Builder builder() {
        return EventContextImpl.builder();
    }
}

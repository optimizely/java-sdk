/**
 *
 *    Copyright 2016, Optimizely
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
package com.optimizely.ab.event.internal.payload;

import com.optimizely.ab.annotations.VisibleForTesting;

import com.optimizely.ab.event.internal.BuildVersionInfo;

public class Event {

    String clientEngine = "java-sdk";
    String clientVersion = BuildVersionInfo.VERSION;

    public String getClientEngine() {
        return clientEngine;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    @VisibleForTesting
    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Event))
            return false;

        Event otherEvent = (Event)other;

        return clientEngine.equals(otherEvent.clientEngine) &&
               clientVersion.equals(otherEvent.clientVersion);
    }

    @Override
    public int hashCode() {
        int result = clientEngine.hashCode();
        result = 31 * result + clientVersion.hashCode();
        return result;
    }
}

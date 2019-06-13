/**
 *
 *    Copyright 2016-2017, 2019, Optimizely and contributors
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

import com.optimizely.ab.common.callback.Callback;
import com.optimizely.ab.common.callback.CallbackHolder;
import com.optimizely.ab.common.message.Message;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.event.internal.serializer.DefaultJsonSerializer;
import com.optimizely.ab.event.internal.serializer.Serializer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Objects;

/**
 * Represents Optimizely tracking and activation events.
 */
@Immutable
public class LogEvent implements Message<EventBatch> {

    private final RequestMethod requestMethod;
    private final String endpointUrl;
    private final Map<String, String> requestParams;
    private final EventBatch eventBatch;

    private final transient CallbackHolder<EventBatch> callback = new CallbackHolder<>();

    public LogEvent(@Nonnull RequestMethod requestMethod,
                    @Nonnull String endpointUrl,
                    @Nonnull Map<String, String> requestParams,
                    EventBatch eventBatch) {
        this.requestMethod = requestMethod;
        this.endpointUrl = endpointUrl;
        this.requestParams = requestParams;
        this.eventBatch = eventBatch;
    }

    public void setCallback(@Nullable Callback<EventBatch> callback) {
        this.callback.set(callback);
    }

    //======== Getters ========//

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public Map<String, String> getRequestParams() {
        return requestParams;
    }

    public String getBody() {
        if (eventBatch == null) {
            return "";
        }

        Serializer serializer = DefaultJsonSerializer.getInstance();
        return serializer.serialize(eventBatch);
    }

    public EventBatch getEventBatch() {
        return eventBatch;
    }

    //======== Overriding method ========//

    @Override
    public String toString() {
        return "LogEvent{" +
            "requestMethod=" + requestMethod +
            ", endpointUrl='" + endpointUrl + '\'' +
            ", requestParams=" + requestParams +
            ", body='" + getBody() + '\'' +
            '}';
    }

    @Override
    public EventBatch getValue() {
        return eventBatch;
    }

    @Override
    public void markSuccess() {
        callback.success(getValue());
    }

    @Override
    public void markFailure(@Nonnull Throwable ex) {
        callback.failure(getValue(), ex);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEvent logEvent = (LogEvent) o;
        return requestMethod == logEvent.requestMethod &&
            Objects.equals(endpointUrl, logEvent.endpointUrl) &&
            Objects.equals(requestParams, logEvent.requestParams) &&
            Objects.equals(eventBatch, logEvent.eventBatch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestMethod, endpointUrl, requestParams, eventBatch);
    }

    //======== Helper classes ========//

    /**
     * The HTTP verb to use when dispatching the log event.
     */
    public enum RequestMethod {
        GET,
        POST
    }
}

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

import com.optimizely.ab.api.EventContext;
import com.optimizely.ab.event.internal.payload.EventBatch;

import java.util.Objects;

/**
 * Value object to help classify {@link EventBatch} that can be grouped together or combined.
 */
public class EventContextKey {
    private final String accountId;
    private final String projectId;
    private final String revision;
    private final String clientName;
    private final String clientVersion;
    private final boolean anonymizeIp;

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(EventContextKey copy) {
        Builder builder = new Builder();
        builder.accountId = copy.accountId;
        builder.projectId = copy.projectId;
        builder.revision = copy.revision;
        builder.clientName = copy.clientName;
        builder.clientVersion = copy.clientVersion;
        builder.anonymizeIp = copy.anonymizeIp;
        return builder;
    }

    public static EventContextKey create(EventContext eventContext) {
        return new EventContextKey(
            eventContext.getAccountId(),
            eventContext.getProjectId(),
            eventContext.getRevision(),
            eventContext.getClientName(),
            eventContext.getClientVersion(),
            eventContext.getAnonymizeIp());
    }

    public static EventContextKey create(EventBatch eventBatch) {
        return new EventContextKey(
            eventBatch.getAccountId(),
            eventBatch.getProjectId(),
            eventBatch.getRevision(),
            eventBatch.getClientName(),
            eventBatch.getClientVersion(),
            Boolean.TRUE.equals(eventBatch.getAnonymizeIp())); // default is false
    }

    private EventContextKey(Builder builder) {
        accountId = builder.accountId;
        projectId = builder.projectId;
        revision = builder.revision;
        clientName = builder.clientName;
        clientVersion = builder.clientVersion;
        anonymizeIp = builder.anonymizeIp;
    }

    private EventContextKey(
        String accountId,
        String projectId,
        String revision,
        String clientName,
        String clientVersion,
        boolean anonymizeIp
    ) {
        this.accountId = accountId;
        this.projectId = projectId;
        this.revision = revision;
        this.clientName = clientName;
        this.clientVersion = clientVersion;
        this.anonymizeIp = anonymizeIp;
    }

    /**
     * contract: provides a builder that has everything set except for visitors
     * <p>
     * alternatively, we could consume (mutate) a Builder that's passed in
     */
    public EventBatch.Builder toEventBatchBuilder() {
        return EventBatch.builder()
            .setAccountId(accountId)
            .setProjectId(projectId)
            .setRevision(revision)
            .setClientName(clientName)
            .setClientVersion(clientVersion)
            .setAnonymizeIp(anonymizeIp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventContextKey)) return false;
        final EventContextKey that = (EventContextKey) o;
        return Objects.equals(accountId, that.accountId) &&
            Objects.equals(projectId, that.projectId) &&
            Objects.equals(revision, that.revision) &&
            Objects.equals(clientName, that.clientName) &&
            Objects.equals(clientVersion, that.clientVersion) &&
            Objects.equals(anonymizeIp, that.anonymizeIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, projectId, revision, clientName, clientVersion, anonymizeIp);
    }

    public static final class Builder {
        private String accountId;
        private String projectId;
        private String revision;
        private String clientName;
        private String clientVersion;
        private boolean anonymizeIp;

        private Builder() {
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder revision(String revision) {
            this.revision = revision;
            return this;
        }

        public Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public Builder clientVersion(String clientVersion) {
            this.clientVersion = clientVersion;
            return this;
        }

        public Builder anonymizeIp(boolean anonymizeIp) {
            this.anonymizeIp = anonymizeIp;
            return this;
        }

        public EventContextKey build() {
            return new EventContextKey(this);
        }
    }
}

/*
 * Copyright 2016-2017, Optimizely and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.optimizely.ab.event.internal.payload.batch;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class Visitor {

    @Nullable private List<Attribute> attributes = new ArrayList<Attribute>();
    @Nullable private String sessionId;
    @Nonnull private List<Snapshot> snapshots = new ArrayList<Snapshot>();
    @Nonnull private String visitorId;

    public Visitor(@Nonnull String visitorId) {
        this.visitorId = visitorId;
    }

    public Visitor(@Nullable List<Attribute> attributes, @Nullable String sessionId, @Nonnull List<Snapshot> snapshots,
                   @Nonnull String visitorId) {
        this.attributes = attributes;
        this.sessionId = sessionId;
        this.snapshots = snapshots;
        this.visitorId = visitorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Visitor visitor = (Visitor) o;

        if (attributes != null ? !attributes.equals(visitor.attributes) : visitor.attributes != null) return false;
        if (sessionId != null ? !sessionId.equals(visitor.sessionId) : visitor.sessionId != null) return false;
        if (!snapshots.equals(visitor.snapshots)) return false;
        return visitorId.equals(visitor.visitorId);
    }

    @Override
    public int hashCode() {
        int result = attributes != null ? attributes.hashCode() : 0;
        result = 31 * result + (sessionId != null ? sessionId.hashCode() : 0);
        result = 31 * result + snapshots.hashCode();
        result = 31 * result + visitorId.hashCode();
        return result;
    }

    @Nullable
    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(@Nullable List<Attribute> attributes) {
        this.attributes = attributes;
    }

    @Nullable
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(@Nullable String sessionId) {
        this.sessionId = sessionId;
    }

    @Nonnull
    public List<Snapshot> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(@Nonnull List<Snapshot> snapshots) {
        this.snapshots = snapshots;
    }

    @Nonnull
    public String getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(@Nonnull String visitorId) {
        this.visitorId = visitorId;
    }
}

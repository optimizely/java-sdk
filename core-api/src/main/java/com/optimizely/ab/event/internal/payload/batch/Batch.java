/*
 *
 *    Copyright 2016-2017, Optimizely and contributors
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

package com.optimizely.ab.event.internal.payload.batch;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class Batch extends com.optimizely.ab.event.internal.payload.Event{

    @Nonnull private String accountId;
    @Nullable private Boolean anonymizeIp;
    @Nullable private String clientName;
    @Nullable private String projectId;
    @Nonnull private List<Visitor> visitors = new ArrayList<Visitor>();

    public Batch(@Nonnull String accountId) {
        this.accountId = accountId;
    }

    public Batch(@Nonnull String accountId, @Nullable Boolean anonymizeIp, @Nullable String clientName,
                 @Nullable String projectId, @Nonnull List<Visitor> visitors) {
        this.accountId = accountId;
        this.anonymizeIp = anonymizeIp;
        this.clientName = clientName;
        this.projectId = projectId;
        this.visitors = visitors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Batch batch = (Batch) o;

        if (!accountId.equals(batch.accountId)) return false;
        if (anonymizeIp != null ? !anonymizeIp.equals(batch.anonymizeIp) : batch.anonymizeIp != null) return false;
        if (clientName != null ? !clientName.equals(batch.clientName) : batch.clientName != null) return false;
        if (projectId != null ? !projectId.equals(batch.projectId) : batch.projectId != null) return false;
        return visitors.equals(batch.visitors);
    }

    @Override
    public int hashCode() {
        int result = accountId.hashCode();
        result = 31 * result + (anonymizeIp != null ? anonymizeIp.hashCode() : 0);
        result = 31 * result + (clientName != null ? clientName.hashCode() : 0);
        result = 31 * result + (projectId != null ? projectId.hashCode() : 0);
        result = 31 * result + visitors.hashCode();
        return result;
    }

    @Nonnull
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(@Nonnull String accountId) {
        this.accountId = accountId;
    }

    @Nullable
    public Boolean getAnonymizeIp() {
        return anonymizeIp;
    }

    public void setAnonymizeIp(@Nullable Boolean anonymizeIp) {
        this.anonymizeIp = anonymizeIp;
    }

    @Nullable
    public String getClientName() {
        return clientName;
    }

    public void setClientName(@Nullable String clientName) {
        this.clientName = clientName;
    }

    @Nullable
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(@Nullable String projectId) {
        this.projectId = projectId;
    }

    @Nonnull
    public List<Visitor> getVisitors() {
        return visitors;
    }

    public void setVisitors(@Nonnull List<Visitor> visitors) {
        this.visitors = visitors;
    }
}

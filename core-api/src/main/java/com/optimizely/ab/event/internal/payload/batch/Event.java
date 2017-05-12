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
import java.util.Map;

public class Event {

    @Nullable private String entityId;
    @Nonnull private String key;
    @Nullable private Long quantity;
    @Nullable private Long revenue;
    @Nullable private Map<String, ?> tags;
    @Nonnull private Long timestamp;
    @Nonnull private String uuid;
    @Nullable private Double value;

    public Event(@Nonnull String key, @Nonnull Long timestamp, @Nonnull String uuid) {
        this.timestamp = timestamp;
        this.uuid = uuid;
        this.key = key;
    }

    public Event(@Nullable String entityId, @Nonnull String key, @Nullable Long quantity, @Nullable Long revenue,
                 @Nullable Map<String, Object> tags, @Nonnull Long timestamp, @Nonnull String uuid, @Nullable Double value) {
        this.entityId = entityId;
        this.key = key;
        this.quantity = quantity;
        this.revenue = revenue;
        this.tags = tags;
        this.timestamp = timestamp;
        this.uuid = uuid;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Event event = (Event) o;

        if (entityId != null ? !entityId.equals(event.entityId) : event.entityId != null) return false;
        if (!key.equals(event.key)) return false;
        if (quantity != null ? !quantity.equals(event.quantity) : event.quantity != null) return false;
        if (revenue != null ? !revenue.equals(event.revenue) : event.revenue != null) return false;
        if (tags != null ? !tags.equals(event.tags) : event.tags != null) return false;
        if (!timestamp.equals(event.timestamp)) return false;
        if (!uuid.equals(event.uuid)) return false;
        return value != null ? value.equals(event.value) : event.value == null;
    }

    @Override
    public int hashCode() {
        int result = entityId != null ? entityId.hashCode() : 0;
        result = 31 * result + key.hashCode();
        result = 31 * result + (quantity != null ? quantity.hashCode() : 0);
        result = 31 * result + (revenue != null ? revenue.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + timestamp.hashCode();
        result = 31 * result + uuid.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Nullable
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(@Nullable String entityId) {
        this.entityId = entityId;
    }

    @Nonnull
    public String getKey() {
        return key;
    }

    public void setKey(@Nonnull String key) {
        this.key = key;
    }

    @Nullable
    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(@Nullable Long quantity) {
        this.quantity = quantity;
    }

    @Nullable
    public Long getRevenue() {
        return revenue;
    }

    public void setRevenue(@Nullable Long revenue) {
        this.revenue = revenue;
    }

    @Nullable
    public Map<String, ?> getTags() {
        return tags;
    }

    public void setTags(@Nullable Map<String, ?> tags) {
        this.tags = tags;
    }

    @Nonnull
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(@Nonnull Long timestamp) {
        this.timestamp = timestamp;
    }

    @Nonnull
    public String getUuid() {
        return uuid;
    }

    public void setUuid(@Nonnull String uuid) {
        this.uuid = uuid;
    }

    @Nullable
    public Double getValue() {
        return value;
    }

    public void setValue(@Nullable Double value) {
        this.value = value;
    }
}

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

import com.optimizely.ab.event.internal.payload.Feature;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Attribute {

    public static final String CUSTOM_ATTRIBUTE_FEATURE_TYPE = "custom";
    public static final String EVENT_FEATURE_TYPE = "custom";

    @Nullable private String entityId;
    @Nullable private String key;
    @Nonnull private String type;
    @Nonnull private Object value;

    public Attribute(@Nonnull String type, @Nonnull Object value) {
        this.type = type;
        this.value = value;
    }

    public Attribute(@Nullable String entityId, @Nullable String key, @Nonnull String type, @Nonnull Object value) {
        this.entityId = entityId;
        this.key = key;
        this.type = type;
        this.value = value;
    }

    public Attribute(@Nonnull Feature feature) {
        this.entityId = feature.getId();
        this.key = feature.getName();
        this.type = feature.getType();
        this.value = feature.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (entityId != null ? !entityId.equals(attribute.entityId) : attribute.entityId != null) return false;
        if (key != null ? !key.equals(attribute.key) : attribute.key != null) return false;
        if (!type.equals(attribute.type)) return false;
        return value.equals(attribute.value);
    }

    @Override
    public int hashCode() {
        int result = entityId != null ? entityId.hashCode() : 0;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + type.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Nullable
    public String getEntityId() {

        return entityId;
    }

    public void setEntityId(@Nullable String entityId) {
        this.entityId = entityId;
    }

    @Nullable
    public String getKey() {
        return key;
    }

    public void setKey(@Nullable String key) {
        this.key = key;
    }

    @Nonnull
    public String getType() {
        return type;
    }

    public void setType(@Nonnull String type) {
        this.type = type;
    }

    @Nonnull
    public Object getValue() {
        return value;
    }

    public void setValue(@Nonnull Object value) {
        this.value = value;
    }
}

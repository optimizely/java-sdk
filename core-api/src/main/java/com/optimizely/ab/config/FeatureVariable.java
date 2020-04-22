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
package com.optimizely.ab.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import javax.print.DocFlavor;

/**
 * Represents a feature variable definition at the project level
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureVariable implements IdKeyMapped {

    public enum VariableStatus {
        @SerializedName("active")
        ACTIVE("active"),

        @SerializedName("archived")
        ARCHIVED("archived");

        private final String variableStatus;

        VariableStatus(String variableStatus) {
            this.variableStatus = variableStatus;
        }

        @JsonValue
        public String getVariableStatus() {
            return variableStatus;
        }

        public static VariableStatus fromString(String variableStatusString) {
            if (variableStatusString != null) {
                for (VariableStatus variableStatusEnum : VariableStatus.values()) {
                    if (variableStatusString.equals(variableStatusEnum.getVariableStatus())) {
                        return variableStatusEnum;
                    }
                }
            }

            return null;
        }
    }

    public static final String STRING_TYPE = "string";
    public static final String INTEGER_TYPE = "integer";
    public static final String DOUBLE_TYPE = "double";
    public static final String BOOLEAN_TYPE = "boolean";
    public static final String JSON_TYPE = "json";

    private final String id;
    private final String key;
    private final String defaultValue;
    private final String type;
    private final String subType;        // this is for backward-compatibility (json type)
    @Nullable
    private final VariableStatus status;

    @JsonCreator
    public FeatureVariable(@JsonProperty("id") String id,
                           @JsonProperty("key") String key,
                           @JsonProperty("defaultValue") String defaultValue,
                           @JsonProperty("status") VariableStatus status,
                           @JsonProperty("type") String type,
                           @JsonProperty("subType") String subType) {
        this.id = id;
        this.key = key;
        this.defaultValue = defaultValue;
        this.status = status;
        this.type = type;
        this.subType = subType;
    }

    @Nullable
    public VariableStatus getStatus() {
        return status;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getType() {
        if (type.equals(STRING_TYPE) && subType.equals(JSON_TYPE)) return JSON_TYPE;
        return type;
    }

    @Override
    public String toString() {
        return "FeatureVariable{" +
            "id='" + id + '\'' +
            ", key='" + key + '\'' +
            ", defaultValue='" + defaultValue + '\'' +
            ", type=" + type +
            ", subType=" + subType +
            ", status=" + status +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeatureVariable variable = (FeatureVariable) o;

        if (!id.equals(variable.id)) return false;
        if (!key.equals(variable.key)) return false;
        if (!defaultValue.equals(variable.defaultValue)) return false;
        if (!type.equals(variable.type)) return false;
        return status == variable.status;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + key.hashCode();
        result = 31 * result + defaultValue.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + subType.hashCode();
        result = 31 * result + status.hashCode();
        return result;
    }
}

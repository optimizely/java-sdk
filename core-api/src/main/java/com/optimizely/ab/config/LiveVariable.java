/**
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
package com.optimizely.ab.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.gson.annotations.SerializedName;
import com.optimizely.ab.faultinjection.ExceptionSpot;
import com.optimizely.ab.faultinjection.FaultInjectionManager;

import javax.annotation.Nullable;

/**
 * Represents a live variable definition at the project level
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveVariable implements IdKeyMapped {

    private static void injectFault(ExceptionSpot spot) {
        FaultInjectionManager.getInstance().injectFault(spot);
    }

    public enum VariableStatus {
        @SerializedName("active")
        ACTIVE ("active"),

        @SerializedName("archived")
        ARCHIVED ("archived");

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

    public enum VariableType {
        @SerializedName("boolean")
        BOOLEAN ("boolean"),

        @SerializedName("integer")
        INTEGER ("integer"),

        @SerializedName("string")
        STRING ("string"),

        @SerializedName("double")
        DOUBLE ("double");

        private final String variableType;

        VariableType(String variableType) {
            this.variableType = variableType;
        }

        @JsonValue
        public String getVariableType() {
            return variableType;
        }

        public static VariableType fromString(String variableTypeString) {
            if (variableTypeString != null) {
                for (VariableType variableTypeEnum : VariableType.values()) {
                    if (variableTypeString.equals(variableTypeEnum.getVariableType())) {
                        return variableTypeEnum;
                    }
                }
            }

            return null;
        }
    }

    private final String id;
    private final String key;
    private final String defaultValue;
    private final VariableType type;
    @Nullable private final VariableStatus status;

    @JsonCreator
    public LiveVariable(@JsonProperty("id") String id,
                        @JsonProperty("key") String key,
                        @JsonProperty("defaultValue") String defaultValue,
                        @JsonProperty("status") VariableStatus status,
                        @JsonProperty("type") VariableType type) {
            this.id = id;
            this.key = key;
            this.defaultValue = defaultValue;
            this.status = status;
            this.type = type;
    }

    public @Nullable VariableStatus getStatus() {
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

    public VariableType getType() {
        return type;
    }

    @Override
    public String toString() {
        try {
            injectFault(ExceptionSpot.LiveVariable_toString_spot1);
            return "LiveVariable{" +
                    "id='" + id + '\'' +
                    ", key='" + key + '\'' +
                    ", defaultValue='" + defaultValue + '\'' +
                    ", type=" + type +
                    ", status=" + status +
                    '}';
        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        try {
            injectFault(ExceptionSpot.LiveVariable_equals_spot1);
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LiveVariable variable = (LiveVariable) o;
            injectFault(ExceptionSpot.LiveVariable_equals_spot2);
            if (!id.equals(variable.id)) return false;
            if (!key.equals(variable.key)) return false;
            if (!defaultValue.equals(variable.defaultValue)) return false;
            if (type != variable.type) return false;
            return status == variable.status;
        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
            return false;
        }
    }

    @Override
    public int hashCode() {
        try {
            injectFault(ExceptionSpot.LiveVariable_hasCode_spot1);
            int result = id.hashCode();
            result = 31 * result + key.hashCode();
            result = 31 * result + defaultValue.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + status.hashCode();
            return result;
        } catch (Exception e) {
            FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled();
            return 0;
        }
    }
}

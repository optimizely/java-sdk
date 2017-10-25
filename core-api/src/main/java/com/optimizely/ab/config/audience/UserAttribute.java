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
package com.optimizely.ab.config.audience;

import javax.annotation.concurrent.Immutable;
import java.util.Map;

/**
 * Represents a user attribute instance within an audience's conditions.
 */
@Immutable
public class UserAttribute implements Condition {

    private final String name;
    private final String type;
    private final String value;

    public UserAttribute(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public boolean evaluate(Map<String, String> attributes) {
        String userAttributeValue = attributes.get(name);

        return value == userAttributeValue;
    }

    @Override
    public String toString() {
        return "{name='" + name + "\'" +
               ", type='" + type + "\'" +
               ", value='" + value + "\'" +
               "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserAttribute that = (UserAttribute) o;

        if (!name.equals(that.name)) return false;
        if (!type.equals(that.type)) return false;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}

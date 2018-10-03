/**
 *
 *    Copyright 2016-2018, Optimizely and contributors
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

import com.optimizely.ab.config.audience.match.MatchType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.Map;

/**
 * Represents a user attribute instance within an audience's conditions.
 */
@Immutable
public class UserAttribute implements Condition {

    private final String name;
    private final String type;
    private final String match;
    private final Object value;

    public UserAttribute(@Nonnull String name, @Nonnull String type, @Nullable String match, @Nullable Object value) {
        this.name = name;
        this.type = type;
        this.match = match;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getMatch() {
        return match;
    }

    public Object getValue() {
        return value;
    }

    public @Nullable Boolean evaluate(Map<String, ?> attributes) {
        if (attributes == null) {
            attributes = Collections.emptyMap();
        }
        // Valid for primative types, but needs to change when a value is an object or an array
        Object userAttributeValue = attributes.get(name);

        if (!"custom_attribute".equals(type)) {
            MatchType.logger.error(String.format("condition type not equal to `custom_attribute` %s", type != null ? type : ""));
            return null; // unknown type
        }
        // check user attribute value is equal
        try {
            return MatchType.getMatchType(match, value).getMatcher().eval(userAttributeValue);
        }
        catch (NullPointerException np) {
            MatchType.logger.error(String.format("attribute or value null for match %s", match != null ? match : "legacy condition"),np);
            return null;
        }

    }

    @Override
    public String toString() {
        return "{name='" + name + "\'" +
               ", type='" + type + "\'" +
               ", value='" + value.toString() + "\'" +
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

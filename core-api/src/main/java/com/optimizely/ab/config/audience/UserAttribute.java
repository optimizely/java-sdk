/**
 *
 *    Copyright 2016-2020, Optimizely and contributors
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.match.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.Map;

/**
 * Represents a user attribute instance within an audience's conditions.
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAttribute<T> implements Condition<T> {

    private static final Logger logger = LoggerFactory.getLogger(UserAttribute.class);
    private final String name;
    private final String type;
    private final String match;
    private final Object value;

    @JsonCreator
    public UserAttribute(@JsonProperty("name") @Nonnull String name,
                         @JsonProperty("type") @Nonnull String type,
                         @JsonProperty("match") @Nullable String match,
                         @JsonProperty("value") @Nullable Object value) {
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

    @Nullable
    public Boolean evaluate(ProjectConfig config, Map<String, ?> attributes) {
        if (attributes == null) {
            attributes = Collections.emptyMap();
        }
        // Valid for primitive types, but needs to change when a value is an object or an array
        Object userAttributeValue = attributes.get(name);

        if (!"custom_attribute".equals(type)) {
            logger.warn("Audience condition \"{}\" uses an unknown condition type. You may need to upgrade to a newer release of the Optimizely SDK.", this);
            return null; // unknown type
        }
        // check user attribute value is equal
        try {
            Match matcher = MatchRegistry.getMatch(match);
            Boolean result = matcher.eval(value, userAttributeValue);

            if (result == null) {
                if (!attributes.containsKey(name)) {
                    //Missing attribute value
                    logger.debug("Audience condition \"{}\" evaluated to UNKNOWN because no value was passed for user attribute \"{}\"", this, name);
                } else {
                    //if attribute value is not valid
                    if (userAttributeValue != null) {
                        logger.warn(
                            "Audience condition \"{}\" evaluated to UNKNOWN because a value of type \"{}\" was passed for user attribute \"{}\"",
                            this,
                            userAttributeValue.getClass().getCanonicalName(),
                            name);
                    } else {
                        logger.debug(
                            "Audience condition \"{}\" evaluated to UNKNOWN because a null value was passed for user attribute \"{}\"",
                            this,
                            name);
                    }
                }
            }
            return result;
        } catch (UnknownMatchTypeException e) {
            logger.warn("Audience condition \"{}\" " + e.getMessage(),
                this);
        } catch (NullPointerException e) {
            logger.error("attribute or value null for match {}", match != null ? match : "legacy condition", e);
        }
        return null;
    }

    @Override
    public String toString() {
        final String valueStr;
        if (value == null) {
            valueStr = "null";
        } else if (value instanceof String) {
            valueStr = String.format("'%s'", value);
        } else {
            valueStr = value.toString();
        }
        return "{name='" + name + "\'" +
            ", type='" + type + "\'" +
            ", match='" + match + "\'" +
            ", value=" + valueStr +
            "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserAttribute that = (UserAttribute) o;

        if (!name.equals(that.name)) return false;
        if (!type.equals(that.type)) return false;
        if (match != null ? !match.equals(that.match) : that.match != null) return false;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (match != null ? match.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}

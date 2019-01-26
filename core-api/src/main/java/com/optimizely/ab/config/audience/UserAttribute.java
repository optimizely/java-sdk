/**
 *
 *    Copyright 2016-2019, Optimizely and contributors
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
import com.optimizely.ab.internal.ConditionUtils;
import com.optimizely.ab.internal.InvalidAudienceCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Map;

/**
 * Represents a leaf node in audience condition AST.
 *
 * Defines a matching operation to evaluate on a specific attribute.
 * The behavior of {@link #evaluate(ProjectConfig, Map)} is determined by specified
 * match operation.
 *
 * If the given match type and value are not compatible, i.e. a substring match with non-string value,
 * this class will always evaluate to UNKNOWN by returning {@code null}.
 *
 * TODO(llinn) convert this to a POJO and separate from {@link Condition} evaluation
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
        // TODO(llinn) don't create inner condition every evaluation
        Condition<?> delegate = getCondition();
        try {
            return delegate.evaluate(config, attributes);
        } catch (Exception e) {
            logger.error("Evaluation failed for '{}' match condition on '{}' attribute", match, name, e);
        }
        return null;
    }

    /**
     * Provides a {@link Condition} that implements match-specific behavior.
     *
     * If this object does hold a valid configuration for a leaf condition,
     * i.e. an unknown {@link #match} type or {@link #value} that is not compatible
     * with {@link #match}, this will return a {@link Condition} that always evaluates
     * to UNKNOWN.
     */
    private Condition<?> getCondition() {
        Condition<?> condition;
        try {
            condition = ConditionUtils.leafCondition(type, match, name, value);
        } catch (InvalidAudienceCondition e) {
            logger.warn(
                "Audience condition \"{}\" is invalid: {}. You may need to upgrade to a newer release of the Optimizely SDK",
                this,
                e.getMessage());

            condition = ConditionUtils.voidCondition();
        }

        logger.debug("Audience condition \"{}\" will be evaluated using: {}", this, condition);

        return condition;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", match='").append(match).append('\'');
        if (value instanceof CharSequence) {
            sb.append(", value='").append(value).append('\'');
        } else {
            sb.append(", value=").append(value);
        }
        sb.append('}');
        return sb.toString();
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

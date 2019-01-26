/**
 * Copyright 2019, Optimizely and contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.config.audience.match;

import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for {@link Condition} implementations that evaluate an untyped
 * {@link Object} value associated to an attribute.
 *
 * Defines an interface to evaluate match in 3 steps:
 * <ol>
 *   <li>extracts the untyped attribute value to evaluate</li>
 *   <li>filter the attribute to types supported by match logic</li>
 *   <li>evaluate whether typed input is a match</li>
 * </ol>
 *
 * @param <T> type of value that input is matched against
 * @param <U> type of converted input that is required to match
 */
public abstract class AbstractAttributeCondition<T, U> implements Condition<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractAttributeCondition.class);

    protected final String matchKey;
    protected final String attributeKey;
    protected final Class<U> inputClass;

    /**
     * @param matchKey
     * @param inputClass class that is required for input conversion
     * @param attributeKey name of attribute to evaluate
     */
    protected AbstractAttributeCondition(String matchKey, @Nonnull Class<U> inputClass, @Nonnull String attributeKey) {
        this.matchKey = matchKey;
        this.attributeKey = attributeKey;
        this.inputClass = inputClass;
    }

    /**
     * Extracts attribute value for this attribute.
     *
     * If missing or null, invokes {@link #evaluateUnset()}
     * If unexpected type, invokes {@link #evaluateUnknown(Object)}
     * Otherwise, invoked {@link #evaluateAttribute(Object)}
     */
    @Nullable
    @Override
    public final Boolean evaluate(ProjectConfig config, Map<String, ?> attributes) {
        Object input = attributes != null ? attributes.get(attributeKey) : null;
        if (input == null) {
            return evaluateUnset();
        }

        if (!inputClass.isInstance(input)) {
            return evaluateUnknown(input);
        }

        return evaluateAttribute(inputClass.cast(input));
    }

    /**
     * Provides the evaluation result when valid attribute value exists.
     *
     * @param input non-null typed value associated to {@link #attributeKey}
     * @return true if positive match, false if negative match, otherwise null
     */
    @Nullable
    protected abstract Boolean evaluateAttribute(@Nonnull U input);

    /**
     * Provides the evaluation result when attribute is not provided or {@code null}.
     *
     * @return true if positive match, false if negative match, otherwise null
     */
    @Nullable
    protected Boolean evaluateUnset() {
        logger.debug("Skipping match condition on unset attribute: {}", attributeKey);

        return null;
    }

    /**
     * Provides the evaluation result when attribute is not instance of {@link #inputClass}.
     *
     * @param input non-null input value associated to {@link #attributeKey}
     * @return true if positive match, false if negative match, otherwise null
     */
    @Nullable
    protected Boolean evaluateUnknown(Object input) {
        logger.warn(
            "Unable to evaluate condition for \"{}\" attribute: cannot convert from \"{}\" to \"{}\"",
            attributeKey,
            input.getClass().getName(),
            inputClass.getName());

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractAttributeCondition)) return false;
        final AbstractAttributeCondition<?, ?> that = (AbstractAttributeCondition<?, ?>) o;
        return Objects.equals(matchKey, that.matchKey) &&
            Objects.equals(attributeKey, that.attributeKey) &&
            Objects.equals(inputClass, that.inputClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchKey, attributeKey, inputClass);
    }
}

/**
 *
 *    Copyright 2019, Optimizely and contributors
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
package com.optimizely.ab.config.audience.match;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Matches input using standard {@link Object#equals(Object)}.
 *
 * If an attribute is set to value that is not an instance of {@code inputClass},
 * this will always evaluate to UNKNOWN.
 *
 * @param <T> type of value to match input against
 */
public class EqualsAttributeCondition<T, U> extends AbstractAttributeCondition<T, U> {
    private final T value;
    private final boolean eager;

    /**
     * Optionally configured for eager evaluation upon missing input value, otherwise when
     * input is unset, this condition evaluates to UNKNOWN.
     */
    public EqualsAttributeCondition(
        String matchKey,
        @Nonnull String attributeKey,
        @Nonnull T value,
        Class<U> inputClass,
        boolean eager
    ) {
        super(matchKey, inputClass, attributeKey);
        this.value = value;
        this.eager = eager;
    }

    @Nullable
    protected Boolean evaluateAttribute(@Nonnull U input) {
        return value.equals(input);
    }

    @Nullable
    @Override
    protected Boolean evaluateUnset() {
        return eager ? value == null : null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EqualsAttributeCondition{");
        sb.append("value=").append(value);
        sb.append(", attributeKey='").append(attributeKey).append('\'');
        sb.append(", inputClass=").append(inputClass);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EqualsAttributeCondition)) return false;
        if (!super.equals(o)) return false;
        final EqualsAttributeCondition<?, ?> that = (EqualsAttributeCondition<?, ?>) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value, eager);
    }
}

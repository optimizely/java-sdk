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

import com.optimizely.ab.internal.AttributesUtil;
import com.optimizely.ab.internal.InvalidAudienceCondition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Abstract condition for {@link Number} comparisons.
 * Comparisons are made by {@link Double#compare(double, double)}.
 */
public abstract class NumericAttributeCondition extends AbstractAttributeCondition<Number, Number> {
    private final Number operand;

    /**
     * @throws InvalidAudienceCondition if number is not valid. See {@link AttributesUtil#isValidNumber(Object)}
     */
    NumericAttributeCondition(
        String matchKey,
        @Nonnull String attributeKey,
        @Nonnull Number operand
    ) throws InvalidAudienceCondition {
        super(matchKey, Number.class, attributeKey);

        if (!AttributesUtil.isValidNumber(operand)) {
            throw new InvalidAudienceCondition("unsupported number type or range");
        }

        this.operand = operand;
    }

    @Nullable
    protected Boolean evaluateAttribute(@Nonnull Number input) {
        if (!AttributesUtil.isValidNumber(input)) {
            return null;
        }

        return evaluateCompare(Double.compare(input.doubleValue(), operand.doubleValue()));
    }

    /**
     * @param cmp result of {@link Double#compare(double, double)}
     * @return true if positive match, false if negative match, otherwise null
     */
    protected abstract Boolean evaluateCompare(int cmp);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumericAttributeCondition)) return false;
        if (!super.equals(o)) return false;
        final NumericAttributeCondition that = (NumericAttributeCondition) o;
        return Objects.equals(operand, that.operand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), operand);
    }

    /**
     * Condition that matches {@link Number} attribute that is greater than the configured operand.
     */
    public static final class GreaterThan extends NumericAttributeCondition {
        public GreaterThan(String matchKey, @Nonnull String name, @Nonnull Number operand) throws InvalidAudienceCondition {
            super(matchKey, name, operand);
        }

        @Override
        protected Boolean evaluateCompare(int cmp) {
            return cmp > 0;
        }
    }

    /**
     * Condition that matches {@link Number} attribute that is less than the configured operand.
     */
    public static final class LessThan extends NumericAttributeCondition {
        public LessThan(String matchKey, @Nonnull String name, @Nonnull Number operand) throws InvalidAudienceCondition {
            super(matchKey, name, operand);
        }

        @Override
        protected Boolean evaluateCompare(int cmp) {
            return cmp < 0;
        }
    }

    /**
     * Condition that matches {@link Number} attribute that is equal to than the configured operand.
     */
    public static final class EqualTo extends NumericAttributeCondition {
        public EqualTo(String matchKey, @Nonnull String name, @Nonnull Number operand) throws InvalidAudienceCondition {
            super(matchKey, name, operand);
        }

        @Override
        protected Boolean evaluateCompare(int cmp) {
            return cmp == 0;
        }
    }
}

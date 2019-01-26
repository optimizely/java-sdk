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
 * Condition that matches {@link String} attribute that contains the specified {@link CharSequence} value.
 */
public class SubstringAttributeCondition extends AbstractAttributeCondition<CharSequence, CharSequence> {
    private final CharSequence substring;

    public SubstringAttributeCondition(
        String matchKey,
        @Nonnull String attributeKey,
        @Nonnull CharSequence substring
    ) {
        super(matchKey, CharSequence.class, attributeKey);
        this.substring = substring;
    }

    @Override
    @Nullable
    protected Boolean evaluateAttribute(@Nonnull CharSequence input) {
        return input.toString().contains(substring);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SubstringAttributeCondition{");
        sb.append("substring=").append(substring);
        sb.append(", attributeKey='").append(attributeKey).append('\'');
        sb.append(", inputClass=").append(inputClass);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubstringAttributeCondition)) return false;
        if (!super.equals(o)) return false;
        final SubstringAttributeCondition that = (SubstringAttributeCondition) o;
        return Objects.equals(substring, that.substring);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), substring);
    }
}


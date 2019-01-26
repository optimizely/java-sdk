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
 * Matches content of {@link String} against any {@link CharSequence} input.
 */
public class StringAttributeCondition extends AbstractAttributeCondition<String, CharSequence> {
    private final String content;

    public StringAttributeCondition(String matchKey, @Nonnull String attributeKey, @Nonnull String content) {
        super(matchKey, CharSequence.class, attributeKey);
        this.content = content;
    }

    @Nullable
    @Override
    protected Boolean evaluateAttribute(@Nonnull CharSequence input) {
        return content.contentEquals(input);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StringAttributeCondition{");
        sb.append("content='").append(content).append('\'');
        sb.append(", attributeKey='").append(attributeKey).append('\'');
        sb.append(", inputClass=").append(inputClass);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringAttributeCondition)) return false;
        if (!super.equals(o)) return false;
        final StringAttributeCondition that = (StringAttributeCondition) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), content);
    }
}

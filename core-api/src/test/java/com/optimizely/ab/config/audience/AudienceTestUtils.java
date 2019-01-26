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
package com.optimizely.ab.config.audience;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class AudienceTestUtils {
    private AudienceTestUtils() {
    }

    public static <T> Condition<T> andCondition(Condition... conditions) {
        return new AndCondition<>(Arrays.asList(conditions));
    }

    public static <T> Condition<T> orCondition(Condition... conditions) {
        return new OrCondition<>(Arrays.asList(conditions));
    }

    public static <T> Condition<T> notCondition(Condition condition) {
        return new NotCondition<>(condition);
    }

    public static UserAttribute matchAny(String attribute) {
        return matchAttribute(attribute, "exists", null);
    }

    public static UserAttribute matchExact(String attribute, Object value) {
        return matchAttribute(attribute, "exact", value);
    }

    public static UserAttribute matchSubstring(String attribute, CharSequence value) {
        return matchAttribute(attribute, "substring", value);
    }

    public static UserAttribute matchGreaterThan(String attribute, Number value) {
        return matchAttribute(attribute, "gt", value);
    }

    public static UserAttribute matchLessThan(String attribute, Number value) {
        return matchAttribute(attribute, "lt", value);
    }

    public static UserAttribute matchLegacy(String attribute, Object value) {
                return matchAttribute(attribute, null, value);
    }

    public static UserAttribute matchAttribute(String attribute, String match, Object value) {
        return new UserAttribute<>(attribute, "custom_attribute", match, value);
    }

    static AttributesBuilder attributes() {
        return new AttributesBuilder();
    }

    static class AttributesBuilder {
        Map<String, Object> m = new HashMap<>();

        AttributesBuilder add(String name, Object value) {
            m.put(name, value);
            return this;
        }

        Map<String, ?> get() {
            return m;
        }
    }
}

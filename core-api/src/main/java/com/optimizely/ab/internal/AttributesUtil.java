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
package com.optimizely.ab.internal;

public final class AttributesUtil {
    private AttributesUtil() {
    }

    /**
     * Checks whether an object is a supported number value for attribute conditions.
     *
     * Returns true iff given {@link Object} is instance of {@link Number}.
     *
     * A supported number is defined as: a finite number with absolute value less than 2<sup>53</sup> (9.0071993e+15)
     * and one of the following types: {@link Integer}, {@link Long}, {@link Float}, {@link Double}.
     *
     * TODO(llinn) be should either be less restrictive or provide stronger rationale for these constraints.
     *
     * @param value attribute value or condition value
     * @return true if value is valid and can safely be casted to {@link Number}, otherwise false
     */
    public static boolean isValidNumber(Object value) {
        if (value instanceof Integer) {
            return Math.abs((Integer) value) <= Math.pow(2, 53);
        } else if (value instanceof Double || value instanceof Float) {
            Double doubleValue = ((Number) value).doubleValue();
            return !(doubleValue.isNaN() || doubleValue.isInfinite() || Math.abs(doubleValue) > Math.pow(2, 53));
        } else if (value instanceof Long) {
            return Math.abs((Long) value) <= Math.pow(2, 53);
        }
        return false;
    }
}

/**
 *
 *    Copyright 2019-2020, Optimizely and contributors
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

public class AttributesUtil {

    /**
     * Validate that value is not infinite, NAN or greater than Math.pow(2, 53).
     *
     * @param value attribute value or condition value.
     * @return boolean value of is valid or not.
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

    /**
     * Parse and validate that String is parse able to integer.
     *
     * @param str String value of integer.
     * @return Integer value if is valid and null if not.
     */
    public static Integer parseNumeric(String str) {
        try {
            return Integer.parseInt(str, 10);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

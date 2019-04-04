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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

    public static boolean isValidRegex(Object regex) {
        if(regex instanceof String) {
            try {
                Pattern.compile((String) regex);
            } catch (PatternSyntaxException exception) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

}

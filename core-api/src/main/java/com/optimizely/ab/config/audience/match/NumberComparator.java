/**
 *
 *    Copyright 2020, Optimizely and contributors
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

import static com.optimizely.ab.internal.AttributesUtil.isValidNumber;

/**
 * NumberComparator performs a numeric comparison. The input values are assumed to be numbers else
 * compare will throw an {@link UnknownValueTypeException}.
 */
public class NumberComparator {
    public static int compare(Object o1, Object o2) throws UnknownValueTypeException {
        if (!isValidNumber(o1) || !isValidNumber(o2)) {
            throw new UnknownValueTypeException();
        }

        return compareUnsafe(o1, o2);
    }

    /**
     * compareUnsafe is provided to avoid checking the input values are numbers. It's assumed that the inputs
     * are known to be Numbers.
     */
    static int compareUnsafe(Object o1, Object o2) {
        return Double.compare(((Number) o1).doubleValue(), ((Number) o2).doubleValue());
    }
}

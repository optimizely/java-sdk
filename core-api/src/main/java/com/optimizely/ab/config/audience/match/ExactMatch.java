/**
 *
 *    Copyright 2018-2020, 2022, Optimizely and contributors
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

import javax.annotation.Nullable;

import static com.optimizely.ab.internal.AttributesUtil.isValidNumber;

/**
 * ExactMatch supports matching Numbers, Strings and Booleans. Numbers are first converted to doubles
 * before the comparison is evaluated. See {@link NumberComparator} Strings and Booleans are evaulated
 * via the Object equals method.
 */
class ExactMatch implements Match {
    @Nullable
    public Boolean eval(Object conditionValue, Object attributeValue) throws UnexpectedValueTypeException {
        if (attributeValue == null) return null;

        if (isValidNumber(attributeValue)) {
            if (isValidNumber(conditionValue)) {
                return NumberComparator.compareUnsafe(attributeValue, conditionValue) == 0;
            }
            return null;
        }

        if (!(conditionValue instanceof String || conditionValue instanceof Boolean)) {
            throw new UnexpectedValueTypeException();
        }

        if (attributeValue.getClass() != conditionValue.getClass()) {
            return null;
        }

        return conditionValue.equals(attributeValue);
    }
}

/**
 *
 *    Copyright 2020, 2022, Optimizely and contributors
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

/**
 * SemanticVersionGEMatch performs a "greater than or equal to" comparison
 * via {@link SemanticVersion#compare(Object, Object)}.
 */
class SemanticVersionGEMatch implements Match {
    @Nullable
    public Boolean eval(Object conditionValue, Object attributeValue) throws UnexpectedValueTypeException {
        if (attributeValue == null) return null;  // stay silent (no WARNING) when attribute value is missing or empty.
        return SemanticVersion.compare(attributeValue, conditionValue) >= 0;
    }
}

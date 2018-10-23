/**
 *
 *    Copyright 2018, Optimizely and contributors
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

class LTMatch extends AttributeMatch<Number> {
    Number value;
    protected LTMatch(Number value) {
        this.value = value;
    }

    public @Nullable
    Boolean eval(Object attributeValue) {
        try {
            return convert(attributeValue, value).doubleValue() < value.doubleValue();
        }
        catch (Exception e) {
            MatchType.logger.error("Less than match failed ", e);
            return null;
        }
    }
}


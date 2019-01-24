/**
 *
 *    Copyright 2018-2019, Optimizely and contributors
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

import com.optimizely.ab.config.ProjectConfig;

import javax.annotation.Nullable;

// Because json number is a double in most java json parsers.  at this
// point we allow comparision of Integer and Double.  The instance class is Double and
// Integer which would fail in our normal exact match.  So, we are special casing for now.  We have already filtered
// out other Number types.
public class ExactNumberMatch extends AttributeMatch<Number> {
    Number value;

    protected ExactNumberMatch(Number value) {
        this.value = value;
    }

    @Nullable
    public Boolean eval(Object attributeValue) {
        try {
            return value.doubleValue() == castToValueType(attributeValue, value).doubleValue();
        } catch (Exception e) {
        }

        return null;
    }
}

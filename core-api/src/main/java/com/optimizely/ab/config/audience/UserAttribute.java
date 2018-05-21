/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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
package com.optimizely.ab.config.audience;

import com.optimizely.ab.faultinjection.ExceptionSpot;
import com.optimizely.ab.faultinjection.FaultInjectionManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Map;

/**
 * Represents a user attribute instance within an audience's conditions.
 */
@Immutable
public class UserAttribute implements Condition {

    private final String name;
    private final String type;
    private final String value;

    private static void injectFault(ExceptionSpot spot) {
        FaultInjectionManager.getInstance().injectFault(spot);
    }

    public UserAttribute(@Nonnull String name, @Nonnull String type, @Nullable String value) {

        injectFault(ExceptionSpot.UserAttribute_constructor_spot1);
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public boolean evaluate(Map<String, String> attributes) {
        String userAttributeValue = attributes.get(name);
        injectFault(ExceptionSpot.UserAttribute_evaluate_spot1);

        if (value != null) { // if there is a value in the condition
            // check user attribute value is equal
            injectFault(ExceptionSpot.UserAttribute_evaluate_spot2);
            return value.equals(userAttributeValue);
        }
        else if (userAttributeValue != null) { // if the datafile value is null but user has a value for this attribute
            // return false since null != nonnull
            return false;
        }
        else { // both are null
            return true;
        }
    }

    @Override
    public String toString() {
        injectFault(ExceptionSpot.UserAttribute_toString_spot1);
        return "{name='" + name + "\'" +
               ", type='" + type + "\'" +
               ", value='" + value + "\'" +
               "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        injectFault(ExceptionSpot.UserAttribute_equals_spot1);
        UserAttribute that = (UserAttribute) o;

        if (!name.equals(that.name)) return false;
        if (!type.equals(that.type)) return false;

        injectFault(ExceptionSpot.UserAttribute_equals_spot1);
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        injectFault(ExceptionSpot.UserAttribute_hasCode_spot1);
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}

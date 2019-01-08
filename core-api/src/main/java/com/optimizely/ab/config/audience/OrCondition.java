/**
 *
 *    Copyright 2016-2019, Optimizely and contributors
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

import com.optimizely.ab.config.ProjectConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Map;

/**
 * Represents an 'Or' conditions condition operation.
 */
@Immutable
public class OrCondition<T> implements Condition<T> {
    private final List<Condition> conditions;

    public OrCondition(@Nonnull List<Condition> conditions) {
        this.conditions = conditions;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    // According to the matrix:
    // true returns true
    // false or null is null
    // false or false is false
    // null or null is null
    @Nullable
    public Boolean evaluate(ProjectConfig config, Map<String, ?> attributes) {
        if (conditions == null) return null;
        boolean foundNull = false;
        for (Condition condition : conditions) {
            Boolean conditionEval = condition.evaluate(config, attributes);
            if (conditionEval == null) { // true with falses and nulls is still true
                foundNull = true;
            } else if (conditionEval) {
                return true;
            }
        }

        // if found null and false return null.  all false return false
        if (foundNull) {
            return null;
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();

        s.append("[or, ");
        for (int i = 0; i < conditions.size(); i++) {
            s.append(conditions.get(i));
            if (i < conditions.size() - 1)
                s.append(", ");
        }
        s.append("]");

        return s.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof OrCondition))
            return false;

        OrCondition otherOrCondition = (OrCondition) other;

        return conditions.equals(otherOrCondition.getConditions());
    }

    @Override
    public int hashCode() {
        return conditions.hashCode();
    }
}

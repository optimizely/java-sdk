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
import java.util.StringJoiner;

/**
 * Represents an 'And' conditions condition operation.
 */
public class AndCondition<T> implements Condition<T> {

    private final List<Condition> conditions;
    private static final String OPERAND = "AND";

    public AndCondition(@Nonnull List<Condition> conditions) {
        this.conditions = conditions;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    @Nullable
    public Boolean evaluate(ProjectConfig config, Map<String, ?> attributes) {
        if (conditions == null) return null;
        boolean foundNull = false;
        // According to the matrix where:
        // false and true is false
        // false and null is false
        // true and null is null.
        // true and false is false
        // true and true is true
        // null and null is null
        for (Condition condition : conditions) {
            Boolean conditionEval = condition.evaluate(config, attributes);
            if (conditionEval == null) {
                foundNull = true;
            } else if (!conditionEval) { // false with nulls or trues is false.
                return false;
            }
            // true and nulls with no false will be null.
        }

        if (foundNull) { // true and null or all null returns null
            return null;
        }

        return true; // otherwise, return true
    }

    @Override
    public String getOperandOrId() {
        return OPERAND;
    }

    @Override
    public String toJson() {
        StringJoiner s = new StringJoiner(", ", "[", "]");
        s.add("\"and\"");
        for (int i = 0; i < conditions.size(); i++) {
            s.add(conditions.get(i).toJson());
        }
        return s.toString();
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("[and, ");
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
        if (!(other instanceof AndCondition))
            return false;

        AndCondition otherAndCondition = (AndCondition) other;

        return conditions.equals(otherAndCondition.getConditions());
    }

    @Override
    public int hashCode() {
        return conditions.hashCode();
    }
}

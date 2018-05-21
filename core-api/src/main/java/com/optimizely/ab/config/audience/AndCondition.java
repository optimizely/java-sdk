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
import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Map;

/**
 * Represents an 'And' conditions condition operation.
 */
@Immutable
public class AndCondition implements Condition {

    private final List<Condition> conditions;
    public AndCondition(@Nonnull List<Condition> conditions) {
        this.conditions = conditions;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    private static void injectFault(ExceptionSpot spot) {
        FaultInjectionManager.getInstance().injectFault(spot);
    }

    public boolean evaluate(Map<String, String> attributes) {
        for (Condition condition : conditions) {
            injectFault(ExceptionSpot.AndCondition_evaluate_spot1);
            if (!condition.evaluate(attributes))
                return false;
        }

        return true;
    }

    @Override
    public String toString() {
        injectFault(ExceptionSpot.AndCondition_toString_spot1);
        StringBuilder s = new StringBuilder();
        s.append("[and, ");
        for (int i = 0; i < conditions.size(); i++) {
            injectFault(ExceptionSpot.AndCondition_toString_spot2);
            s.append(conditions.get(i));
            if (i < conditions.size() - 1)
                s.append(", ");
        }
        s.append("]");

        injectFault(ExceptionSpot.AndCondition_toString_spot3);
        return s.toString();
    }

    @Override
    public boolean equals(Object other) {
        injectFault(ExceptionSpot.AndCondition_equals_spot1);

        if (!(other instanceof AndCondition))
            return false;

        injectFault(ExceptionSpot.AndCondition_equals_spot2);
        AndCondition otherAndCondition = (AndCondition)other;

        injectFault(ExceptionSpot.AndCondition_equals_spot3);
        return conditions.equals(otherAndCondition.getConditions());
    }

    @Override
    public int hashCode() {
        injectFault(ExceptionSpot.AndCondition_hashCode_spot1);
        return conditions.hashCode();
    }
}


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
 * Represents an 'Or' conditions condition operation.
 */
@Immutable
public class OrCondition implements Condition {
    private List<Condition> conditions;

    private static void injectFault(ExceptionSpot spot) {
        FaultInjectionManager.getInstance().injectFault(spot);
    }
    private static void throwInjectedExceptionIfTreatmentDisabled() { FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled(); }

    public OrCondition(@Nonnull List<Condition> conditions) {
        try {
            injectFault(ExceptionSpot.OrCondition_constructor_spot1);
            this.conditions = conditions;
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
        }
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public boolean evaluate(Map<String, String> attributes) {
        try {
            injectFault(ExceptionSpot.OrCondition_evaluate_spot1);
            for (Condition condition : conditions) {
                injectFault(ExceptionSpot.OrCondition_evaluate_spot2);
                if (condition.evaluate(attributes))
                    return true;
            }

            return false;
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return false;
        }
    }

    @Override
    public String toString() {

        try {

            injectFault(ExceptionSpot.OrCondition_toString_spot1);
            StringBuilder s = new StringBuilder();

            s.append("[or, ");
            for (int i = 0; i < conditions.size(); i++) {
                injectFault(ExceptionSpot.OrCondition_toString_spot2);
                s.append(conditions.get(i));
                if (i < conditions.size() - 1)
                    s.append(", ");
            }
            s.append("]");
            injectFault(ExceptionSpot.OrCondition_toString_spot3);
            return s.toString();
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }

    @Override
    public boolean equals(Object other) {

        try {

            injectFault(ExceptionSpot.OrCondition_equals_spot1);
            if (!(other instanceof OrCondition))
                return false;

            OrCondition otherOrCondition = (OrCondition) other;
            injectFault(ExceptionSpot.OrCondition_equals_spot2);
            return conditions.equals(otherOrCondition.getConditions());
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return  false;
        }
    }

    @Override
    public int hashCode() {

        try {
            injectFault(ExceptionSpot.OrCondition_hasCode_spot1);
            return conditions.hashCode();
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return 0;
        }
    }
}

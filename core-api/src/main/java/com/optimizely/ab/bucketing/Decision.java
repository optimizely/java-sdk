/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/
package com.optimizely.ab.bucketing;

import com.optimizely.ab.faultinjection.ExceptionSpot;
import com.optimizely.ab.faultinjection.FaultInjectionManager;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * A class representing a stored decision.
 */
public class Decision {

    /** The ID of the {@link com.optimizely.ab.config.Variation} the user was bucketed into. */
    @Nonnull public String variationId;

    private static void injectFault(ExceptionSpot spot) {
        FaultInjectionManager.getInstance().injectFault(spot);
    }
    private static void throwInjectedExceptionIfTreatmentDisabled() { FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled(); }

    /**
     * Initialize a Decision object.
     * @param variationId The ID of the variation the user was bucketed into.
     */
    public Decision(@Nonnull String variationId) {
        try {
            injectFault(ExceptionSpot.Decision_constructor_spot1);
            this.variationId = variationId;
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
        }
    }

    @Override
    public boolean equals(Object o) {

        try {
            injectFault(ExceptionSpot.Decision_equals_spot1);

            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            injectFault(ExceptionSpot.Decision_equals_spot2);

            Decision decision = (Decision) o;

            injectFault(ExceptionSpot.Decision_equals_spot3);
            return variationId.equals(decision.variationId);
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return false;
        }
    }

    @Override
    public int hashCode() {
        try {
            injectFault(ExceptionSpot.Decision_hashCode_spot1);
            return variationId.hashCode();
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return 0;
        }
    }

    public Map<String, String> toMap() {
        try {
            injectFault(ExceptionSpot.Decision_toMap_spot1);
            Map<String, String> decisionMap = new HashMap<String, String>(1);
            injectFault(ExceptionSpot.Decision_toMap_spot2);
            decisionMap.put(UserProfileService.variationIdKey, variationId);
            return decisionMap;
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return null;
        }
    }
}

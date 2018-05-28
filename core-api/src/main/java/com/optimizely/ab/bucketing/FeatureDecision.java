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

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.faultinjection.ExceptionSpot;
import com.optimizely.ab.faultinjection.FaultInjectionManager;

import javax.annotation.Nullable;

public class FeatureDecision {
    /** The {@link Experiment} the Feature is associated with. */
    @Nullable public Experiment experiment;

    /** The {@link Variation} the user was bucketed into. */
    @Nullable public Variation variation;

    /** The source of the {@link Variation}. */
    @Nullable public DecisionSource decisionSource;

    public static void injectFault(ExceptionSpot spot) {
        FaultInjectionManager.getInstance().injectFault(spot);
    }
    private static void throwInjectedExceptionIfTreatmentDisabled() { FaultInjectionManager.getInstance().throwExceptionIfTreatmentDisabled(); }

    public enum DecisionSource {
        EXPERIMENT,
        ROLLOUT
    }

    /**
     * Initialize a FeatureDecision object.
     * @param experiment The {@link Experiment} the Feature is associated with.
     * @param variation The {@link Variation} the user was bucketed into.
     * @param decisionSource The source of the variation.
     */
    public FeatureDecision(@Nullable Experiment experiment, @Nullable Variation variation,
                           @Nullable DecisionSource decisionSource) {

        try {
            injectFault(ExceptionSpot.FeatureDecision_constructor_spot1);
            this.experiment = experiment;
            this.variation = variation;
            this.decisionSource = decisionSource;
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
        }
    }

    @Override
    public boolean equals(Object o) {
        try {
            injectFault(ExceptionSpot.FeatureDecision_equals_spot1);
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FeatureDecision that = (FeatureDecision) o;
            injectFault(ExceptionSpot.FeatureDecision_equals_spot2);
            if (variation != null ? !variation.equals(that.variation) : that.variation != null)
                return false;

            injectFault(ExceptionSpot.FeatureDecision_equals_spot3);
            return decisionSource == that.decisionSource;
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return false;
        }
    }

    @Override
    public int hashCode() {
        try {
            injectFault(ExceptionSpot.FeatureDecision_hashCode_spot1);
            int result = variation != null ? variation.hashCode() : 0;
            result = 31 * result + (decisionSource != null ? decisionSource.hashCode() : 0);
            injectFault(ExceptionSpot.FeatureDecision_hashCode_spot2);
            return result;
        } catch (Exception e) {
            throwInjectedExceptionIfTreatmentDisabled();
            return 0;
        }
     }
}

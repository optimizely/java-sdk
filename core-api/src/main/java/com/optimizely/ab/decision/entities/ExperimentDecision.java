/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
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
package com.optimizely.ab.decision.entities;

import javax.annotation.Nullable;
import com.optimizely.ab.config.Variation;

/**
 * ExperimentDecision contains the decision information about an experiment
 */
public class ExperimentDecision {
    /**
     * Represents the Optimizely Variation configuration.
     */
    @Nullable
    public Variation variation;
    /**
     * Decision reason and status
     */
    @Nullable
    public DecisionStatus decisionStatus;

    /**
     * Initialize a ExperimentDecision object.
     */
    public ExperimentDecision(@Nullable Variation variation,
                              @Nullable DecisionStatus decisionStatus) {
        this.variation = variation;
        this.decisionStatus = decisionStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExperimentDecision that = (ExperimentDecision) o;
        return (variation != null ? variation.equals(that.variation) : that.variation == null) && decisionStatus == that.decisionStatus;
    }

    @Override
    public int hashCode() {
        int result = variation != null ? variation.hashCode() : 0;
        result = 31 * result + (decisionStatus != null ? decisionStatus.hashCode() : 0);
        return result;
    }
}

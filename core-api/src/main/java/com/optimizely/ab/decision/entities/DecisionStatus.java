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

public class DecisionStatus {
    /**
     *  Flag for decision status
     */
    public boolean decisionMade;
    /**
     *  Possible Reason decision is made for
     */
    public Reason reason;

    /**
     * Initialize Decision Status
     * @param decisionMade Flag for decision status
     * @param reason  Possible Reason decision is made for
     */
    public DecisionStatus(boolean decisionMade, Reason reason) {
        this.decisionMade = decisionMade;
        this.reason = reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DecisionStatus that = (DecisionStatus) o;
        return (reason != null ? reason.equals(that.reason) : that.reason == null) && decisionMade == that.decisionMade;
    }

    @Override
    public int hashCode() {
        return 31 * (reason != null ? reason.hashCode() : 0);
    }
}
